package com.opt.github_search_repo.controllers;

import com.opt.github_search_repo.dto.BranchInfo;
import com.opt.github_search_repo.dto.RepositoryInfo;
import com.opt.github_search_repo.service.GithubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub API", description = "Operations related to GitHub repositories and branches")
public class GithubController {
    private final GithubService githubService;

    @Operation(summary = "Get Non-Fork Repositories", description = "Retrieve all non-fork repositories for a given GitHub username.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved non-fork repositories"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Rate limit exceeded or access forbidden")
    })
    @GetMapping(value = "/users/{username}/repos",produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<RepositoryInfo> getNonForkRepositories(@PathVariable String username) {
        return githubService.getNonForkRepositories(username);
    }



    @Operation(summary = "Get Branches", description = "Retrieve all branches for a specific repository of a GitHub user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved branches"),
            @ApiResponse(responseCode = "404", description = "Repository or user not found"),
            @ApiResponse(responseCode = "403", description = "Rate limit exceeded or access forbidden")
    })
    @GetMapping(value = "/users/{username}/repos/{repoName}/branches",produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<BranchInfo> getBranches(@PathVariable String username, @PathVariable String repoName) {
        return githubService.getBranches(username, repoName);
    }
}