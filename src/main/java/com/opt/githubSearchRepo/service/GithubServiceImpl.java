package com.opt.githubSearchRepo.service;

import com.opt.githubSearchRepo.dto.BranchInfo;
import com.opt.githubSearchRepo.dto.GitHubBranch;
import com.opt.githubSearchRepo.dto.GitHubRepository;
import com.opt.githubSearchRepo.dto.RepositoryInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class GithubServiceImpl implements GithubService {

    private static final String GITHUB_SERVICE = "githubService";
    private final WebClient webClient;
    private final CacheService cacheService;
    private final Scheduler parallelScheduler = Schedulers.newParallel("custom-parallel-scheduler", 10);

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

        return cacheService.getFromCacheAsFlux(cacheKey, BranchInfo.class)
                .switchIfEmpty(
                        webClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder.path("/repos/{username}/{repoName}/branches")
                                                .build(username, repoName))
                                .retrieve()
                                .bodyToFlux(GitHubBranch.class)
                                .parallel(10)
                                .runOn(parallelScheduler)
                                .map(branch -> new BranchInfo(branch.name(),
                                        branch.commit().sha()))
                                .sequential()
                                .timeout(Duration.ofSeconds(5))
                                .collectList()
                                .doOnNext(branches ->
                                        cacheService.putInCacheAsync(cacheKey, branches))
                                .flatMapMany(Flux::fromIterable)
                                .onErrorResume(WebClientResponseException.class, ex -> {
                                    log.error("WebClient error fetching branches: {}", ex.getMessage());
                                    return Flux.empty();
                                })
                )
                .doOnComplete(() ->
                        log.info("Successfully fetched branches for repository: {}/{}", username, repoName));
    }

    @Override
    @Cacheable(value = "repositories", key = "#username")
    public Flux<RepositoryInfo> getNonForkRepositories(String username) {
        log.info("Fetching non-fork repositories for user: {}", username);

        String cacheKey = "repos-" + username;

        return cacheService.getFromCacheAsFlux(cacheKey, RepositoryInfo.class)
                .switchIfEmpty(
                        webClient.get()
                                .uri("/users/{username}/repos", username)
                                .retrieve()
                                .bodyToFlux(GitHubRepository.class)
                                .filter(repo -> !repo.fork())
                                .flatMap(repo -> getBranches(username, repo.name())
                                        .collectList()
                                        .map(branches ->
                                                new RepositoryInfo(repo.name(),
                                                        repo.owner().login(), branches))
                                        .subscribeOn(parallelScheduler))
                                .timeout(Duration.ofSeconds(5))
                                .collectList()
                                .doOnNext(repos -> cacheService.putInCacheAsFlux(cacheKey, repos))
                                .flatMapMany(Flux::fromIterable)
                                .onErrorResume(WebClientResponseException.class, ex -> {
                                    log.error("WebClient error fetching repositories: {}", ex.getMessage());
                                    return Flux.empty();
                                })
                )
                .doOnComplete(() -> log.info("Successfully fetched repositories for user: {}", username));
    }
}
