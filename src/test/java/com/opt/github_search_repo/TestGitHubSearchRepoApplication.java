package com.opt.github_search_repo;

import org.springframework.boot.SpringApplication;

public class TestGitHubSearchRepoApplication {

    public static void main(String[] args) {
        SpringApplication.from(GitHubSearchRepoApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
