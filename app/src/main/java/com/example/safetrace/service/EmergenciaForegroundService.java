package com.example.safetrace.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.safetrace.MainActivity;
import com.example.safetrace.R;

import java.util.List;

public class EmergenciaForegroundService extends Service {
    private static final String TAG = "EmergenciaFgService";
    private static final String CHANNEL_ID = "emergencia_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private final IBinder binder = new LocalBinder();
    private EmergenciaService emergenciaService;
    private LocationService locationService;
    private String usuarioId;
    private String usuarioNome;
    private List<String> contatosIds;
    private List<String> contatosNomes;
    
    public class LocalBinder extends Binder {
        public EmergenciaForegroundService getService() {
            return EmergenciaForegroundService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Serviço criado");
        emergenciaService = EmergenciaService.getInstance(this);
        locationService = LocationService.getInstance(this);
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            
            if ("START_EMERGENCY".equals(action)) {
                usuarioId = intent.getStringExtra("usuario_id");
                usuarioNome = intent.getStringExtra("usuario_nome");
                
                // Recuperar listas de contatos do SharedPreferences (salvas temporariamente)
                SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                String contatosIdsJson = prefs.getString("temp_contatos_ids", "[]");
                String contatosNomesJson = prefs.getString("temp_contatos_nomes", "[]");
                
                try {
                    org.json.JSONArray idsArray = new org.json.JSONArray(contatosIdsJson);
                    org.json.JSONArray nomesArray = new org.json.JSONArray(contatosNomesJson);
                    
                    contatosIds = new java.util.ArrayList<>();
                    contatosNomes = new java.util.ArrayList<>();
                    
                    for (int i = 0; i < idsArray.length(); i++) {
                        contatosIds.add(idsArray.getString(i));
                    }
                    for (int i = 0; i < nomesArray.length(); i++) {
                        contatosNomes.add(nomesArray.getString(i));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao carregar contatos", e);
                    contatosIds = new java.util.ArrayList<>();
                    contatosNomes = new java.util.ArrayList<>();
                }
                
                iniciarEmergencia();
            } else if ("STOP_EMERGENCY".equals(action)) {
                finalizarEmergencia();
                stopForeground(true);
                stopSelf();
            }
        }
        
        return START_STICKY; // Reiniciar serviço se for morto pelo sistema
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    private void iniciarEmergencia() {
        Log.d(TAG, "Iniciando emergência no foreground service");
        
        // Iniciar emergência
        if (emergenciaService != null && usuarioId != null && usuarioNome != null) {
            emergenciaService.iniciarEmergencia(usuarioId, usuarioNome, contatosIds, contatosNomes);
        }
        
        // Iniciar rastreamento de localização
        if (locationService != null) {
            locationService.iniciarRastreamento();
        }
        
        // Iniciar como foreground service com notificação
        // Para Android 14+ (API 34+), especificar os tipos de foreground service
        Notification notification = createNotification();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE |
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        
        Log.d(TAG, "Emergência iniciada em foreground");
    }
    
    private void finalizarEmergencia() {
        Log.d(TAG, "Finalizando emergência no foreground service");
        
        // Parar rastreamento de localização
        if (locationService != null) {
            locationService.pararRastreamento();
        }
        
        // Finalizar emergência
        if (emergenciaService != null) {
            emergenciaService.finalizarEmergencia();
        }
        
        Log.d(TAG, "Emergência finalizada");
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Emergência em Andamento",
                NotificationManager.IMPORTANCE_LOW // Importância baixa para ser silenciosa
            );
            channel.setDescription("Notificação silenciosa para emergência ativa");
            channel.setShowBadge(false);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null); // Sem som
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            );
        }
        
        String usuarioNomeExibido = usuarioNome != null ? usuarioNome : "Usuário";
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergência em Andamento")
            .setContentText("Gravando áudio e localização para: " + usuarioNomeExibido)
            .setSmallIcon(R.drawable.icone) // Usar o ícone do app
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Prioridade baixa para ser silenciosa
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true) // Notificação silenciosa
            .build();
    }
    
    public boolean isEmergenciaAtiva() {
        return emergenciaService != null && emergenciaService.isEmergenciaEmAndamento();
    }
}

