package com.example.safetrace;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class APIService {
    private static APIService instance;
    private static String BASE_API_URL;
    private RequestQueue requestQueue;



    private APIService(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        //BASE_API_URL = BuildConfig.BASE_API_URL;
        BASE_API_URL = "http://10.0.2.2:8000/api/v1/";
    }

    public static synchronized APIService getInstance(Context context) {
        if (instance == null) {
            instance = new APIService(context);
        }
        return instance;
    }

    public void login(Context context, String email, String password,
                      final APIServiceCallback callback) {
        String url = BASE_API_URL + "auth/login";
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
                    SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
                    if (token != null) {
                        prefs.edit().putString("api_token", token).apply();
                    }
                    
                    // Tentar salvar nome do usuário se estiver na resposta do login
                    try {
                        String userName = null;
                        if (response.has("user")) {
                            JSONObject user = response.getJSONObject("user");
                            userName = user.optString("name", null);
                        } else if (response.has("name")) {
                            userName = response.optString("name", null);
                        }
                        
                        if (userName != null && !userName.isEmpty()) {
                            prefs.edit().putString("user_name", userName).apply();
                            android.util.Log.d("APIService", "Nome salvo da resposta de login: " + userName);
                        }
                    } catch (JSONException e) {
                        // Ignorar se não tiver nome na resposta
                    }
                    
                    callback.onSuccess(response);
                },
                error -> {
                    String message = "Erro ao fazer login. Verifique suas credenciais.";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            
                            // Tentar vários campos possíveis de erro
                            if (data.has("error")) {
                                message = data.optString("error", message);
                            } else if (data.has("message")) {
                                message = data.optString("message", message);
                            } else if (data.has("errors")) {
                                // Laravel pode retornar erros em formato de objeto
                                try {
                                    JSONObject errors = data.optJSONObject("errors");
                                    if (errors != null && errors.length() > 0) {
                                        // Pegar o primeiro erro disponível
                                        Iterator<String> keys = errors.keys();
                                        if (keys.hasNext()) {
                                            String firstKey = keys.next();
                                            Object errorObj = errors.get(firstKey);
                                            if (errorObj instanceof String) {
                                                message = (String) errorObj;
                                            } else if (errorObj instanceof JSONArray) {
                                                JSONArray errorArray = (JSONArray) errorObj;
                                                if (errorArray.length() > 0) {
                                                    message = errorArray.getString(0);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // Se falhar, usar mensagem padrão
                                }
                            }
                            
                            // Traduzir mensagens comuns
                            message = translateErrorMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (error.getMessage() != null) {
                        message = translateErrorMessage(error.getMessage());
                    }
                    callback.onError(message);
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    public void register(Context context, String name, String email, String phone, String document, String password,
                         final APIServiceCallback callback) {
        String url = BASE_API_URL + "auth/register";
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
                    SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
                    if (token != null) {
                        prefs.edit().putString("api_token", token).apply();
                    }
                    
                    // Tentar salvar nome do usuário se estiver na resposta
                    try {
                        String userName = null;
                        if (response.has("name")) {
                            userName = response.optString("name", null);
                        } else if (response.has("user")) {
                            JSONObject user = response.getJSONObject("user");
                            userName = user.optString("name", null);
                        } else if (response.has("data")) {
                            JSONObject data = response.getJSONObject("data");
                            userName = data.optString("name", null);
                        }
                        
                        // Se não encontrou na resposta, usar o nome passado como parâmetro
                        if ((userName == null || userName.isEmpty()) && name != null && !name.isEmpty()) {
                            userName = name;
                        }
                        
                        if (userName != null && !userName.isEmpty()) {
                            prefs.edit().putString("user_name", userName).apply();
                        }
                    } catch (JSONException e) {
                        // Se não tiver nome na resposta, usar o nome passado como parâmetro
                        if (name != null && !name.isEmpty()) {
                            prefs.edit().putString("user_name", name).apply();
                        }
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
        String url = BASE_API_URL + "auth/logout";

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
        String url = BASE_API_URL + "contact";
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

    public void getUserProfile(Context context, final APIServiceCallback callback) {
        String url = BASE_API_URL + "auth/user";

        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", null);

        if (token == null || token.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        // Extract user data directly from the response
                        String userId = response.has("id") ? String.valueOf(response.optInt("id")) : null;
                        String userName = response.optString("name", null);

                        SharedPreferences prefs2 = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs2.edit();

                        if (userId != null && !userId.isEmpty()) {
                            editor.putString("user_id", userId);
                            android.util.Log.d("APIService", "User ID saved: " + userId);
                        }
                        if (userName != null && !userName.isEmpty()) {
                            editor.putString("user_name", userName);
                            android.util.Log.d("APIService", "User name saved: " + userName);
                        } else {
                            android.util.Log.w("APIService", "User name not found in response");
                        }
                        editor.apply();
                    } catch (Exception e) {
                        android.util.Log.e("APIService", "Error parsing user profile response", e);
                        // Ignore parsing error
                    }
                    callback.onSuccess(response);
                },
                error -> {
                    String message = "Failed to fetch user profile";
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

    public void addContact(Context context, String code, final APIServiceCallback callback) {
        String url = BASE_API_URL + "contact";
        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", null);
        String userId = prefs.getString("user_id", null);

        if (token == null || token.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }
        if (userId == null || userId.isEmpty()) {
            callback.onError("User ID not available. Please re-login.");
            return;
        }

        JSONObject params = new JSONObject();
        try {
            params.put("ref_user", code);
            params.put("ref_owner", userId);
        } catch (JSONException e) {
            callback.onError("JSON error");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, url, params,
                response -> {
                    callback.onSuccess(response);
                },
                error -> {
                    String message = "Failed to add contact";
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
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public void createAlert(Context context, String userId, String name, List<String> contactIds, final APIServiceCallback callback) {
        String url = BASE_API_URL + "alert";
        JSONObject params = new JSONObject();
        try {
            System.out.println(userId);
            params.put("ref_user", Integer.parseInt(userId));  // parse userId to number if needed
            if (name != null && !name.isEmpty()) {
                params.put("name", name);
            }
            JSONArray contactsArray = new JSONArray();
            for (String contactId : contactIds) {
                try {
                    contactsArray.put(Integer.parseInt(contactId));  // parse each contact to integer
                } catch (NumberFormatException e) {
                    // Log invalid format, skip or handle as needed
                }
            }
            params.put("contacts", contactsArray);

        } catch (JSONException e) {
            callback.onError("JSON error building alert creation payload");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", null);

        if (token == null || token.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> callback.onSuccess(response) ,
                error -> {
                    String message = "Failed to create alert";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject data = new JSONObject(responseBody);
                            message = data.optString("error", message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    System.out.println(message);
                    callback.onError(message);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public void addLocation(Context context, String alertId, double latitude, double longitude, final APIServiceCallback callback) {
        String url = BASE_API_URL + "location";
        JSONObject params = new JSONObject();
        try {
            params.put("ref_alert", alertId);
            params.put("latitude", latitude);
            params.put("longitude", longitude);
        } catch (JSONException e) {
            callback.onError("JSON error building location payload");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", null);

        if (token == null || token.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> callback.onSuccess(response),
                error -> {
                    String message = "Failed to add location";
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
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public void addRecording(Context context, String alertId, String filePath, int duration, final APIServiceCallback callback) {
        String url = BASE_API_URL + "recording";
        JSONObject params = new JSONObject();
        try {
            params.put("ref_alert", alertId);
            params.put("file_path", filePath);
            params.put("duration", duration);
        } catch (JSONException e) {
            callback.onError("JSON error building recording payload");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("api_token", null);

        if (token == null || token.isEmpty()) {
            callback.onError("User not authenticated");
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> callback.onSuccess(response),
                error -> {
                    String message = "Failed to add recording";
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
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    private String translateErrorMessage(String error) {
        if (error == null) {
            return "Erro desconhecido";
        }
        
        String lowerError = error.toLowerCase();
        
        // Traduzir mensagens comuns da API
        if (lowerError.contains("no query results") || lowerError.contains("user not found")) {
            return "Email ou senha incorretos";
        }
        if (lowerError.contains("unauthorized") || lowerError.contains("invalid credentials")) {
            return "Email ou senha incorretos";
        }
        if (lowerError.contains("password")) {
            return "Senha incorreta";
        }
        if (lowerError.contains("email") || lowerError.contains("login")) {
            return "Email não encontrado";
        }
        if (lowerError.contains("network") || lowerError.contains("connection")) {
            return "Erro de conexão. Verifique sua internet.";
        }
        
        return error;
    }

    public interface APIServiceCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}