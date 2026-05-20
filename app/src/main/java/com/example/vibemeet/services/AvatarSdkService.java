package com.example.vibemeet.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Avatar SDK Cloud API client.
 * Takes a selfie photo and generates a 3D head avatar (.glb) with ARKit blendshapes.
 *
 * Flow:
 *   1. Authenticate with client_id + client_secret  → access_token
 *   2. POST photo → returns avatar code + Queued status
 *   3. Poll avatar status until Completed (~30-90 seconds)
 *   4. Get list of exports
 *   5. Download GLB file to local storage
 */
public class AvatarSdkService {

    private static final String TAG = "AvatarSdkService";

    private static final String CLIENT_ID = "rkfqrbHvCYyR7LReqtPnpI3jkp2KEF4UoFlJKInm";
    private static final String CLIENT_SECRET = "BlDwlkMEa2KFLh753jyUoEJ6UbkNZcitmLz28XD2XEbKWqOqVoZgmhoaQOlcKBMyiaIXB1WmvDWVPbsRsDBX8LiqutN8Tos8X1icZ6VpdXoxlTFIR08JTMOKTcVurY4d";

    // Avatar SDK Cloud API endpoints (OAuth2)
    private static final String AUTH_URL = "https://api.avatarsdk.com/o/token/";
    private static final String API_BASE = "https://api.avatarsdk.com";

    // Try multiple pipelines — different accounts have access to different ones
    private static final String[] PIPELINE_CANDIDATES = {
            "head_2.0",
            "head_1.2",
            "mobile_2.0",
            "head"
    };

    // Pipeline subtype enables ARKit blendshapes
    private static final String PIPELINE_SUBTYPE = "head_2.0_blendshapes";

    public interface Listener {
        void onProgress(int percent, String message);
        void onSuccess(File glbFile);
        void onError(String error);
    }

    private final Context context;
    private final OkHttpClient httpClient;

    public AvatarSdkService(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    public void generateAvatar(File photoFile, Listener listener) {
        new Thread(() -> {
            try {
                listener.onProgress(5, "Authenticating with Avatar SDK…");
                String token = authenticate();
                Log.d(TAG, "Got access token");

                listener.onProgress(15, "Uploading your selfie…");
                String avatarCode = createAvatar(token, photoFile, listener);
                Log.d(TAG, "Avatar created: " + avatarCode);

                listener.onProgress(25, "Generating 3D avatar (≈ 1 minute)…");
                pollUntilCompleted(token, avatarCode, listener);

                listener.onProgress(85, "Downloading 3D model…");
                File glbFile = downloadGlb(token, avatarCode);
                Log.d(TAG, "Downloaded to " + glbFile.getAbsolutePath());

                listener.onProgress(100, "Done! 🎉");
                listener.onSuccess(glbFile);
            } catch (Exception e) {
                Log.e(TAG, "Avatar generation failed", e);
                listener.onError(e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        }, "AvatarSdkWorker").start();
    }

    // ============== STEP 1: Authentication ==============

    private String authenticate() throws Exception {
        FormBody form = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();

        Request req = new Request.Builder()
                .url(AUTH_URL)
                .post(form)
                .addHeader("Accept", "application/json")
                .build();

        Log.d(TAG, "Auth request → " + AUTH_URL);

        try (Response resp = httpClient.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            Log.d(TAG, "Auth response (" + resp.code() + "): " + truncate(body, 400));
            if (!resp.isSuccessful()) {
                throw new Exception("Auth " + resp.code() + ": " + truncate(body, 150));
            }
            JSONObject json = new JSONObject(body);
            return json.getString("access_token");
        }
    }

    // ============== STEP 2: Create avatar from photo ==============

    private String createAvatar(String token, File photo, Listener listener) throws Exception {
        // Try each pipeline candidate until one succeeds
        Exception lastError = null;
        for (String pipeline : PIPELINE_CANDIDATES) {
            try {
                Log.d(TAG, "Trying pipeline: " + pipeline);
                listener.onProgress(15, "Uploading (pipeline: " + pipeline + ")");
                return createAvatarWithPipeline(token, photo, pipeline);
            } catch (Exception e) {
                Log.w(TAG, "Pipeline " + pipeline + " failed: " + e.getMessage());
                lastError = e;
            }
        }
        throw new Exception("All pipelines failed. Last error: " +
                (lastError != null ? lastError.getMessage() : "?"));
    }

    private String createAvatarWithPipeline(String token, File photo, String pipeline) throws Exception {
        RequestBody fileBody = RequestBody.create(photo, MediaType.parse("image/jpeg"));
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("photo", "selfie.jpg", fileBody)
                .addFormDataPart("name", "VibeMeetTwin")
                .addFormDataPart("pipeline", pipeline);

        // Add subtype for ARKit blendshapes when using head_2.0
        if ("head_2.0".equals(pipeline)) {
            builder.addFormDataPart("pipeline_subtype", PIPELINE_SUBTYPE);
        }

        MultipartBody body = builder.build();

        Request req = new Request.Builder()
                .url(API_BASE + "/avatars/")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            String respBody = resp.body() != null ? resp.body().string() : "";
            Log.d(TAG, "Pipeline " + pipeline + " response (" + resp.code() + "): " + truncate(respBody, 300));
            if (!resp.isSuccessful()) {
                throw new Exception("Pipeline '" + pipeline + "' failed (" + resp.code() + "): " + truncate(respBody, 300));
            }
            return new JSONObject(respBody).getString("code");
        }
    }

    // ============== STEP 3: Poll for completion ==============

    private void pollUntilCompleted(String token, String code, Listener listener) throws Exception {
        long startTime = System.currentTimeMillis();
        int attempt = 0;

        while (true) {
            Thread.sleep(4000);
            attempt++;

            Request req = new Request.Builder()
                    .url(API_BASE + "/avatars/" + code + "/")
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            try (Response resp = httpClient.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    throw new Exception("Poll failed (" + resp.code() + "): " + truncate(body, 200));
                }

                JSONObject obj = new JSONObject(body);
                String status = obj.optString("status", "Unknown");
                int progress = obj.optInt("progress", -1);

                Log.d(TAG, "Poll " + attempt + ": status=" + status + " progress=" + progress);

                int displayPercent = Math.min(80, 25 + (attempt * 3));
                if (progress > 0) displayPercent = Math.min(80, 25 + (progress * 55 / 100));
                listener.onProgress(displayPercent, "Generating: " + status +
                        (progress > 0 ? " (" + progress + "%)" : ""));

                if ("Completed".equalsIgnoreCase(status)) return;
                if ("Failed".equalsIgnoreCase(status) || "Timed Out".equalsIgnoreCase(status)) {
                    throw new Exception("Avatar generation failed on server");
                }
            }

            // Timeout after 5 minutes
            if (System.currentTimeMillis() - startTime > 5 * 60 * 1000) {
                throw new Exception("Timeout waiting for avatar");
            }
        }
    }

    // ============== STEP 4: Download GLB ==============

    private File downloadGlb(String token, String code) throws Exception {
        // List exports
        Request listReq = new Request.Builder()
                .url(API_BASE + "/avatars/" + code + "/exports/")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        String exportsBody;
        try (Response resp = httpClient.newCall(listReq).execute()) {
            exportsBody = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                throw new Exception("List exports failed (" + resp.code() + "): " + truncate(exportsBody, 300));
            }
        }

        Log.d(TAG, "Exports response: " + truncate(exportsBody, 500));

        String downloadUrl = findGlbUrl(exportsBody);
        if (downloadUrl == null) {
            // No GLB export available — try to request one
            downloadUrl = requestGlbExport(token, code);
        }

        if (downloadUrl == null) {
            throw new Exception("No GLB export available for this avatar");
        }

        // Download the file
        Request dlReq = new Request.Builder()
                .url(downloadUrl)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response resp = httpClient.newCall(dlReq).execute()) {
            if (!resp.isSuccessful()) {
                throw new Exception("Download failed: " + resp.code());
            }

            File outFile = new File(context.getFilesDir(), "twin_" + code + ".glb");
            try (FileOutputStream fos = new FileOutputStream(outFile);
                 InputStream is = resp.body().byteStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
            }
            return outFile;
        }
    }

    /** Search response JSON (could be array or paginated object) for a GLB file URL. */
    private String findGlbUrl(String exportsBody) {
        try {
            // Try as array first
            JSONArray exports;
            try {
                exports = new JSONArray(exportsBody);
            } catch (Exception e) {
                JSONObject paginated = new JSONObject(exportsBody);
                exports = paginated.optJSONArray("results");
                if (exports == null) exports = paginated.optJSONArray("exports");
                if (exports == null) return null;
            }

            for (int i = 0; i < exports.length(); i++) {
                JSONObject export = exports.getJSONObject(i);
                String format = export.optString("format", "").toLowerCase();
                // Match glb / gltf format
                if (format.contains("glb") || format.contains("gltf")) {
                    // Find file URL
                    JSONArray files = export.optJSONArray("files");
                    if (files != null && files.length() > 0) {
                        return files.getJSONObject(0).getString("url");
                    }
                    // Sometimes the URL is directly on the export
                    String directUrl = export.optString("url", null);
                    if (directUrl != null && !directUrl.isEmpty()) return directUrl;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse exports: " + e.getMessage());
        }
        return null;
    }

    /** If no GLB export exists yet, request one and poll until ready. */
    private String requestGlbExport(String token, String code) throws Exception {
        FormBody form = new FormBody.Builder()
                .add("format", "glb")
                .add("lod", "1")
                .add("texture_profile", "1K.jpg")
                .add("blendshapes", "visemes")
                .add("blendshapes", "head_2.0")
                .build();
        Request req = new Request.Builder()
                .url(API_BASE + "/avatars/" + code + "/exports/")
                .post(form)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                Log.w(TAG, "Export request failed (" + resp.code() + "): " + body);
                return null;
            }
            // Poll the new export
            JSONObject export = new JSONObject(body);
            String exportId = export.optString("code", export.optString("id", null));
            if (exportId == null) return null;

            for (int i = 0; i < 30; i++) {
                Thread.sleep(3000);
                Request statusReq = new Request.Builder()
                        .url(API_BASE + "/avatars/" + code + "/exports/" + exportId + "/")
                        .get()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                try (Response statusResp = httpClient.newCall(statusReq).execute()) {
                    String sBody = statusResp.body().string();
                    JSONObject obj = new JSONObject(sBody);
                    String status = obj.optString("status", "");
                    if ("Completed".equalsIgnoreCase(status)) {
                        JSONArray files = obj.optJSONArray("files");
                        if (files != null && files.length() > 0) {
                            return files.getJSONObject(0).getString("url");
                        }
                    }
                    if ("Failed".equalsIgnoreCase(status)) return null;
                }
            }
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
