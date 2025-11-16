package com.example.safetrace.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia a fila de dados pendentes de envio à API
 */
public class PendingDataQueue {
    private static final String TAG = "PendingDataQueue";
    private static final String PREFS_NAME = "safetrace_prefs";
    private static final String KEY_PENDING_ALERTS = "pending_alerts";
    private static final String KEY_PENDING_LOCATIONS = "pending_locations";
    private static final String KEY_PENDING_RECORDINGS = "pending_recordings";
    
    private Context context;
    
    public PendingDataQueue(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Adiciona um alert pendente à fila
     */
    public void addPendingAlert(String alertId, String userId) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_ALERTS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            JSONObject alertData = new JSONObject();
            alertData.put("alert_id", alertId);
            alertData.put("ref_user", userId);
            alertData.put("created_at", System.currentTimeMillis());
            
            pendingArray.put(alertData);
            
            prefs.edit().putString(KEY_PENDING_ALERTS, pendingArray.toString()).apply();
            Log.d(TAG, "Alert adicionado à fila pendente: " + alertId);
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao adicionar alert pendente", e);
        }
    }
    
    /**
     * Adiciona uma localização pendente à fila
     */
    public void addPendingLocation(String alertId, String latitude, String longitude) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_LOCATIONS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            JSONObject locationData = new JSONObject();
            locationData.put("ref_alert", alertId);
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("created_at", System.currentTimeMillis());
            
            pendingArray.put(locationData);
            
            prefs.edit().putString(KEY_PENDING_LOCATIONS, pendingArray.toString()).apply();
            Log.d(TAG, "Localização adicionada à fila pendente para alert: " + alertId);
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao adicionar localização pendente", e);
        }
    }
    
    /**
     * Adiciona uma gravação pendente à fila
     */
    public void addPendingRecording(String alertId, String filePath) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_RECORDINGS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            JSONObject recordingData = new JSONObject();
            recordingData.put("ref_alert", alertId);
            recordingData.put("file_path", filePath);
            recordingData.put("created_at", System.currentTimeMillis());
            
            pendingArray.put(recordingData);
            
            prefs.edit().putString(KEY_PENDING_RECORDINGS, pendingArray.toString()).apply();
            Log.d(TAG, "Gravação adicionada à fila pendente para alert: " + alertId);
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao adicionar gravação pendente", e);
        }
    }
    
    /**
     * Obtém todos os alerts pendentes
     */
    public List<JSONObject> getPendingAlerts() {
        List<JSONObject> alerts = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_ALERTS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            for (int i = 0; i < pendingArray.length(); i++) {
                alerts.add(pendingArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao obter alerts pendentes", e);
        }
        return alerts;
    }
    
    /**
     * Obtém todas as localizações pendentes
     */
    public List<JSONObject> getPendingLocations() {
        List<JSONObject> locations = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_LOCATIONS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            for (int i = 0; i < pendingArray.length(); i++) {
                locations.add(pendingArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao obter localizações pendentes", e);
        }
        return locations;
    }
    
    /**
     * Obtém todas as gravações pendentes
     */
    public List<JSONObject> getPendingRecordings() {
        List<JSONObject> recordings = new ArrayList<>();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_RECORDINGS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            for (int i = 0; i < pendingArray.length(); i++) {
                recordings.add(pendingArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao obter gravações pendentes", e);
        }
        return recordings;
    }
    
    /**
     * Remove um alert da fila pendente após envio bem-sucedido
     */
    public void removePendingAlert(JSONObject alertData) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_ALERTS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < pendingArray.length(); i++) {
                JSONObject item = pendingArray.getJSONObject(i);
                if (!item.getString("alert_id").equals(alertData.optString("alert_id"))) {
                    newArray.put(item);
                }
            }
            
            prefs.edit().putString(KEY_PENDING_ALERTS, newArray.toString()).apply();
            Log.d(TAG, "Alert removido da fila pendente");
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao remover alert pendente", e);
        }
    }
    
    /**
     * Remove uma localização da fila pendente após envio bem-sucedido
     */
    public void removePendingLocation(JSONObject locationData) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_LOCATIONS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            JSONArray newArray = new JSONArray();
            String targetRefAlert = locationData.optString("ref_alert");
            String targetLat = locationData.optString("latitude");
            String targetLng = locationData.optString("longitude");
            
            for (int i = 0; i < pendingArray.length(); i++) {
                JSONObject item = pendingArray.getJSONObject(i);
                if (!(item.optString("ref_alert").equals(targetRefAlert) &&
                      item.optString("latitude").equals(targetLat) &&
                      item.optString("longitude").equals(targetLng))) {
                    newArray.put(item);
                }
            }
            
            prefs.edit().putString(KEY_PENDING_LOCATIONS, newArray.toString()).apply();
            Log.d(TAG, "Localização removida da fila pendente");
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao remover localização pendente", e);
        }
    }
    
    /**
     * Remove uma gravação da fila pendente após envio bem-sucedido
     */
    public void removePendingRecording(JSONObject recordingData) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String pendingJson = prefs.getString(KEY_PENDING_RECORDINGS, "[]");
            JSONArray pendingArray = new JSONArray(pendingJson);
            
            JSONArray newArray = new JSONArray();
            String targetRefAlert = recordingData.optString("ref_alert");
            String targetFilePath = recordingData.optString("file_path");
            
            for (int i = 0; i < pendingArray.length(); i++) {
                JSONObject item = pendingArray.getJSONObject(i);
                if (!(item.optString("ref_alert").equals(targetRefAlert) &&
                      item.optString("file_path").equals(targetFilePath))) {
                    newArray.put(item);
                }
            }
            
            prefs.edit().putString(KEY_PENDING_RECORDINGS, newArray.toString()).apply();
            Log.d(TAG, "Gravação removida da fila pendente");
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao remover gravação pendente", e);
        }
    }
    
    /**
     * Salva localizações pendentes atualizadas de volta na fila
     */
    public void savePendingLocations(List<JSONObject> locations) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONArray locationsArray = new JSONArray();
            for (JSONObject location : locations) {
                locationsArray.put(location);
            }
            prefs.edit().putString(KEY_PENDING_LOCATIONS, locationsArray.toString()).apply();
            Log.d(TAG, "Localizações pendentes atualizadas salvas");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar localizações pendentes atualizadas", e);
        }
    }
    
    /**
     * Salva gravações pendentes atualizadas de volta na fila
     */
    public void savePendingRecordings(List<JSONObject> recordings) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            JSONArray recordingsArray = new JSONArray();
            for (JSONObject recording : recordings) {
                recordingsArray.put(recording);
            }
            prefs.edit().putString(KEY_PENDING_RECORDINGS, recordingsArray.toString()).apply();
            Log.d(TAG, "Gravações pendentes atualizadas salvas");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar gravações pendentes atualizadas", e);
        }
    }
    
    /**
     * Limpa todas as filas pendentes (útil para testes ou reset)
     */
    public void clearAllPending() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_PENDING_ALERTS, "[]")
            .putString(KEY_PENDING_LOCATIONS, "[]")
            .putString(KEY_PENDING_RECORDINGS, "[]")
            .apply();
        Log.d(TAG, "Todas as filas pendentes foram limpas");
    }
}

