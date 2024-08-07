package com.opt.github_search_repo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GitHubSearchRepoApplicationTests {

    @Test
    void contextLoads() {
    }

}
