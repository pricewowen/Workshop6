// Contributor(s): Robbie
// Main: Robbie - Transfer thread ownership between staff recipients.

package com.example.workshop6.data.api.dto;

/**
 * Gson body to transfer a chat thread to another staff user on Workshop 7.
 */
public class TransferThreadRequest {
    public String employeeUserId;

    public TransferThreadRequest(String employeeUserId) {
        this.employeeUserId = employeeUserId;
    }
}
