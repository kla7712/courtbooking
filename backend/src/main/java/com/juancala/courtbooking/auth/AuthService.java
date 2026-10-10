package com.juancala.courtbooking.auth;

import com.juancala.courtbooking.auth.dto.AuthResponse;
import com.juancala.courtbooking.auth.dto.LoginRequest;
import com.juancala.courtbooking.auth.dto.RegisterRequest;
import com.juancala.courtbooking.common.ConflictException;
import com.juancala.courtbooking.common.NotFoundException;
import com.juancala.courtbooking.common.UnauthorizedException;
import com.juancala.courtbooking.security.JwtService;
import com.juancala.courtbooking.user.Role;
import com.juancala.courtbooking.user.User;
import com.juancala.courtbooking.user.UserRepository;
import com.juancala.courtbooking.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** El registro público siempre crea socios; nunca administradores. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ya existe una cuenta con ese email");
        }
        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.MEMBER);
        return toAuthResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        // Mismo mensaje si falla el email o la contraseña, para no revelar qué cuentas existen
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Email o contraseña incorrectos"));
        return toAuthResponse(user);
    }

    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("El usuario ya no existe"));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                jwtService.getExpirationSeconds(),
                UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
