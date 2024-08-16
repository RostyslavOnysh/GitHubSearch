package com.opt.githubSearchRepo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.opt.githubSearchRepo.dto.BranchInfo;
import com.opt.githubSearchRepo.dto.GitHubBranch;
import com.opt.githubSearchRepo.dto.GitHubRepository;
import com.opt.githubSearchRepo.dto.RepositoryInfo;
import com.opt.githubSearchRepo.exception.UserNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class GithubServiceImpl implements GithubService {

    private static final String GITHUB_SERVICE = "githubService";
    private final WebClient webClient;
    private final CacheService cacheService;

    // Оптимізація: Власний шедулер з 20 потоками для паралельних операцій
    private final Scheduler parallelScheduler = Schedulers.newParallel("custom-parallel-scheduler", 20);

    public GithubServiceImpl(WebClient.Builder webClientBuilder, CacheService cacheService) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.cacheService = cacheService;
    }

    @Override
    @CircuitBreaker(name = GITHUB_SERVICE, fallbackMethod = "fallbackGetBranches")
    @Retry(name = GITHUB_SERVICE)
    @RateLimiter(name = GITHUB_SERVICE)
    @Cacheable(value = "branches", key = "#username + '-' + #repoName")
    public Flux<BranchInfo> getBranches(String username, String repoName) {
        log.info("Fetching branches for repository: {}/{}", username, repoName);
        String cacheKey = username + "-" + repoName;

        List<BranchInfo> cachedBranches = cacheService.getFromCache(cacheKey, new TypeReference<List<BranchInfo>>() {});
        if (cachedBranches != null) {
            log.info("Returning cached branches for repository: {}/{}", username, repoName);
            return Flux.fromIterable(cachedBranches);
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/repos/{username}/{repoName}/branches").build(username, repoName))
                .retrieve()
                .bodyToFlux(GitHubBranch.class)
                .parallel(4)
                .runOn(parallelScheduler) // Використання налаштованого шедулера
                .map(branch -> new BranchInfo(branch.name(), branch.commit().sha()))
                .sequential()
                .timeout(Duration.ofSeconds(10)) // Оптимізація: Збільшення таймауту до 10 секунд
                .doOnNext(branch -> log.info("Fetched branch: {}", branch.name()))
                .collectList()
                .doOnNext(branches -> cacheService.putInCache(cacheKey, branches))
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    log.error("User or repository not found: {}", ex.getMessage());
                    throw new UserNotFoundException("User or repository not found");
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClient error fetching branches: {}", ex.getMessage());
                    return Flux.empty();
                })
                .doOnComplete(()
                        -> log.info("Successfully fetched branches for repository: {}/{}", username, repoName));
    }

    @Override
    @Cacheable(value = "repositories", key = "#username")
    public Flux<RepositoryInfo> getNonForkRepositories(String username) {
        log.info("Fetching non-fork repositories for user: {}", username);

        String cacheKey = "repos-" + username;
        List<RepositoryInfo> cachedRepos = cacheService
                .getFromCache(cacheKey, new TypeReference<List<RepositoryInfo>>() {});
        if (cachedRepos != null) {
            log.info("Returning cached repositories for user: {}", username);
            return Flux.fromIterable(cachedRepos);
        }

        return webClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .bodyToFlux(GitHubRepository.class)
                .filter(repo -> !repo.fork())
                .limitRate(10)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .flatMap(repo -> getBranches(username, repo.name())
                        .collectList()
                        .map(branches -> new RepositoryInfo(repo.name(), repo.owner().login(), branches))
                        .subscribeOn(parallelScheduler)) // Використання налаштованого шедулера
                .timeout(Duration.ofSeconds(10)) // Оптимізація: Збільшення таймауту до 10 секунд
                .doOnNext(repoInfo -> cacheService.putInCache(cacheKey, List.of(repoInfo)))
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    log.error("User or repository not found: {}", ex.getMessage());
                    throw new UserNotFoundException("User or repository not found");
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClient error fetching repositories: {}", ex.getMessage());
                    return Flux.empty();
                })
                .doOnComplete(() -> log.info("Successfully fetched repositories for user: {}", username));
    }

    public Flux<BranchInfo> fallbackGetBranches(String username, String repoName, Throwable throwable) {
        log.warn("Fallback triggered for getBranches for repository {}/{}. Reason: {}",
                username, repoName, throwable.getMessage());
        return Flux.empty();
    }
}
