/*
 * File purpose: Creates and configures demo data seeder.
 */
package com.buy01.user.config;

import com.buy01.user.domain.Role;
import com.buy01.user.domain.UserAccount;
import com.buy01.user.repository.UserAccountRepository;
import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Learning annotation: @Component marks the class for component scanning so Spring creates and manages one instance.
@Component
// Learning annotation: @ConditionalOnProperty enables this bean only when the named configuration property has the required value.
@ConditionalOnProperty(name = "app.seed-demo-users", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // Learning annotation: @Override asks the Java compiler to verify that this method implements or overrides a parent contract.
    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("client@buy01.local", "Demo Client", "Client123!", Role.CLIENT);
        createIfMissing("seller@buy01.local", "Demo Seller", "Seller123!", Role.SELLER);
        createIfMissing("admin@buy01.local", "Demo Admin", "Admin123!", Role.ADMIN);
    }

    private void createIfMissing(
            String email,
            String name,
            String password,
            Role role) {
        if (!repository.existsByEmailIgnoreCase(email)) {
            repository.save(new UserAccount(
                    name,
                    email,
                    passwordEncoder.encode(password),
                    role,
                    Instant.now()));
        }
    }
}
