package com.opt.github_search_repo.service;

import com.opt.github_search_repo.dto.BranchInfo;
import com.opt.github_search_repo.dto.RepositoryInfo;
import reactor.core.publisher.Flux;

public interface GithubService {
    Flux<BranchInfo> getBranches(String username, String repoName);
    Flux<RepositoryInfo> getNonForkRepositories(String username);
}
