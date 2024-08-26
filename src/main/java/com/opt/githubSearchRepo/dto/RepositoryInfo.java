package com.opt.githubSearchRepo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RepositoryInfo(
        @JsonProperty("name") String name,
        @JsonProperty("ownerLogin") String ownerLogin,
        @JsonProperty("branches") List<BranchInfo> branches) {}
