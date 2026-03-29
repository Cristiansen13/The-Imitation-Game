package imitationgame.chatservice.controller;

import imitationgame.chatservice.model.UserProfile;
import imitationgame.chatservice.repository.MessageLogRepository;
import imitationgame.chatservice.repository.RoomPlayerRepository;
import imitationgame.chatservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class ProfileControllerTest {

    private UserProfileRepository userRepo;
    private MessageLogRepository messageRepo;
    private RoomPlayerRepository roomPlayerRepo;
    private ProfileController controller;

    @BeforeEach
    void setUp() {
        userRepo = Mockito.mock(UserProfileRepository.class);
        messageRepo = Mockito.mock(MessageLogRepository.class);
        roomPlayerRepo = Mockito.mock(RoomPlayerRepository.class);
        controller = new ProfileController(userRepo, messageRepo, roomPlayerRepo);
    }

    private Jwt jwtToken() {
        return Jwt.withTokenValue("t")
                .header("alg", "none")
                .subject("user-1")
                .claim("preferred_username", "mihai")
                .claim("email", "mihai@example.com")
                .build();
    }

    @Test
    void me_ok() {
        UserProfile profile = new UserProfile();
        profile.setId("user-1");
        Mockito.when(userRepo.findById("user-1")).thenReturn(Optional.of(profile));
        Mockito.when(userRepo.save(any(UserProfile.class))).thenReturn(profile);

        assertEquals(HttpStatus.OK, controller.me(jwtToken()).getStatusCode());
    }

    @Test
    void updateMyEmail_ok() {
        UserProfile profile = new UserProfile();
        profile.setId("user-1");
        Mockito.when(userRepo.findById("user-1")).thenReturn(Optional.of(profile));
        Mockito.when(userRepo.save(any(UserProfile.class))).thenReturn(profile);

        assertEquals(HttpStatus.OK, controller.updateMyEmail(jwtToken(), Map.of("email", "new@example.com")).getStatusCode());
    }

    @Test
    void deleteMyAccount_ok() {
        Mockito.when(userRepo.existsById("user-1")).thenReturn(true);
        Mockito.when(messageRepo.findAll()).thenReturn(List.of());
        Mockito.when(roomPlayerRepo.findAll()).thenReturn(List.of());

        assertEquals(HttpStatus.OK, controller.deleteMyAccount(jwtToken()).getStatusCode());
    }
}
