/*
 * File purpose: Exposes admin HTTP endpoints.
 */
package com.nexora.user.web;

import com.nexora.user.dto.UserResponse;
import com.nexora.user.repository.UserAccountRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Learning annotation: @RestController combines @Controller and @ResponseBody so methods return serialized API data.
@RestController
// Learning annotation: @RequestMapping defines the shared base URL (and optionally other rules) for this controller.
@RequestMapping("/admin/users")
public class AdminController {

    private final UserAccountRepository repository;

    public AdminController(UserAccountRepository repository) {
        this.repository = repository;
    }

    // Learning annotation: @GetMapping maps HTTP GET requests to this read-only controller method.
    @GetMapping
    // Learning annotation: @PreAuthorize evaluates this authorization expression before the method is allowed to run.
    @PreAuthorize("hasRole('ADMIN')")
    List<UserResponse> listUsers() {
        return repository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }
}
