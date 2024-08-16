package com.opt.githubSearchRepo.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
class GithubServiceImplTest {
    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        stubFor(get(urlPathMatching("/test/repos/userWithoutRepos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        stubFor(get(urlPathMatching("/Hlib13/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\":\"repo1\",\"owner\":{\"login\":\"Hlib13\"}}]")));

        stubFor(get(urlPathMatching("/userWithoutRepos/repos"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        stubFor(get(urlPathMatching("/user123czxczci9fi/repos"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":404, \"message\":\"This user does not exist\"}")));

        stubFor(get(urlPathMatching("/Hlib13/repos"))
                .withHeader("Accept", equalTo("application/xml"))
                .willReturn(aResponse()
                        .withStatus(406)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":406, \"message\":\"Not acceptable format\"}")));
    }

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
}
