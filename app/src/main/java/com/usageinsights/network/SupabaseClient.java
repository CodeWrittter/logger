package com.usageinsights.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.usageinsights.R;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SupabaseClient {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override public boolean shouldSkipField(FieldAttributes f) {
                    return f.getName().equals("id") || f.getName().equals("synced");
                }
                @Override public boolean shouldSkipClass(Class<?> c) { return false; }
            })
            .create();

    private final String baseUrl;
    private final String anonKey;

    public SupabaseClient(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("usageinsights", Context.MODE_PRIVATE);
        String savedUrl = prefs.getString("supabase_url", "");
        String savedKey = prefs.getString("supabase_anon_key", "");
        String url = savedUrl.isEmpty() ? context.getString(R.string.supabase_url) : savedUrl;
        this.baseUrl = url.replaceAll("/+$", "");
        this.anonKey = savedKey.isEmpty() ? context.getString(R.string.supabase_anon_key) : savedKey;
    }

    public SupabaseClient(String baseUrl, String anonKey) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.anonKey = anonKey;
    }

    public void insert(String table, List<?> rows) throws IOException {
        if (rows.isEmpty()) return;
        String json = gson.toJson(rows);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/" + table)
                .post(body)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + anonKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                ResponseBody rb = response.body();
                String detail = rb != null ? rb.string() : "(no body)";
                throw new IOException(table + " HTTP " + response.code() + ": " + detail);
            }
        }
    }

    // Returns the HTTP status code, or 0 on network/IO failure.
    public int ping() {
        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/error_logs?select=id&limit=0")
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + anonKey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.code();
        } catch (IOException e) {
            return 0;
        }
    }

    // Returns total row count for a table via Supabase's count=exact, or -1 on failure.
    public long getRemoteCount(String table) {
        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/" + table + "?select=*")
                .head()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + anonKey)
                .addHeader("Prefer", "count=exact")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return -1;
            String range = response.header("Content-Range");
            if (range == null) return -1;
            // format: 0-24/12492
            int slash = range.indexOf('/');
            if (slash < 0) return -1;
            return Long.parseLong(range.substring(slash + 1));
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }
}
