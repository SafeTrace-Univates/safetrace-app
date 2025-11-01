package com.example.safetrace.service;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safetrace.model.Emergencia;
import com.example.safetrace.model.Localizacao;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EmergenciaService {
    private static final String TAG = "EmergenciaService";
    private static EmergenciaService instance;
    private Emergencia emergenciaAtual;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private Context context;
    
    private EmergenciaService(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized EmergenciaService getInstance(Context context) {
        if (instance == null) {
            instance = new EmergenciaService(context);
        }
        return instance;
    }
    
    public void iniciarEmergencia(String usuarioId, String usuarioNome, List<String> contatosIds, List<String> contatosNomes) {
        try {
            Log.d(TAG, "iniciarEmergencia chamado");
            
            if (emergenciaAtual != null && emergenciaAtual.isEmAndamento()) {
                Log.w(TAG, "Já existe uma emergência em andamento");
                return;
            }
            
            emergenciaAtual = new Emergencia();
            emergenciaAtual.setId(UUID.randomUUID().toString());
            emergenciaAtual.setDataInicio(new Date());
            emergenciaAtual.setUsuarioId(usuarioId);
            emergenciaAtual.setUsuarioNome(usuarioNome);
            emergenciaAtual.setNotificadosIds(contatosIds);
            emergenciaAtual.setNotificadosNomes(contatosNomes);
            emergenciaAtual.setEmAndamento(true);
            
            Log.d(TAG, "Emergencia criada: " + emergenciaAtual.getId());
            
            // Iniciar gravação de áudio
            try {
                iniciarGravacaoAudio();
                Log.d(TAG, "Gravação de áudio iniciada");
            } catch (Exception e) {
                Log.e(TAG, "Erro ao iniciar gravação de áudio", e);
                // Continuar mesmo sem áudio
            }
            
            Log.d(TAG, "Emergência iniciada: " + emergenciaAtual.getId());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar emergência", e);
            e.printStackTrace();
            throw new RuntimeException("Erro ao iniciar emergência", e);
        }
    }
    
    public void finalizarEmergencia() {
        if (emergenciaAtual == null || !emergenciaAtual.isEmAndamento()) {
            Log.w(TAG, "Nenhuma emergência em andamento");
            return;
        }
        
        emergenciaAtual.setDataFim(new Date());
        emergenciaAtual.setEmAndamento(false);
        
        // Parar gravação de áudio
        pararGravacaoAudio();
        
        // Salvar emergência
        salvarEmergencia();
        
        Log.d(TAG, "Emergência finalizada: " + emergenciaAtual.getId());
        
        emergenciaAtual = null;
    }
    
    public void adicionarLocalizacao(double latitude, double longitude, float precisao) {
        if (emergenciaAtual != null && emergenciaAtual.isEmAndamento()) {
            Localizacao localizacao = new Localizacao(latitude, longitude, precisao);
            emergenciaAtual.adicionarLocalizacao(localizacao);
            Log.d(TAG, "Localização adicionada: " + latitude + ", " + longitude);
        }
    }
    
    public Emergencia getEmergenciaAtual() {
        return emergenciaAtual;
    }
    
    public boolean isEmergenciaEmAndamento() {
        return emergenciaAtual != null && emergenciaAtual.isEmAndamento();
    }
    
    private void iniciarGravacaoAudio() {
        if (isRecording) {
            Log.w(TAG, "Gravação já está em andamento");
            return;
        }
        
        try {
            // Verificar permissão
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permissão de áudio não concedida");
                return;
            }
            
            Log.d(TAG, "Criando diretório de áudios...");
            // Criar diretório para áudios se não existir
            File audioDir = new File(context.getFilesDir(), "audios");
            if (!audioDir.exists()) {
                boolean created = audioDir.mkdirs();
                Log.d(TAG, "Diretório criado: " + created);
            }
            
            // Criar arquivo de áudio com timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String audioFileName = "emergencia_" + timestamp + ".m4a";
            File audioFile = new File(audioDir, audioFileName);
            
            Log.d(TAG, "Arquivo de áudio: " + audioFile.getAbsolutePath());
            
            if (emergenciaAtual != null) {
                emergenciaAtual.setCaminhoAudio(audioFile.getAbsolutePath());
            }
            
            Log.d(TAG, "Inicializando MediaRecorder...");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            
            Log.d(TAG, "Preparando MediaRecorder...");
            mediaRecorder.prepare();
            
            Log.d(TAG, "Iniciando gravação...");
            mediaRecorder.start();
            isRecording = true;
            
            Log.d(TAG, "Gravação de áudio iniciada: " + audioFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar gravação de áudio", e);
            e.printStackTrace();
            isRecording = false;
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.release();
                } catch (Exception ex) {
                    Log.e(TAG, "Erro ao liberar MediaRecorder", ex);
                }
                mediaRecorder = null;
            }
            // Não lançar exceção, apenas logar o erro e continuar sem áudio
            Log.e(TAG, "Continuando sem gravação de áudio devido a erro");
        }
    }
    
    private void pararGravacaoAudio() {
        if (!isRecording || mediaRecorder == null) {
            return;
        }
        
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            Log.d(TAG, "Gravação de áudio finalizada");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parar gravação de áudio", e);
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
            isRecording = false;
        }
    }
    
    private void salvarEmergencia() {
        if (emergenciaAtual == null) {
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String emergenciasJson = prefs.getString("emergencias", "[]");
        
        try {
            org.json.JSONArray emergenciasArray = new org.json.JSONArray(emergenciasJson);
            org.json.JSONObject emergenciaJson = new org.json.JSONObject();
            
            emergenciaJson.put("id", emergenciaAtual.getId());
            emergenciaJson.put("dataInicio", emergenciaAtual.getDataInicio().getTime());
            emergenciaJson.put("dataFim", emergenciaAtual.getDataFim() != null ? emergenciaAtual.getDataFim().getTime() : 0);
            emergenciaJson.put("usuarioId", emergenciaAtual.getUsuarioId());
            emergenciaJson.put("usuarioNome", emergenciaAtual.getUsuarioNome());
            emergenciaJson.put("caminhoAudio", emergenciaAtual.getCaminhoAudio() != null ? emergenciaAtual.getCaminhoAudio() : "");
            
            // Salvar notificados
            org.json.JSONArray notificadosIdsArray = new org.json.JSONArray();
            for (String id : emergenciaAtual.getNotificadosIds()) {
                notificadosIdsArray.put(id);
            }
            emergenciaJson.put("notificadosIds", notificadosIdsArray);
            
            org.json.JSONArray notificadosNomesArray = new org.json.JSONArray();
            for (String nome : emergenciaAtual.getNotificadosNomes()) {
                notificadosNomesArray.put(nome);
            }
            emergenciaJson.put("notificadosNomes", notificadosNomesArray);
            
            // Salvar localizações
            org.json.JSONArray localizacoesArray = new org.json.JSONArray();
            for (Localizacao loc : emergenciaAtual.getLocalizacoes()) {
                org.json.JSONObject locJson = new org.json.JSONObject();
                locJson.put("latitude", loc.getLatitude());
                locJson.put("longitude", loc.getLongitude());
                locJson.put("timestamp", loc.getTimestamp().getTime());
                locJson.put("precisao", loc.getPrecisao());
                localizacoesArray.put(locJson);
            }
            emergenciaJson.put("localizacoes", localizacoesArray);
            
            emergenciasArray.put(emergenciaJson);
            
            prefs.edit().putString("emergencias", emergenciasArray.toString()).apply();
            Log.d(TAG, "Emergência salva: " + emergenciaAtual.getId());
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Erro ao salvar emergência", e);
        }
    }
    
    public java.util.List<Emergencia> carregarEmergencias() {
        java.util.List<Emergencia> emergencias = new java.util.ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        String emergenciasJson = prefs.getString("emergencias", "[]");
        
        try {
            org.json.JSONArray emergenciasArray = new org.json.JSONArray(emergenciasJson);
            for (int i = 0; i < emergenciasArray.length(); i++) {
                org.json.JSONObject emergenciaJson = emergenciasArray.getJSONObject(i);
                
                Emergencia emergencia = new Emergencia();
                emergencia.setId(emergenciaJson.getString("id"));
                emergencia.setDataInicio(new java.util.Date(emergenciaJson.getLong("dataInicio")));
                long dataFim = emergenciaJson.optLong("dataFim", 0);
                if (dataFim > 0) {
                    emergencia.setDataFim(new java.util.Date(dataFim));
                }
                emergencia.setUsuarioId(emergenciaJson.getString("usuarioId"));
                emergencia.setUsuarioNome(emergenciaJson.getString("usuarioNome"));
                emergencia.setCaminhoAudio(emergenciaJson.optString("caminhoAudio", ""));
                
                // Carregar notificados
                org.json.JSONArray notificadosIdsArray = emergenciaJson.getJSONArray("notificadosIds");
                java.util.List<String> notificadosIds = new java.util.ArrayList<>();
                for (int j = 0; j < notificadosIdsArray.length(); j++) {
                    notificadosIds.add(notificadosIdsArray.getString(j));
                }
                emergencia.setNotificadosIds(notificadosIds);
                
                org.json.JSONArray notificadosNomesArray = emergenciaJson.getJSONArray("notificadosNomes");
                java.util.List<String> notificadosNomes = new java.util.ArrayList<>();
                for (int j = 0; j < notificadosNomesArray.length(); j++) {
                    notificadosNomes.add(notificadosNomesArray.getString(j));
                }
                emergencia.setNotificadosNomes(notificadosNomes);
                
                // Carregar localizações
                java.util.List<Localizacao> localizacoes = new java.util.ArrayList<>();
                if (emergenciaJson.has("localizacoes")) {
                    try {
                        org.json.JSONArray localizacoesArray = emergenciaJson.getJSONArray("localizacoes");
                        Log.d(TAG, "Carregando " + localizacoesArray.length() + " localizações para emergência " + emergencia.getId());
                        for (int j = 0; j < localizacoesArray.length(); j++) {
                            org.json.JSONObject locJson = localizacoesArray.getJSONObject(j);
                            Localizacao loc = new Localizacao();
                            loc.setLatitude(locJson.getDouble("latitude"));
                            loc.setLongitude(locJson.getDouble("longitude"));
                            loc.setTimestamp(new java.util.Date(locJson.getLong("timestamp")));
                            loc.setPrecisao((float) locJson.getDouble("precisao"));
                            localizacoes.add(loc);
                        }
                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "Erro ao carregar localizações para emergência " + emergencia.getId(), e);
                    }
                } else {
                    Log.w(TAG, "Emergência " + emergencia.getId() + " não tem campo 'localizacoes'");
                }
                emergencia.setLocalizacoes(localizacoes);
                
                emergencias.add(emergencia);
            }
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Erro ao carregar emergências", e);
        }
        
        return emergencias;
    }
}

