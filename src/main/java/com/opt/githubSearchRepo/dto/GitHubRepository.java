package com.opt.githubSearchRepo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepository(
        @JsonProperty("name") String name,
        @JsonProperty("owner") Owner owner,
        @JsonProperty("fork") boolean fork
) {
    public record Owner(@JsonProperty("login") String login) {}
}
