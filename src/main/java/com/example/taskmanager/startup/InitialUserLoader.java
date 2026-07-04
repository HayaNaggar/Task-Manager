package com.example.taskmanager.startup;

import com.example.taskmanager.entity.Role;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialUserLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialUserLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail("admin@example.com")) {
            User admin = new User();
            admin.setFullName("Administrator");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("adminpass"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("member@example.com")) {
            User member = new User();
            member.setFullName("Member User");
            member.setEmail("member@example.com");
            member.setPassword(passwordEncoder.encode("memberpass"));
            member.setRole(Role.MEMBER);
            userRepository.save(member);
        }
    }
}
