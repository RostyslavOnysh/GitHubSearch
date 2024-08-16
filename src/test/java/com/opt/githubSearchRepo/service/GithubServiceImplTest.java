package com.opt.githubSearchRepo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
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
}
