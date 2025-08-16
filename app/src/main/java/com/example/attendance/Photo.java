package com.example.attendance;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "photos")
public class Photo {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String filePath;
    public long timestamp;

    public Photo(String filePath, long timestamp) {
        this.filePath = filePath;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getFilePath() { return filePath; }

    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getTimestamp() { return timestamp; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
