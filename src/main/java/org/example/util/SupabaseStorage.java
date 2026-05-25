package org.example.util;

import okhttp3.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SupabaseStorage {

    private static final String SUPABASE_URL;
    private static final String SUPABASE_KEY;
    private static final String BUCKET;

    private static final OkHttpClient client;

    static {
        try {
            Properties props = new Properties();

            try (InputStream input = SupabaseStorage.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties")) {

                if (input == null) {
                    throw new RuntimeException("config.properties not found");
                }

                props.load(input);
            }

            SUPABASE_URL = props.getProperty("supabase.url")
                    .replaceAll("/+$", "");

            SUPABASE_KEY = props.getProperty("supabase.key");

            BUCKET = props.getProperty("supabase.bucket", "item-images");

            if (SUPABASE_URL == null || SUPABASE_KEY == null) {
                throw new RuntimeException("Missing Supabase configuration!");
            }

            client = new OkHttpClient();

            System.out.println("Supabase Storage initialization successful!");

        } catch (Exception e) {
            throw new RuntimeException("Supabase Storage Configuration Error", e);
        }
    }

    /**
     * Upload ảnh lên Supabase Storage
     * @param file file ảnh
     * @return public URL nếu thành công, null nếu thất bại
     */
    public static String uploadImage(File file) {

        if (file == null || !file.exists()) {
            System.err.println("File does not exist!");
            return null;
        }

        String fileName = System.currentTimeMillis()
                + "_" + file.getName().replace(" ", "_");

        String uploadUrl = SUPABASE_URL
                + "/storage/v1/object/"
                + BUCKET
                + "/"
                + fileName;

        String mimeType;

        if (file.getName().toLowerCase().endsWith(".png")) {
            mimeType = "image/png";
        } else if (file.getName().toLowerCase().endsWith(".jpg")
                || file.getName().toLowerCase().endsWith(".jpeg")) {
            mimeType = "image/jpeg";
        } else {
            mimeType = "application/octet-stream";
        }

        RequestBody body = RequestBody.create(
                file,
                MediaType.parse(mimeType)
        );

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("x-upsert", "true")
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (response.isSuccessful()) {

                String publicUrl = SUPABASE_URL
                        + "/storage/v1/object/public/"
                        + BUCKET
                        + "/"
                        + fileName;

                System.out.println("Upload successful: " + publicUrl);

                return publicUrl;

            } else {

                String errorBody = response.body() != null
                        ? response.body().string()
                        : "Unknown error";

                System.err.println(
                        "Upload failed: "
                                + response.code()
                                + " - "
                                + errorBody
                );

                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}