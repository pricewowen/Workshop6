package com.example.workshop6;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.data.model.ChatMessage;
import com.example.workshop6.data.model.ChatThread;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Order;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.RewardTier;
import com.example.workshop6.data.model.User;
import com.example.workshop6.util.SearchUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SqlSafetyInstrumentedTest {

    private AppDatabase db;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void loginQueries_treatMaliciousUsernameAsPlainData() {
        User user = new User();
        user.userUsername = "safeuser";
        user.userEmail = "safe@bakery.com";
        user.userPasswordHash = "hash";
        user.userRole = "CUSTOMER";
        user.isActive = true;
        user.userCreatedAt = System.currentTimeMillis();
        db.userDao().insert(user);

        User result = db.userDao().getUserByUsername("' OR 1=1 --");
        assertNull(result);
        assertNotNull(db.userDao().getUserByUsername("safeuser"));
    }

    @Test
    public void productSearch_escapesWildcardPayloads() {
        db.productDao().insert(new Product(1, "100% Rye", "Percent bread", 4.0, 0));
        db.productDao().insert(new Product(2, "Underscore_Bun", "Underscore bread", 5.0, 0));
        db.productDao().insert(new Product(3, "Plain Bun", "Regular bread", 3.0, 0));

        List<Product> percentResults = db.productDao().searchProducts(SearchUtils.normalizeUserSearch("%"));
        assertEquals(1, percentResults.size());
        assertEquals("100% Rye", percentResults.get(0).getProductName());

        List<Product> underscoreResults = db.productDao().searchProducts(SearchUtils.normalizeUserSearch("_"));
        assertEquals(1, underscoreResults.size());
        assertEquals("Underscore_Bun", underscoreResults.get(0).getProductName());
    }

    @Test
    public void addressMatching_handlesQuotedPayloadAsData() {
        Address address = new Address();
        address.addressLine1 = "12 O'Neil St; DROP TABLE";
        address.addressLine2 = null;
        address.addressCity = "Calgary";
        address.addressProvince = "Alberta";
        address.addressPostalCode = "T2P 1A1";
        long addressId = db.addressDao().insert(address);

        Address matched = db.addressDao().findMatchingAddress(
                "12 O'Neil St; DROP TABLE",
                null,
                "Calgary",
                "Alberta",
                "T2P 1A1"
        );

        assertNotNull(matched);
        assertEquals((int) addressId, matched.addressId);
    }

    @Test
    public void chatMessages_storeMaliciousLookingTextSafely() {
        User customer = insertUser("chatuser", "chat@bakery.com");

        ChatThread thread = new ChatThread();
        thread.customerUserId = customer.userId;
        thread.employeeUserId = null;
        thread.status = "OPEN";
        thread.createdAt = System.currentTimeMillis();
        thread.updatedAt = System.currentTimeMillis();
        long threadId = db.chatDao().insertThread(thread);

        String payload = "'; DROP TABLE chat_message; --";
        ChatMessage message = new ChatMessage();
        message.threadId = (int) threadId;
        message.senderUserId = customer.userId;
        message.messageText = payload;
        message.sentAt = System.currentTimeMillis();
        message.isRead = false;
        db.chatDao().insertMessage(message);

        List<ChatMessage> messages = db.chatDao().getMessagesForThread((int) threadId);
        assertEquals(1, messages.size());
        assertEquals(payload, messages.get(0).messageText);
    }

    @Test
    public void orderComment_storesMaliciousLookingTextSafely() {
        db.rewardTierDao().insert(new RewardTier(1, "Default", 0, 9999, "Default tier"));

        User customerUser = insertUser("orderuser", "order@bakery.com");

        Address customerAddress = new Address();
        customerAddress.addressLine1 = "1 Baker St";
        customerAddress.addressLine2 = null;
        customerAddress.addressCity = "Toronto";
        customerAddress.addressProvince = "Ontario";
        customerAddress.addressPostalCode = "M5V1A1";
        int customerAddressId = (int) db.addressDao().insert(customerAddress);

        Customer customer = new Customer();
        customer.userId = customerUser.userId;
        customer.addressId = customerAddressId;
        customer.rewardTierId = 1;
        customer.customerFirstName = "Order";
        customer.customerLastName = "User";
        customer.customerRole = "Customer";
        customer.customerPhone = "(555) 555-0000";
        customer.customerRewardBalance = 0;
        customer.customerEmail = "order@bakery.com";
        int customerId = (int) db.customerDao().insert(customer);

        Address bakeryAddress = new Address();
        bakeryAddress.addressLine1 = "99 Flour Ave";
        bakeryAddress.addressLine2 = null;
        bakeryAddress.addressCity = "Toronto";
        bakeryAddress.addressProvince = "Ontario";
        bakeryAddress.addressPostalCode = "M5V1B1";
        int bakeryAddressId = (int) db.addressDao().insert(bakeryAddress);

        BakeryLocation bakery = new BakeryLocation();
        bakery.name = "Safe Bakery";
        bakery.addressId = bakeryAddressId;
        bakery.phone = "(555) 555-1111";
        bakery.email = "safe@bakery.com";
        bakery.status = "Open";
        bakery.latitude = 0;
        bakery.longitude = 0;
        int bakeryId = (int) db.bakeryLocationDao().insert(bakery);

        String payload = "\"; DROP TABLE orders; --";
        Order order = new Order(
                0,
                customerId,
                bakeryId,
                customerAddressId,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                "pickup",
                payload,
                12.0,
                0.0,
                "pending"
        );

        long orderId = db.orderDao().insert(order);
        Order stored = db.orderDao().getOrderById((int) orderId);

        assertNotNull(stored);
        assertEquals(payload, stored.getOrderComment());
        assertTrue(db.orderDao().getAllOrders().size() >= 1);
    }

    private User insertUser(String username, String email) {
        User user = new User();
        user.userUsername = username;
        user.userEmail = email;
        user.userPasswordHash = "hash";
        user.userRole = "CUSTOMER";
        user.isActive = true;
        user.userCreatedAt = System.currentTimeMillis();
        long userId = db.userDao().insert(user);
        user.userId = (int) userId;
        return user;
    }
}
