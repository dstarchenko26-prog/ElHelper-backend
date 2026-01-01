package ua.nulp.elHelper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.nulp.elHelper.entity.user.AdminRequest;
import ua.nulp.elHelper.service.dto.user.*;
import ua.nulp.elHelper.entity.user.User;
import ua.nulp.elHelper.entity.jsonb.SimpleSettings;
import ua.nulp.elHelper.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserByEmail(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getPublicProfile(id));
    }

    @PatchMapping("/profile")
    public ResponseEntity<User> updateProfile(Authentication authentication, @RequestBody ChangeProfileDto dto) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), dto));
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(Authentication authentication, @RequestBody SimpleSettings settings) {
        userService.updateSettings(authentication.getName(), settings);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, Authentication authentication) {
        String url = userService.uploadAvatar(authentication.getName(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    @PostMapping("/password")
    public ResponseEntity<String> changePassword(Authentication authentication, @RequestBody ChangePasswordDto dto) {
        userService.changePassword(authentication.getName(), dto);
        return ResponseEntity.ok("Password successfully changed");
    }

    @GetMapping("/my-admin-request")
    public ResponseEntity<AdminRequest> getAdminRequest(Authentication authentication) {
        return ResponseEntity.ok(userService.getAdminRequest(authentication.getName()));
    }

    @PostMapping("/my-admin-request")
    public ResponseEntity<String> requestAdmin(Authentication authentication, @RequestBody Map<String, String> body) {
        userService.createAdminRequest(authentication.getName(), body.get("reason"));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/my-admin-request")
    public ResponseEntity<String> archiveRequest(Authentication authentication, @RequestBody Map<String, Long> body) {
        userService.archiveRequest(authentication.getName(), body.get("id"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin-request")
    public ResponseEntity<List<AdminRequest>> adminRequest() {
        return ResponseEntity.ok(userService.getAllAdminRequest());
    }

    @PostMapping("/admin-request/{id}")
    public ResponseEntity<String> responseAdminRequest(@PathVariable Long id, @RequestBody RespAdmReqDto request) {
        userService.responseAdminRequest(id, request);
        return ResponseEntity.ok().build();
    }
}