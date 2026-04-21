// Contributor(s): Owen
// Main: Owen - Error body when login requires role choice between linked accounts.

package com.example.workshop6.data.api.dto;

import java.util.List;

/**
 * Conflict JSON from auth login when linked employee and customer accounts both match the password.
 */
public class LoginRoleChoiceErrorBody {
    public String message;
    public List<Choice> choices;

    public static class Choice {
        public String username;
        public String role;
        public String label;
    }
}
