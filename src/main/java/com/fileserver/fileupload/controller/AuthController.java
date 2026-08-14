package com.fileserver.fileupload.controller;

import com.fileserver.fileupload.dto.LoginRequest;
import com.fileserver.fileupload.dto.LoginResponse;
import com.fileserver.fileupload.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fileserver.fileupload.entity.ActivityAction;
import com.fileserver.fileupload.service.ActivityLogService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ActivityLogService activityLogService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, ActivityLogService activityLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.activityLogService = activityLogService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(request.getUsername(), request.getPassword());

        try {
            Authentication authentication = authenticationManager.authenticate(authenticationRequest);

            String accessToken = jwtService.generateToken(authentication);

            activityLogService.log(authentication.getName(), ActivityAction.LOGIN, true, "POST", "/api/auth/login", null, "Login successful");

            return ResponseEntity.ok(new LoginResponse(accessToken));

        } catch (AuthenticationException exception) {

            activityLogService.logFailure(request.getUsername(), ActivityAction.LOGIN, "POST", "/api/auth/login", null, "Invalid username or password");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


}