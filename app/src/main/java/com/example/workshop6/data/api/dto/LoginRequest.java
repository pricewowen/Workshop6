// Contributor(s): Owen
// Main: Owen - Login POST body for Workshop 7 auth including linked-account username resolution.

package com.example.workshop6.data.api.dto;

/**
 * JSON body for {@code POST api/v1/auth/login}. First sign-in sends email or username in the email
 * field and password together. After a linked-account conflict response call {@link #forResolvedUsername}
 * so only username and password are sent.
 */
public class LoginRequest {
    public String email;
    public String username;
    public final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static LoginRequest forResolvedUsername(String username, String password) {
        LoginRequest r = new LoginRequest(null, password);
        r.username = username;
        return r;
    }
}
