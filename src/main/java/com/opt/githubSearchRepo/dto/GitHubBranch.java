package com.opt.githubSearchRepo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubBranch(
        @JsonProperty("name") String name,
        @JsonProperty("commit") Commit commit
) {

    public record Commit(@JsonProperty("sha") String sha) {}
}
