package com.opt.githubSearchRepo.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonDeserialize
public class WrapperValue {
    private String name;
    private String commitSha;

    public WrapperValue() {
    }

    @JsonCreator
    public WrapperValue(@JsonProperty("name") String name,
                        @JsonProperty("commitSha") String commitSha) {
        this.name = name;
        this.commitSha = commitSha;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }
}
