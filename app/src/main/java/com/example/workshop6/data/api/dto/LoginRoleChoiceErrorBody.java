package com.example.workshop6.data.api.dto;

import java.util.List;

/** 409 CONFLICT body from {@code /api/v1/auth/login} when employee + customer share a link and credentials match both. */
public class LoginRoleChoiceErrorBody {
    public String message;
    public List<Choice> choices;

    public static class Choice {
        public String username;
        public String role;
        public String label;
    }
}
