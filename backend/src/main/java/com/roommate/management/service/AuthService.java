package com.roommate.management.service;

import com.roommate.management.dto.request.ForgotPasswordRequest;
import com.roommate.management.dto.request.LoginRequest;
import com.roommate.management.dto.request.RegisterRequest;
import com.roommate.management.dto.request.ResetPasswordRequest;
import com.roommate.management.dto.response.AuthResponse;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.User;
import com.roommate.management.entity.enums.MemberStatus;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ConflictException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.exception.UnauthorizedException;
import com.roommate.management.repository.RoomMemberRepository;
import com.roommate.management.repository.UserRepository;
import com.roommate.management.security.JwtUtil;
import com.roommate.management.util.RandomPasswordGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .mobileNumber(request.mobileNumber())
                .enabled(true)
                .build();
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedException("This account has been disabled");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(request.email());
        // Always behave the same way whether or not the email exists, to avoid leaking which emails are registered.
        userOpt.ifPresent(user -> {
            user.setResetToken(RandomPasswordGenerator.generateToken());
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            // NOTE: In production this token would be emailed to the user via JavaMailSender.
            // For local development the token can be read from the database directly.
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        Optional<RoomMember> membership = roomMemberRepository.findByUserIdAndStatus(user.getId(), MemberStatus.ACTIVE);
        Long roomId = membership.map(m -> m.getRoom().getId()).orElse(null);
        String roomName = membership.map(m -> m.getRoom().getRoomName()).orElse(null);
        String role = membership.map(m -> m.getRole().name()).orElse(null);
        return new AuthResponse(token, "Bearer", user.getId(), user.getFullName(), user.getEmail(), roomId, roomName, role);
    }
}
