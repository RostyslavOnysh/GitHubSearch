package com.opt.github_search_repo.service;

import com.opt.github_search_repo.controllers.GithubController;
import com.opt.github_search_repo.dto.BranchInfo;
import com.opt.github_search_repo.dto.RepositoryInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(GithubController.class)
public class GithubControllerTest {
    @MockBean
    private GithubService githubService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(new GithubController(githubService)).build();
    }


    @Test
    void testGetNonForkRepositories() {
        String username = "testuser";

        RepositoryInfo repo1 = new RepositoryInfo("Repo1", "testuser", List.of());
        RepositoryInfo repo2 = new RepositoryInfo("Repo2", "testuser", List.of());

        when(githubService.getNonForkRepositories(username)).thenReturn(Flux.just(repo1, repo2));

        webTestClient.get()
                .uri("/api/github/users/{username}/repos", username)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(RepositoryInfo.class)
                .hasSize(2)
                .contains(repo1, repo2);
    }

    @Test
    void testGetBranches() {
        String username = "testuser";
        String repoName = "Repo1";

        BranchInfo branch1 = new BranchInfo("main", "commitSha1");
        BranchInfo branch2 = new BranchInfo("dev", "commitSha2");

        when(githubService.getBranches(username, repoName)).thenReturn(Flux.just(branch1, branch2));

        webTestClient.get()
                .uri("/api/github/users/{username}/repos/{repoName}/branches", username, repoName)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BranchInfo.class)
                .hasSize(2)
                .contains(branch1, branch2);
    }

    @Test
    void testCheckRateLimit() {
        webTestClient.get()
                .uri("/api/github/rate-limit")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("Rate limit checked. See logs for details.");
    }
}
