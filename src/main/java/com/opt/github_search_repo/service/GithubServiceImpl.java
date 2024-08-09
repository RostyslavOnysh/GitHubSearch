package com.opt.github_search_repo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opt.github_search_repo.dto.BranchInfo;
import com.opt.github_search_repo.dto.GitHubBranch;
import com.opt.github_search_repo.dto.GitHubRepository;
import com.opt.github_search_repo.dto.RepositoryInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class GithubServiceImpl implements GithubService {

    private static final String GITHUB_SERVICE = "githubService";
    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public GithubServiceImpl(WebClient.Builder webClientBuilder, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void checkRateLimit() {
        webClient.get()
                .uri("/rate_limit")
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> log.info("Rate limit status: {}", response))
                .doOnError(WebClientResponseException.class, ex -> log.error("Rate limit check failed: {}", ex.getMessage()))
                .subscribe();
    }

    @Override
    @CircuitBreaker(name = GITHUB_SERVICE, fallbackMethod = "fallbackGetBranches")
    @Retry(name = GITHUB_SERVICE)
    @RateLimiter(name = GITHUB_SERVICE)
    @Cacheable(value = "branches", key = "#username + '-' + #repoName")
    public Flux<BranchInfo> getBranches(String username, String repoName) {
        log.info("Fetching branches for repository: {}/{}", username, repoName);
        String cacheKey = username + "-" + repoName;

        List<BranchInfo> cachedBranches = getBranchesFromCache(cacheKey);
        if (cachedBranches != null) {
            log.info("Returning cached branches for repository: {}/{}", username, repoName);
            return Flux.fromIterable(cachedBranches);
        }

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{username}/{repoName}/branches")
                        .build(username, repoName))
                .retrieve()
                .bodyToFlux(GitHubBranch.class)
                .parallel(4)
                .runOn(Schedulers.parallel())
                .map(branch -> new BranchInfo(branch.name(), branch.commit().sha()))
                .sequential()
                .timeout(Duration.ofSeconds(5))
                .doOnNext(branch -> log.info("Fetched branch: {}", branch.name()))
                .collectList()
                .doOnNext(branches -> {
                    putBranchesInCache(cacheKey, branches);
                    log.info("Caching {} branches for repository: {}/{}", branches.size(), username, repoName);
                })
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("WebClient error fetching branches: {}", ex.getMessage());
                    return Flux.empty();
                })
                .doOnComplete(() -> log.info("Successfully fetched branches for repository: {}/{}", username, repoName));
    }

    @Override
    @Cacheable(value = "repositories", key = "#username")
    public Flux<RepositoryInfo> getNonForkRepositories(String username) {
        log.info("Fetching non-fork repositories for user: {}", username);

        String cacheKey = "repos-" + username;
        List<RepositoryInfo> cachedRepos = getRepositoriesFromCache(cacheKey);
        if (cachedRepos != null) {
            log.info("Returning cached repositories for user: {}", username);
            return Flux.fromIterable(cachedRepos);
        }

        return webClient.get()
                .uri("/users/{username}/repos", username)
                .ifNoneMatch("W/\"etag_value\"")
                .retrieve()
                .bodyToFlux(GitHubRepository.class)
                .doOnNext(repo -> log.info("Repository Data: {}", repo))
                .filter(repo -> !repo.fork())
                .flatMap(repo -> getBranches(username, repo.name())
                        .collectList()
                        .map(branches -> new RepositoryInfo(repo.name(), repo.owner().login(), branches))
                        .subscribeOn(Schedulers.parallel()))
                .timeout(Duration.ofSeconds(5))
                .doOnError(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        log.error("Forbidden: Rate limit exceeded or permissions issue.");
                    } else {
                        log.error("WebClient error fetching repositories: {}", ex.getMessage());
                    }
                })
                .doOnNext(repoInfo -> putRepositoriesInCache(cacheKey, List.of(repoInfo)))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                        log.warn("Returning cached data due to rate limit: {}", ex.getMessage());
                        return Flux.fromIterable(cachedRepos);
                    } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("User or resource not found:: {}", ex.getMessage());
                        return Flux.error(ex);
                    }
                    return Flux.empty();
                })
                .doOnComplete(() -> log.info("Successfully fetched repositories for user: {}", username));
    }

    private void putBranchesInCache(String key, List<BranchInfo> branches) {
        try {
            String serializedData = objectMapper.writeValueAsString(branches);
            redisTemplate.opsForValue().set(key, serializedData);
            log.info("Serialized branches for caching: {}", serializedData);
        } catch (JsonProcessingException e) {
            log.error("Error serializing branches for caching: {}", e.getMessage());
        }
    }

    private List<BranchInfo> getBranchesFromCache(String key) {
        try {
            String data = (String) redisTemplate.opsForValue().get(key);
            List<BranchInfo> branches = objectMapper.readValue(data, new TypeReference<List<BranchInfo>>() {});
            log.info("Deserialized branches from cache: {}", branches);
            return branches;
        } catch (Exception e) {
            log.warn("Error deserializing branches from cache: {}", e.getMessage());
            return null;
        }
    }

    private void putRepositoriesInCache(String key, List<RepositoryInfo> repositories) {
        try {
            String serializedData = objectMapper.writeValueAsString(repositories);
            redisTemplate.opsForValue().set(key, serializedData);
            log.info("Serialized repositories for caching: {}", serializedData);
        } catch (JsonProcessingException e) {
            log.error("Error serializing repositories for caching: {}", e.getMessage());
        }
    }

    private List<RepositoryInfo> getRepositoriesFromCache(String key) {
        try {
            String data = (String) redisTemplate.opsForValue().get(key);
            List<RepositoryInfo> repositories = objectMapper.readValue(data, new TypeReference<List<RepositoryInfo>>() {});
            log.info("Deserialized repositories from cache: {}", repositories);
            return repositories;
        } catch (Exception e) {
            log.warn("Error deserializing repositories from cache: {}", e.getMessage());
            return null;
        }
    }

    public Flux<BranchInfo> fallbackGetBranches(String username, String repoName, Throwable throwable) {
        log.warn("Fallback triggered for getBranches for repository {}/{}. Reason: {}", username, repoName, throwable.getMessage());
        return Flux.empty();
    }
}