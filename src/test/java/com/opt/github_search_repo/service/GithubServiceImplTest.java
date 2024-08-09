package com.opt.github_search_repo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
class GithubServiceImplTest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void should_return_Repositories_for_user_and_status_200() {
        String userName = "Hlib13";
        webTestClient.get().uri("/api/github/users/{userName}/repos", userName)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$[0].name").exists()
                .jsonPath("$[0].ownerLogin").isEqualTo(userName);
    }

    @Test
    public void should_return_status_200_and_empty_array() {
        String userWithoutRepos = "testuserGHsearch";
        webTestClient.get().uri("/api/github/users/{userWithoutRepos}/repos", userWithoutRepos)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("[]");
    }

    @Test
    public void should_return_not_existing_user_and_status_404() {
        String notExistingUser = "userWithoutRepos";
        webTestClient.get()
                .uri("/api/github/users/{notExistingUser}/repos", notExistingUser)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/json")
                .expectBody(String.class)
                .consumeWith(response -> System.out.println("Response: " + response.getResponseBody()));
    }

    @Test
    public void should_return_not_acceptable_format_and_status_406() {
        String userName = "Hlib13";
        webTestClient.get().uri("/api/github/users/{userName}/repos", userName)
                .accept(MediaType.APPLICATION_XML)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(406)
                .jsonPath("$.message").isEqualTo("Not acceptable format");
    }
}
