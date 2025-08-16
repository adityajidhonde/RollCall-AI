package com.example.attendance;

import android.util.Log;
import java.io.*;

public class FaceEmbeddingUtils {
    private static final String TAG = "FaceEmbeddingUtils";

    /**
     * Load face embedding from file
     */
    public static float[] loadEmbedding(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return null;

            FileInputStream inputStream = new FileInputStream(file);
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            int size = dataInputStream.readInt();
            float[] embedding = new float[size];

            for (int i = 0; i < size; i++) {
                embedding[i] = dataInputStream.readFloat();
            }

            dataInputStream.close();
            inputStream.close();

            return embedding;
        } catch (Exception e) {
            Log.e(TAG, "Error loading embedding", e);
            return null;
        }
    }

    /**
     * Calculate cosine similarity between two embeddings
     * Returns value between -1 and 1, where 1 is identical
     */
    public static float calculateSimilarity(float[] embedding1, float[] embedding2) {
        if (embedding1.length != embedding2.length) return -1f;

        float dotProduct = 0f;
        float norm1 = 0f;
        float norm2 = 0f;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        norm1 = (float) Math.sqrt(norm1);
        norm2 = (float) Math.sqrt(norm2);

        if (norm1 > 0 && norm2 > 0) {
            return dotProduct / (norm1 * norm2);
        } else {
            return -1f;
        }
    }

    /**
     * Check if two faces are the same person
     * Threshold of 0.6 is commonly used for face recognition
     */
    public static boolean isSamePerson(float[] embedding1, float[] embedding2, float threshold) {
        float similarity = calculateSimilarity(embedding1, embedding2);
        return similarity >= threshold;
    }

    public static boolean isSamePerson(float[] embedding1, float[] embedding2) {
        return isSamePerson(embedding1, embedding2, 0.6f);
    }
}