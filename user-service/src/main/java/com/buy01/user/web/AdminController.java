/* BUY-01 learning header
 * File purpose: Exposes admin HTTP endpoints.
 * Learning focus: Thin REST controllers, request validation, status codes, and delegated business logic.
 */
package com.buy01.user.web;

import com.buy01.user.dto.UserResponse;
import com.buy01.user.repository.UserAccountRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminController {

    private final UserAccountRepository repository;

    public AdminController(UserAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<UserResponse> listUsers() {
        return repository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }
}
