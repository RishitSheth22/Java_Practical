package com.themepark.controller;

import com.themepark.dto.Requests;
import com.themepark.service.AuthService;
import com.themepark.util.Auth;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Requests.Register r, HttpSession session) {
        Map<String, Object> v = auth.register(r);
        session.setAttribute(Auth.VISITOR, v.get("id"));
        session.setAttribute("visitorName", v.get("name"));
        return v;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Requests.Login r, HttpSession session) {
        Map<String, Object> v = auth.loginVisitor(r);
        session.setAttribute(Auth.VISITOR, v.get("id"));
        session.setAttribute("visitorName", v.get("name"));
        return v;
    }

    @PostMapping("/staff-login")
    public Map<String, Object> staffLogin(@RequestBody Requests.Login r, HttpSession session) {
        Map<String, Object> s = auth.loginStaff(r);
        session.setAttribute(Auth.STAFF, s.get("id"));
        session.setAttribute("staffName", s.get("name"));
        return s;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    /** Who am I? Used by the pages on load. */
    @GetMapping("/me")
    public Map<String, Object> me(HttpSession session) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visitor", session.getAttribute(Auth.VISITOR) == null ? null : session.getAttribute("visitorName"));
        m.put("staff", session.getAttribute(Auth.STAFF) == null ? null : session.getAttribute("staffName"));
        return m;
    }
}
