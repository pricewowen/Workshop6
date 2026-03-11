package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "bakery_hours",
        foreignKeys = @ForeignKey(
                entity = BakeryLocation.class,
                parentColumns = "bakeryId",
                childColumns = "bakeryId"
        ),
        indices = { @Index("bakeryId") }
)
public class BakeryHours {
    @PrimaryKey(autoGenerate = true)
    public int bakeryHoursId;
    public int bakeryId;
    public int dayOfWeek; // 1 = Monday ... 7 = Sunday
    // Stored as 24-hour text time in SQLite format HH:mm (e.g. "07:00", "19:30")
    public String openTime;
    public String closeTime;
    public boolean isClosed;

    public BakeryHours(int bakeryId, int dayOfWeek, String openTime, String closeTime, boolean isClosed) {
        this.bakeryId = bakeryId;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.isClosed = isClosed;
    }
}
