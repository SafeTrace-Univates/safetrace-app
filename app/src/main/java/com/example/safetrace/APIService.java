package com.example.safetrace;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.HashMap;
import java.util.Map;

public class APIService {
    private static APIService instance;
    private RequestQueue requestQueue;


    private APIService(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized APIService getInstance(Context context) {
        if (instance == null) {
            instance = new APIService(context);
        }
        return instance;
    }

    public void login(Context context, String email, String password,
                      final APIServiceCallback callback) {
        String url = "http://10.0.2.2:8000/api/v1/auth/login";
        JSONObject params = new JSONObject();
        try {
            params.put("login", email);
            params.put("password", password);
        } catch (JSONException e) {
            callback.onError("JSON error");
            return;
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, url, params,
                response -> {
                    String token = response.optString("token", null);
                    if (token != null) {
                        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("api_token", token).apply();
                    }
                    callback.onSuccess(response);
                },
                error -> {
                    String message = "Login failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("error", message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (error.getMessage() != null) {
                        message = error.getMessage();
                    }
                    callback.onError(message);
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    public void register(Context context, String name, String email, String phone, String document, String password,
                         final APIServiceCallback callback) {
        String url = "http://10.0.2.2:8000/api/v1/auth/register";
        JSONObject params = new JSONObject();
        try {
            params.put("name", name);
            params.put("email", email);
            params.put("phone", phone);
            params.put("document", document);
            params.put("password", password);
        } catch (JSONException e) {
            callback.onError("JSON error");
            return;
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, url, params,
                response -> {
                    String token = response.optString("token", null);
                    if (token != null) {
                        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
                        prefs.edit().putString("api_token", token).apply();
                    }
                    callback.onSuccess(response);
                },
                error -> {
                    String message = "Register failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("error", message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (error.getMessage() != null) {
                        message = error.getMessage();
                    }
                    callback.onError(message);
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    public void logout(Context context, final APIServiceCallback callback) {
        String url = "http://10.0.2.2:8000/api/v1/auth/logout";

        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", null);

        if (token == null || token.isEmpty()) {
            callback.onError("No token found. User may not be logged in.");
            return;
        }

        JSONObject params = new JSONObject();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, url, params,
                response -> {
                    callback.onSuccess(response);
                },
                error -> {
                    String message = "Logout failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("error", message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (error.getMessage() != null) {
                        message = error.getMessage();
                    }
                    callback.onError(message);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public void getContacts(Context context, boolean includeUser, final APIServiceCallback callback) {
        String url = "http://10.0.2.2:8000/api/v1/contact";
        if (includeUser) {
            url += "?include=user";
        }

        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", null);

        if (token == null || token.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    callback.onSuccess(response);
                },
                error -> {
                    String message = "Failed to fetch contacts";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("error", message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    callback.onError(message);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }


    public interface APIServiceCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}