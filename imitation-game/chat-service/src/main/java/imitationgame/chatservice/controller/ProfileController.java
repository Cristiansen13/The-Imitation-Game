package imitationgame.chatservice.controller;

import imitationgame.chatservice.model.UserProfile;
import imitationgame.chatservice.repository.UserProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserProfileRepository userRepo;
    public ProfileController(UserProfileRepository userRepo) { this.userRepo = userRepo; }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> me(@AuthenticationPrincipal Jwt jwt) {
        String sub = jwt.getSubject(); // Keycloak user id
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        // upsert minimal
        var p = userRepo.findById(sub).orElseGet(() -> {
            UserProfile up = new UserProfile();
            up.setId(sub); up.setUsername(username); up.setEmail(email);
            return up;
        });
        userRepo.save(p);
        return ResponseEntity.ok(p);
    }
}
