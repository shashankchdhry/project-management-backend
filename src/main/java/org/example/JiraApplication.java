package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the Jira-like project management platform.
 *
 * <p>This is intentionally the only Java source present until the design
 * documentation under {@code docs/} is finalized. Domain, application, and
 * adapter packages will be introduced per the architecture defined there.
 */
@SpringBootApplication
public class JiraApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiraApplication.class, args);
    }
}