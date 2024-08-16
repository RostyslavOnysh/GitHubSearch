package com.opt.githubSearchRepo.service;

import com.opt.githubSearchRepo.dto.BranchInfo;
import com.opt.githubSearchRepo.dto.RepositoryInfo;
import reactor.core.publisher.Flux;

public interface GithubService {
    Flux<BranchInfo> getBranches(String username, String repoName);

    Flux<RepositoryInfo> getNonForkRepositories(String username);
}
