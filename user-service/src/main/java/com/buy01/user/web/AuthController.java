/* BUY-01 learning header
 * File purpose: Exposes auth HTTP endpoints.
 * Learning focus: Thin REST controllers, request validation, status codes, and delegated business logic.
 */
package com.buy01.user.web;

import com.buy01.user.dto.AuthResponse;
import com.buy01.user.dto.LoginRequest;
import com.buy01.user.dto.RegisterRequest;
import com.buy01.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Learning annotation: @RestController combines @Controller and @ResponseBody so methods return serialized API data.
@RestController
// Learning annotation: @RequestMapping defines the shared base URL (and optionally other rules) for this controller.
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Learning annotation: @PostMapping maps HTTP POST requests to this create/action controller method.
    @PostMapping("/register")
    // Learning annotation: @ResponseStatus sets the successful HTTP status returned by this controller method.
    @ResponseStatus(HttpStatus.CREATED)
    // Learning annotations: @RequestBody converts JSON to RegisterRequest; @Valid runs its validation rules.
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Learning annotation: @PostMapping maps HTTP POST requests to this create/action controller method.
    @PostMapping("/login")
    // Learning annotations: @RequestBody converts JSON to LoginRequest; @Valid runs its validation rules.
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
