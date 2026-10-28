package com.themepark.service;

import com.themepark.dto.Requests;
import com.themepark.repository.UserRepository;
import com.themepark.util.ApiException;
import com.themepark.util.PasswordUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

@Service
public class AuthService {
    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public Map<String, Object> register(Requests.Register r) {
        if (r == null || blank(r.name()) || blank(r.email()) || blank(r.password())) {
            throw ApiException.badRequest("Name, email and password are required.");
        }
        String email = r.email().trim().toLowerCase();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ApiException.badRequest("Enter a valid email address.");
        if (r.password().length() < 6) throw ApiException.badRequest("Password must be at least 6 characters.");
        if (r.name().trim().length() > 50) throw ApiException.badRequest("Name is too long (max 50 characters).");
        if (users.findVisitorByEmail(email).isPresent()) throw ApiException.conflict("An account with this email already exists.");

        Integer age = null;
        if (r.dob() != null) {
            if (r.dob().isAfter(LocalDate.now())) throw ApiException.badRequest("Date of birth cannot be in the future.");
            age = Period.between(r.dob(), LocalDate.now()).getYears();
        }
        int id = users.insertVisitor(r.name().trim(), r.gender(), age, r.dob(),
                r.phone() == null ? null : r.phone().trim(), email, PasswordUtil.hash(r.password()));
        return Map.of("id", id, "name", r.name().trim());
    }

    public Map<String, Object> loginVisitor(Requests.Login r) {
        Map<String, Object> row = users.findVisitorByEmail(r == null || r.email() == null ? "" : r.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        if (!PasswordUtil.matches(r.password() == null ? "" : r.password(), (String) row.get("password"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        return Map.of("id", row.get("id"), "name", row.get("name"));
    }

    public Map<String, Object> loginStaff(Requests.Login r) {
        Map<String, Object> row = users.findStaffByEmail(r == null || r.email() == null ? "" : r.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        if (!PasswordUtil.matches(r.password() == null ? "" : r.password(), (String) row.get("password"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        return Map.of("id", row.get("id"), "name", row.get("name"), "role", String.valueOf(row.get("role")));
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
