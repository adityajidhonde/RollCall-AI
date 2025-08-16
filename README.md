🏫 RollCall-AI: Android App
An Android application for student attendance tracking using face detection. This app allows a teacher or administrator to register students by name and ID, capturing their photo, and later taking attendance by comparing new photos to the database of registered faces.

✨ Key Features
Student Registration: Register new students by entering their name and ID and capturing a photo.

Attendance Tracking: Use the device's camera to take a new photo and automatically match it against registered students.

Local Database: All student information (name, ID, and photo URI) is securely stored on the device using a Room database.

Camera Integration: Seamlessly integrates with CameraX for real-time camera preview and photo capture.

ML-Powered Face Detection: Utilizes Google's ML Kit for fast and efficient face detection.

🛠️ Technologies Used
Platform: Android

Language: Java

UI Toolkit: XML, Material Design

Camera: CameraX

Database: Room Persistence Library

Machine Learning: ML Kit Face Detection

🚀 Getting Started
Follow these steps to get a copy of the project up and running on your local machine.

Prerequisites
Android Studio: Ensure you have the latest version installed.

Android SDK: The app requires a minimum SDK version of 24.

Installation
Clone the repository:

git clone https://github.com/your-username/your-repo-name.git

Open in Android Studio:
Open Android Studio, select "Open an Existing Project," and navigate to the cloned directory.

Sync Gradle:
Android Studio will automatically prompt you to sync the Gradle files. If it doesn't, click the "Sync Project with Gradle Files" button in the toolbar.

Run the app:
Connect an Android device or start an emulator and click the "Run" button (▶️) in the toolbar.

✍️ Usage
Register a Student:

On the main screen, tap "Register Student."

Enter the student's name and ID.

Tap the "Register" button to take a photo. The student's information and photo will be saved to the local database.

Take Attendance:

On the main screen, tap "Take Attendance."

Point the camera at a student's face.

Tap the "Take Attendance" button. The app will detect the face and attempt to match it with a registered student.

A message will appear showing whose attendance was marked.

🤝 Contributing
Contributions are welcome! If you'd like to improve the app, please follow these steps:

Fork the repository.

Create a new branch for your feature (git checkout -b feature/your-feature-name).

Commit your changes (git commit -m 'feat: Add new feature').

Push to the branch (git push origin feature/your-feature-name).

Create a Pull Request.
