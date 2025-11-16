package com.example.safetrace.service;

import android.content.Context;
import android.util.Log;

import com.example.safetrace.APIService;
import com.example.safetrace.util.NetworkUtils;
import com.example.safetrace.util.PendingDataQueue;

import org.json.JSONObject;

import java.util.List;

/**
 * Serviço para sincronizar dados pendentes com a API quando houver conexão
 */
public class SyncService {
    private static final String TAG = "SyncService";
    private static SyncService instance;
    private Context context;
    private PendingDataQueue pendingQueue;
    
    private SyncService(Context context) {
        this.context = context.getApplicationContext();
        this.pendingQueue = new PendingDataQueue(this.context);
    }
    
    public static synchronized SyncService getInstance(Context context) {
        if (instance == null) {
            instance = new SyncService(context);
        }
        return instance;
    }
    
    /**
     * Processa a fila de dados pendentes e tenta enviar à API
     */
    public void processPendingQueue() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "Sem conexão com internet. Dados permanecerão na fila pendente.");
            return;
        }
        
        Log.d(TAG, "Processando fila de dados pendentes...");
        
        // Processar alerts pendentes
        processPendingAlerts();
        
        // Processar localizações pendentes
        processPendingLocations();
        
        // Processar gravações pendentes
        processPendingRecordings();
    }
    
    private void processPendingAlerts() {
        List<JSONObject> pendingAlerts = pendingQueue.getPendingAlerts();
        Log.d(TAG, "Processando " + pendingAlerts.size() + " alerts pendentes");
        
        for (JSONObject alertData : pendingAlerts) {
            try {
                String alertId = alertData.optString("alert_id");
                String userId = alertData.optString("ref_user");
                
                if (alertId == null || alertId.isEmpty()) {
                    // Se não tem alert_id, precisa criar o alert na API
                    if (userId != null && !userId.isEmpty()) {
                        APIService.getInstance(context).createAlert(
                            context,
                            userId,
                            null,
                            new java.util.ArrayList<String>(),
                            new APIService.APIServiceCallback() {
                                @Override
                                public void onSuccess(org.json.JSONObject response) {
                                    try {
                                        String newAlertId = null;
                                        if (response.has("data")) {
                                            org.json.JSONObject data = response.getJSONObject("data");
                                            newAlertId = data.optString("id", null);
                                        } else if (response.has("id")) {
                                            newAlertId = response.optString("id", null);
                                        }
                                        
                                        if (newAlertId != null) {
                                            Log.d(TAG, "Alert criado na API: " + newAlertId);
                                            // Salvar alertId no SharedPreferences
                                            android.content.SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
                                            prefs.edit().putString("current_alert_id", newAlertId).apply();
                                            
                                            // Atualizar localizações e gravações pendentes com o novo alertId
                                            updatePendingDataWithAlertId(newAlertId);
                                            
                                            // Remover da fila
                                            pendingQueue.removePendingAlert(alertData);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Erro ao processar resposta do alert", e);
                                    }
                                }
                                
                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Erro ao criar alert na API: " + error);
                                    // Manter na fila para tentar novamente depois
                                }
                            }
                        );
                    }
                } else {
                    // Alert já existe, apenas remover da fila
                    pendingQueue.removePendingAlert(alertData);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao processar alert pendente", e);
            }
        }
    }
    
    /**
     * Atualiza localizações e gravações pendentes com o alertId correto
     */
    private void updatePendingDataWithAlertId(String alertId) {
        boolean updated = false;
        
        // Atualizar localizações pendentes que têm ref_alert vazio
        List<JSONObject> pendingLocations = pendingQueue.getPendingLocations();
        for (JSONObject location : pendingLocations) {
            String refAlert = location.optString("ref_alert", "");
            if (refAlert == null || refAlert.isEmpty()) {
                try {
                    location.put("ref_alert", alertId);
                    updated = true;
                    Log.d(TAG, "Localização pendente atualizada com alertId: " + alertId);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Erro ao atualizar localização pendente", e);
                }
            }
        }
        
        if (updated) {
            // Salvar localizações atualizadas de volta na fila
            pendingQueue.savePendingLocations(pendingLocations);
        }
        
        updated = false;
        
        // Atualizar gravações pendentes que têm ref_alert vazio
        List<JSONObject> pendingRecordings = pendingQueue.getPendingRecordings();
        for (JSONObject recording : pendingRecordings) {
            String refAlert = recording.optString("ref_alert", "");
            if (refAlert == null || refAlert.isEmpty()) {
                try {
                    recording.put("ref_alert", alertId);
                    updated = true;
                    Log.d(TAG, "Gravação pendente atualizada com alertId: " + alertId);
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Erro ao atualizar gravação pendente", e);
                }
            }
        }
        
        if (updated) {
            // Salvar gravações atualizadas de volta na fila
            pendingQueue.savePendingRecordings(pendingRecordings);
        }
        
        Log.d(TAG, "AlertId criado: " + alertId + ". Localizações e gravações pendentes atualizadas e processadas.");
        
        // Processar novamente as filas para enviar os dados atualizados
        processPendingLocations();
        processPendingRecordings();
    }
    
    private void processPendingLocations() {
        List<JSONObject> pendingLocations = pendingQueue.getPendingLocations();
        Log.d(TAG, "Processando " + pendingLocations.size() + " localizações pendentes");
        
        // Verificar se há alertId disponível no SharedPreferences
        android.content.SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String currentAlertId = prefs.getString("current_alert_id", null);
        
        for (JSONObject locationData : pendingLocations) {
            try {
                String alertIdTemp = locationData.optString("ref_alert");
                
                // Se não tem alertId, usar o do SharedPreferences
                if (alertIdTemp == null || alertIdTemp.isEmpty()) {
                    alertIdTemp = currentAlertId;
                }
                
                final String alertId = alertIdTemp; // Variável final para usar no callback
                String latitude = locationData.optString("latitude");
                String longitude = locationData.optString("longitude");
                
                if (alertId != null && !alertId.isEmpty() && 
                    latitude != null && !latitude.isEmpty() &&
                    longitude != null && !longitude.isEmpty()) {
                    
                    APIService.getInstance(context).addLocation(
                        context,
                        alertId,
                        Double.parseDouble(latitude),
                        Double.parseDouble(longitude),
                        new APIService.APIServiceCallback() {
                            @Override
                            public void onSuccess(org.json.JSONObject response) {
                                Log.d(TAG, "Localização enviada com sucesso para alert: " + alertId);
                                pendingQueue.removePendingLocation(locationData);
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Erro ao enviar localização: " + error);
                                // Manter na fila para tentar novamente depois
                            }
                        }
                    );
                } else {
                    Log.d(TAG, "Localização pendente aguardando alertId. AlertId: " + alertId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao processar localização pendente", e);
            }
        }
    }
    
    private void processPendingRecordings() {
        List<JSONObject> pendingRecordings = pendingQueue.getPendingRecordings();
        Log.d(TAG, "Processando " + pendingRecordings.size() + " gravações pendentes");
        
        // Verificar se há alertId disponível no SharedPreferences
        android.content.SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String currentAlertId = prefs.getString("current_alert_id", null);
        
        for (JSONObject recordingData : pendingRecordings) {
            try {
                String alertIdTemp = recordingData.optString("ref_alert");
                
                // Se não tem alertId, usar o do SharedPreferences
                if (alertIdTemp == null || alertIdTemp.isEmpty()) {
                    alertIdTemp = currentAlertId;
                }
                
                final String alertId = alertIdTemp; // Variável final para usar no callback
                String filePath = recordingData.optString("file_path");
                
                if (alertId != null && !alertId.isEmpty() && 
                    filePath != null && !filePath.isEmpty()) {
                    
                    // Duração não está no ER model, mas o método ainda requer
                    APIService.getInstance(context).addRecording(
                        context,
                        alertId,
                        filePath,
                        0, // duration não está no ER model
                        new APIService.APIServiceCallback() {
                            @Override
                            public void onSuccess(org.json.JSONObject response) {
                                Log.d(TAG, "Gravação enviada com sucesso para alert: " + alertId);
                                pendingQueue.removePendingRecording(recordingData);
                            }
                            
                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Erro ao enviar gravação: " + error);
                                // Manter na fila para tentar novamente depois
                            }
                        }
                    );
                } else {
                    Log.d(TAG, "Gravação pendente aguardando alertId. AlertId: " + alertId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao processar gravação pendente", e);
            }
        }
    }
    
    /**
     * Adiciona dados à fila pendente quando não houver conexão
     */
    public void addToPendingQueue(String alertId, String userId, String latitude, String longitude, String filePath) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "Sem conexão. Adicionando dados à fila pendente...");
            
            // Adicionar alert se necessário
            if (alertId == null || alertId.isEmpty()) {
                pendingQueue.addPendingAlert(null, userId);
            }
            
            // Adicionar localização se fornecida
            if (latitude != null && longitude != null && !latitude.isEmpty() && !longitude.isEmpty()) {
                String finalAlertId = alertId != null ? alertId : "";
                pendingQueue.addPendingLocation(finalAlertId, latitude, longitude);
            }
            
            // Adicionar gravação se fornecida
            if (filePath != null && !filePath.isEmpty()) {
                String finalAlertId = alertId != null ? alertId : "";
                pendingQueue.addPendingRecording(finalAlertId, filePath);
            }
        }
    }
}

