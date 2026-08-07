package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.UserDao;
import com.braculink.dto.AuthResponse;
import com.braculink.dto.LoginRequest;
import com.braculink.dto.SignupRequest;
import com.braculink.dto.SignupResponse;
import com.braculink.dto.VerifyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Walks the signup -&gt; OTP -&gt; verify -&gt; login flow against a real database, through the real
 * services and DAOs.
 *
 * <p>The console print is the delivery channel in this project, so the test reads the code straight
 * out of the {@code otp} table instead — same row the console line was printed from.
 */
@SpringBootTest
@ActiveProfiles("test")
class OtpSignupFlowTest {

    private static final String EMAIL = "otp.flow@g.bracu.ac.bd";
    private static final String PASSWORD = "supersecret1";

    /** Stops the course sync scheduler making a real network call during tests. */
    @MockitoBean
    private CourseSyncService courseSyncService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void reset() {
        jdbcTemplate.execute("DELETE FROM otp");
        jdbcTemplate.execute("DELETE FROM swap_request");
        jdbcTemplate.execute("DELETE FROM enrollment");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM notification");
        jdbcTemplate.execute("DELETE FROM user");
    }

    @Test
    void signupThenVerifyThenLogin() {
        SignupResponse signup = authService.signup(signupRequest());
        assertNotNull(signup.getUserId());

        // Signup stored exactly one code for this email, 6 digits, expiring 10 minutes out.
        assertEquals(1, countOtpRows());
        String code = storedCode();
        assertTrue(code.matches("\\d{6}"), "OTP should be 6 digits, was " + code);
        assertExpiryRoughlyTenMinutesOut();

        // Not usable until verified.
        assertFalse(userDao.findByBracuEmail(EMAIL).orElseThrow().isEmailVerified());
        ApiException unverified = assertThrows(ApiException.class,
                () -> authService.login(loginRequest()));
        assertEquals("Email not verified", unverified.getMessage());

        authService.verify(verifyRequest(code));

        // Verified, and the code is consumed.
        assertTrue(userDao.findByBracuEmail(EMAIL).orElseThrow().isEmailVerified());
        assertEquals(0, countOtpRows(), "the otp row should be deleted on verify");

        AuthResponse login = authService.login(loginRequest());
        assertNotNull(login.getToken());
        assertEquals(EMAIL, login.getBracuEmail());
    }

    @Test
    void aWrongCodeIsRejectedAndTheRowSurvives() {
        authService.signup(signupRequest());
        String realCode = storedCode();
        String wrongCode = realCode.equals("000000") ? "111111" : "000000";

        ApiException rejected = assertThrows(ApiException.class,
                () -> authService.verify(verifyRequest(wrongCode)));
        assertEquals("Invalid or expired OTP", rejected.getMessage());

        assertFalse(userDao.findByBracuEmail(EMAIL).orElseThrow().isEmailVerified());
        assertEquals(1, countOtpRows(), "a failed attempt must not consume the code");
    }

    @Test
    void anExpiredCodeIsRejected() {
        authService.signup(signupRequest());
        String code = storedCode();

        jdbcTemplate.update("UPDATE otp SET expires_at = ? WHERE email = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)), EMAIL);

        ApiException rejected = assertThrows(ApiException.class,
                () -> authService.verify(verifyRequest(code)));
        assertEquals("Invalid or expired OTP", rejected.getMessage());
        assertFalse(userDao.findByBracuEmail(EMAIL).orElseThrow().isEmailVerified());
    }

    /**
     * The email is the primary key, so re-issuing replaces rather than accumulates — and the old
     * code stops working the moment the new one is written.
     */
    @Test
    void reissuingACodeOverwritesTheOldOne() {
        authService.signup(signupRequest());
        String firstCode = storedCode();

        // Simulate a re-send: same DAO call signup makes.
        String secondCode = firstCode.equals("000000") ? "111111" : "000000";
        jdbcTemplate.update("INSERT INTO otp (email, code, expires_at) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE code = VALUES(code), expires_at = VALUES(expires_at)",
                EMAIL, secondCode, Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)));

        assertEquals(1, countOtpRows(), "one live code per email");
        assertEquals(secondCode, storedCode());

        assertThrows(ApiException.class, () -> authService.verify(verifyRequest(firstCode)));
        authService.verify(verifyRequest(secondCode));
        assertTrue(userDao.findByBracuEmail(EMAIL).orElseThrow().isEmailVerified());
    }

    // ------------------------------------------------------------------ helpers

    private int countOtpRows() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM otp WHERE email = ?", Integer.class, EMAIL);
        return count == null ? 0 : count;
    }

    private String storedCode() {
        return jdbcTemplate.queryForObject(
                "SELECT code FROM otp WHERE email = ?", String.class, EMAIL);
    }

    private void assertExpiryRoughlyTenMinutesOut() {
        LocalDateTime expiresAt = jdbcTemplate.queryForObject(
                "SELECT expires_at FROM otp WHERE email = ?", Timestamp.class, EMAIL).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();
        assertTrue(expiresAt.isAfter(now.plusMinutes(9)) && expiresAt.isBefore(now.plusMinutes(11)),
                "expires_at should be ~10 minutes out, was " + expiresAt);
    }

    private SignupRequest signupRequest() {
        SignupRequest request = new SignupRequest();
        request.setFullName("Otp Flow");
        request.setStudentId("20109999");
        request.setBracuEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private VerifyRequest verifyRequest(String code) {
        VerifyRequest request = new VerifyRequest();
        request.setBracuEmail(EMAIL);
        request.setOtp(code);
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setBracuEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }
}
