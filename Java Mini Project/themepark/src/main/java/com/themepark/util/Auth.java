package com.themepark.util;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;

/** Session helpers: who is logged in? */
public final class Auth {
    public static final String VISITOR = "visitorId";
    public static final String STAFF = "staffId";

    private Auth() {}

    public static int visitor(HttpSession session) {
        Object o = session.getAttribute(VISITOR);
        if (o == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
        return (Integer) o;
    }

    public static int staff(HttpSession session) {
        Object o = session.getAttribute(STAFF);
        if (o == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Session expired. Please log in again.");
        return (Integer) o;
    }
}
