package com.opt.githubSearchRepo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Flux;

public record RepositoryInfo(
        @JsonProperty("name") String name,
        @JsonProperty("ownerLogin") String ownerLogin,
        @JsonProperty("branches") Flux<BranchInfo> branches) {}
