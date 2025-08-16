package com.example.attendance;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PhotoDao {
    @Insert
    void insertPhoto(Photo photo);

    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    List<Photo> getAllPhotos();
}