package com.ppesafety.api.auth.service;

import com.ppesafety.api.auth.dto.AuthResponse;
import com.ppesafety.api.auth.dto.LoginRequest;
import com.ppesafety.api.auth.dto.RegisterRequest;
import com.ppesafety.api.config.JwtTokenProvider;
import com.ppesafety.api.core.annotation.Auditable;
import com.ppesafety.api.core.exception.BadRequestException;
import com.ppesafety.api.user.dto.UserDto;
import com.ppesafety.api.user.entity.User;
import com.ppesafety.api.user.mapper.UserMapper;
import com.ppesafety.api.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    public AuthService(AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
    }

    @Auditable(action = "LOGIN")
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = (User) authentication.getPrincipal();
        UserDto userDto = userMapper.toDto(user);

        logger.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.of(token, jwtExpiration / 1000, userDto);
    }

    @Auditable(action = "REGISTER")
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {}", savedUser.getUsername());

        String token = tokenProvider.generateToken(savedUser.getUsername());
        UserDto userDto = userMapper.toDto(savedUser);

        return AuthResponse.of(token, jwtExpiration / 1000, userDto);
    }
}
