package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.AuthResponse;
import com.braculink.dto.LoginRequest;
import com.braculink.dto.SignupRequest;
import com.braculink.dto.SignupResponse;
import com.braculink.dto.VerifyRequest;
import com.braculink.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Signup with a @g.bracu.ac.bd email, OTP verification, and login")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @Operation(summary = "Register with a BRACU email and receive an OTP")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("OTP sent, check the server console", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify the OTP sent at signup")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyRequest request) {
        String message = authService.verify(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
