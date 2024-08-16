package com.opt.githubSearchRepo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BranchInfo(
        @JsonProperty("name") String name,
        @JsonProperty("commitSha") String commitSha
) {}
