package com.themepark.repository;

import com.themepark.util.Db;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> findVisitorByEmail(String email) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT VisitorID AS id, Name AS name, Password AS password FROM VISITOR WHERE Email = ?", email);
        return rows.stream().findFirst();
    }

    public int insertVisitor(String name, String gender, Integer age, LocalDate dob, String phone,
                             String email, String passwordHash) {
        return Db.insert(jdbc,
                "INSERT INTO VISITOR (Name, Gender, Age, DOB, PhoneNo, Email, Password) VALUES (?,?,?,?,?,?,?)",
                name, gender, age, dob, phone, email, passwordHash);
    }

    public Optional<Map<String, Object>> findStaffByEmail(String email) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT StaffID AS id, Name AS name, Role AS role, Password AS password FROM STAFF WHERE Email = ?", email);
        return rows.stream().findFirst();
    }
}
