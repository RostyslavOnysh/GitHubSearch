package com.opt.github_search_repo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opt.github_search_repo.dto.RepositoryInfo;

import java.util.List;

public class WrapperValue {
    @JsonProperty("repositories")
    private final List<RepositoryInfo> repositories;

    public WrapperValue(List<RepositoryInfo> repositories) {
        this.repositories = List.copyOf(repositories);
    }

    public List<RepositoryInfo> getRepositories() {
        return repositories;
    }
}
