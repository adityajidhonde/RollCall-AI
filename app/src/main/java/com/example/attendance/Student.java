package com.example.attendance;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "students")
public class Student {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String studentId;
    public String name;
    public String photoUri;

    public Student(String studentId, String name, String photoUri) {
        this.studentId = studentId;
        this.name = name;
        this.photoUri = photoUri;
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }

    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getPhotoUri() { return photoUri; }

    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
}