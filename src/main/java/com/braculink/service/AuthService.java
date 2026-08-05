package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.common.util.EmailDomainValidator;
import com.braculink.dao.OtpDao;
import com.braculink.dao.UserDao;
import com.braculink.dto.AuthResponse;
import com.braculink.dto.LoginRequest;
import com.braculink.dto.SignupRequest;
import com.braculink.dto.SignupResponse;
import com.braculink.dto.VerifyRequest;
import com.braculink.model.User;
import com.braculink.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final int OTP_MIN = 100000;
    private static final int OTP_RANGE = 900000;
    private static final long OTP_VALIDITY_MINUTES = 10;

    private final UserDao userDao;
    private final OtpDao otpDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserDao userDao, OtpDao otpDao, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userDao = userDao;
        this.otpDao = otpDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public SignupResponse signup(SignupRequest request) {
        if (!EmailDomainValidator.isValidBracuEmail(request.getBracuEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email must end with @g.bracu.ac.bd");
        }
        if (userDao.findByBracuEmail(request.getBracuEmail()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userDao.findByStudentId(request.getStudentId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Student ID already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setStudentId(request.getStudentId());
        user.setBracuEmail(request.getBracuEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhonePublic(false);
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        Long userId = userDao.save(user);

        String code = generateOtp();
        otpDao.save(request.getBracuEmail(), code, LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        System.out.println("[Braculink] OTP for " + request.getBracuEmail() + ": " + code);

        return new SignupResponse(userId, request.getBracuEmail());
    }

    public String verify(VerifyRequest request) {
        if (!otpDao.isValid(request.getBracuEmail(), request.getOtp())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }
        User user = userDao.findByBracuEmail(request.getBracuEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        userDao.markVerified(user.getId());
        otpDao.deleteByEmail(request.getBracuEmail());
        return "Email verified successfully";
    }

    public AuthResponse login(LoginRequest request) {
        User user = userDao.findByBracuEmail(request.getBracuEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Email not verified");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getBracuEmail());
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getStudentId(), user.getBracuEmail());
    }

    private String generateOtp() {
        int code = OTP_MIN + secureRandom.nextInt(OTP_RANGE);
        return String.valueOf(code);
    }
}
