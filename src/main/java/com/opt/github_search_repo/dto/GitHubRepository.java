package com.opt.github_search_repo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepository(
        @JsonProperty("name") String name,
        @JsonProperty("owner") Owner owner,
        @JsonProperty("fork") boolean fork
) {

    public static record Owner(@JsonProperty("login") String login) {}
}