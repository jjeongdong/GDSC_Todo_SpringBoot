package com.example.todo.service;

import com.example.todo.config.JwtTokenProvider;
import com.example.todo.dto.TokenResponse;
import com.example.todo.dto.UserRequestDto;
import com.example.todo.dto.UserResponseDto;
import com.example.todo.entity.RefreshToken;
import com.example.todo.entity.User;
import com.example.todo.exception.ErrorCode;
import com.example.todo.exception.CustomException;
import com.example.todo.repository.RefreshTokenRepository;
import com.example.todo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncryptionService passwordEncryptionService;


    @Transactional
    public UserResponseDto signup(UserRequestDto userRequestDto) {
        User user = User.builder()
                .username(userRequestDto.getUsername())
                .password(passwordEncryptionService.encrypt(userRequestDto.getPassword()))
                .build();

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new CustomException(ErrorCode.DUPLICATE_USER_ID);
        }

        userRepository.save(user);

        return UserResponseDto.builder()
                .username(userRequestDto.getUsername())
                .build();
    }

    @Transactional
    public TokenResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user != null && Objects.equals(user.getPassword(), passwordEncryptionService.encrypt(password))) {

            Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findTopByUserOrderByIdDesc(user);

            if (optionalRefreshToken.isPresent()) {
                String DBRefreshToken = optionalRefreshToken.get().getToken();

                if (jwtTokenProvider.validateToken(DBRefreshToken) && DBRefreshToken != null) {
                    String accessToken = jwtTokenProvider.generateAccessToken(username);
                    return new TokenResponse(accessToken, DBRefreshToken);
                }
            }

            String accessToken = jwtTokenProvider.generateAccessToken(username);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .token(newRefreshToken)
                    .user(user)
                    .build();

            refreshTokenRepository.save(refreshTokenEntity);

            return new TokenResponse(accessToken, newRefreshToken);
        } else {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }
    }

}
