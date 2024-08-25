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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class GithubServiceImpl implements GithubService {

    private static final String GITHUB_SERVICE = "githubService";
    private final WebClient webClient;

    public GithubServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
    }

    @Override
    @CircuitBreaker(name = GITHUB_SERVICE, fallbackMethod = "fallbackGetBranches")
    @Retry(name = GITHUB_SERVICE)
    @RateLimiter(name = GITHUB_SERVICE)
    public Flux<BranchInfo> getBranches(String username, String repoName) {
        log.info("Fetching branches for repository: {}/{}", username, repoName);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/repos/{username}/{repoName}/branches").build(username, repoName))
                .retrieve()
                .bodyToFlux(GitHubBranch.class)
                .parallel(4)
                .runOn(Schedulers.parallel())
                .map(branch -> new BranchInfo(branch.name(), branch.commit().sha()))
                .sequential()
                .timeout(Duration.ofSeconds(5))
                .doOnNext(branch -> log.info("Fetched branch: {}", branch.name()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClient error fetching branches: {}", ex.getMessage());
                    return Flux.empty();
                })
                .doOnComplete(() ->
                        log.info("Successfully fetched branches for repository: {}/{}", username, repoName));
    }

    @Override
    public Flux<RepositoryInfo> getNonForkRepositories(String username) {
        log.info("Fetching non-fork repositories for user: {}", username);

        return webClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .bodyToFlux(GitHubRepository.class)
                .filter(repo -> !repo.fork())
                .flatMap(repo -> getBranches(username, repo.name())
                        .map(branch -> new RepositoryInfo(repo.name(), repo.owner().login(), Flux.just(branch)))
                )
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        log.warn("Rate limit exceeded: {}", ex.getMessage());
                        return Flux.error(ex);
                    } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("User or resource not found: {}", ex.getMessage());
                        return Flux.error(ex);
                    }
                    return Flux.empty();
                })
                .doOnComplete(() -> log.info("Successfully fetched repositories for user: {}", username));
    }
}
