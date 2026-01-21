package imitationgame.chatservice.controller;

import imitationgame.chatservice.model.UserProfile;
import imitationgame.chatservice.repository.MessageLogRepository;
import imitationgame.chatservice.repository.RoomPlayerRepository;
import imitationgame.chatservice.repository.UserProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    private final UserProfileRepository userRepo;
    private final MessageLogRepository messageRepo;
    private final RoomPlayerRepository roomPlayerRepo;
    
    public ProfileController(UserProfileRepository userRepo, 
                           MessageLogRepository messageRepo,
                           RoomPlayerRepository roomPlayerRepo) {
        this.userRepo = userRepo;
        this.messageRepo = messageRepo;
        this.roomPlayerRepo = roomPlayerRepo;
    }

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
    
    /**
     * PATCH /profile/me - Update the authenticated user's email address
     * 
     * Postman Test:
     * PATCH http://localhost:8080/profile/me
     * Headers: Authorization: Bearer {your_jwt_token}
     *          Content-Type: application/json
     * Body (raw JSON):
     * {
     *   "email": "newemail@example.com"
     * }
     * 
     * Response: 200 OK
     * {
     *   "id": "user-id",
     *   "username": "username",
     *   "email": "newemail@example.com",
     *   ...other profile fields
     * }
     */
    @PatchMapping(value = "/me", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserProfile> updateMyEmail(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> updates) {
        String userId = jwt.getSubject();
        
        // Find existing user
        UserProfile profile = userRepo.findById(userId).orElse(null);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Update email if provided
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            if (newEmail == null || newEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            profile.setEmail(newEmail.trim());
        }
        
        // Save and return updated profile
        userRepo.save(profile);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * DELETE /profile/me - Delete the authenticated user's account
     * Cascades deletion to all associated data (messages, room players)
     * 
     * Postman Test:
     * DELETE http://localhost:8080/profile/me
     * Headers: Authorization: Bearer {your_jwt_token}
     * 
     * Response: 200 OK
     * {
     *   "message": "User account deleted successfully",
     *   "userId": "user-id-here"
     * }
     */
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Map<String, String>> deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        
        // Check if user exists
        if (!userRepo.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        // Delete associated data
        // 1. Delete all messages from this user
        messageRepo.deleteAll(
            messageRepo.findAll().stream()
                .filter(m -> userId.equals(m.getUserId()))
                .toList()
        );
        
        // 2. Delete all room player entries
        roomPlayerRepo.deleteAll(
            roomPlayerRepo.findAll().stream()
                .filter(rp -> userId.equals(rp.getOderId()))
                .toList()
        );
        
        // 3. Delete the user profile
        userRepo.deleteById(userId);
        
        return ResponseEntity.ok(Map.of(
            "message", "User account deleted successfully",
            "userId", userId
        ));
    }
}
