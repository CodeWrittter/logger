package com.phonelogger.network;

import android.content.Context;

import com.google.gson.Gson;
import com.phonelogger.R;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClient {

    private static final MediaType JSON = MediaType.parse("application/json");
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private final String baseUrl;
    private final String anonKey;

    public SupabaseClient(Context context) {
        this.baseUrl = context.getString(R.string.supabase_url);
        this.anonKey = context.getString(R.string.supabase_anon_key);
    }

    public boolean insert(String table, List<?> rows) throws IOException {
        if (rows.isEmpty()) return true;
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
            return response.isSuccessful();
        }
    }

    public boolean testConnection() {
        Request request = new Request.Builder()
                .url(baseUrl + "/rest/v1/")
                .get()
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + anonKey)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }
}
