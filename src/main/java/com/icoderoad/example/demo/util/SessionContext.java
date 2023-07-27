package com.icoderoad.example.demo.util;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

public class SessionContext {
    private static final Map<String, HttpSession> sessionMap = new HashMap<>();

    public static synchronized void addSession(HttpSession session) {
        if (session != null) {
            sessionMap.put(session.getId(), session);
        }
    }

    public static synchronized void removeSession(HttpSession session) {
        if (session != null) {
            sessionMap.remove(session.getId());
        }
    }

    public static synchronized HttpSession getSession(String sessionId) {
        return sessionMap.get(sessionId);
    }
}