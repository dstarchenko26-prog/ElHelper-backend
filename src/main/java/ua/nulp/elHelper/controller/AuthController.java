package ua.nulp.elHelper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.nulp.elHelper.service.AuthService;
import ua.nulp.elHelper.service.dto.auth.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PatchMapping("/confirm")
    public String confirm(@RequestParam("token") String token) {
        return authService.confirmToken(token);
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> resendConfirm(@RequestBody ResendCTRequest request) {
        authService.resendConfirmToken(request);
        return ResponseEntity.ok("Confirm email resend");
    }

    @PostMapping("/link-login")
    public ResponseEntity<String> sendLinkLogin(@RequestBody LTLinkRequest request) {
        authService.sendLinkLogin(request);
        return ResponseEntity.ok("Login email send");
    }

    @GetMapping("/link-login")
    public ResponseEntity<AuthResponse> LinkLogin(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.loginWithLinkToken(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody FPRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("Password reset send");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody RPRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password successfully changed");
    }

    @PatchMapping("/special")
    public ResponseEntity<String> special (@RequestBody SPRequest request) {
        authService.special(request);
        return ResponseEntity.ok("Success");
    }
}