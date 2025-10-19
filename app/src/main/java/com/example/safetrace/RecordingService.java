package com.example.safetrace;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {
    private static final String TAG = "RecordingService";
    private static final String CHANNEL_ID = "recording_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private MediaRecorder mediaRecorder;
    private PowerManager.WakeLock wakeLock;
    private boolean isRecording = false;
    private String currentFilePath;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // Configurar WakeLock para manter o dispositivo ativo
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeTrace:RecordingWakeLock");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("start_recording".equals(action)) {
                startRecording();
            } else if ("stop_recording".equals(action)) {
                stopRecording();
            }
        }
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Gravação de Emergência",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notificação para gravação de áudio de emergência");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private void startRecording() {
        if (isRecording) {
            return;
        }
        
        try {
            // Criar diretório de áudios se não existir
            File audioDir = new File(getExternalFilesDir(null), "audios");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            
            // Gerar nome do arquivo com timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            currentFilePath = new File(audioDir, "emergency_" + timestamp + ".mp4").getAbsolutePath();
            
            // Configurar MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000); // 128 kbps
            mediaRecorder.setAudioSamplingRate(44100); // 44.1 kHz
            mediaRecorder.setOutputFile(currentFilePath);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            wakeLock.acquire();
            
            // Iniciar serviço em foreground
            try {
                startForeground(NOTIFICATION_ID, createNotification("Gravação Iniciada"));
            } catch (SecurityException se) {
                Log.e(TAG, "Falha ao iniciar foreground sem permissão de notificação", se);
            }
            
            Log.d(TAG, "Gravação iniciada: " + currentFilePath);
            
        } catch (IOException e) {
            Log.e(TAG, "Erro ao iniciar gravação", e);
            stopSelf();
        }
    }
    
    private void stopRecording() {
        if (!isRecording || mediaRecorder == null) {
            return;
        }
        
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            
            isRecording = false;
            
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            
            // Atualizar notificação
            updateNotification("Gravação Finalizada");
            
            Log.d(TAG, "Gravação finalizada: " + currentFilePath);
            
            // Parar o serviço após um delay
            new android.os.Handler().postDelayed(() -> stopSelf(), 2000);
            
        } catch (RuntimeException e) {
            Log.e(TAG, "Erro ao parar gravação", e);
            stopSelf();
        }
    }
    
    private Notification createNotification(String title) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("SafeTrace - Gravação de Emergência")
            .setSmallIcon(R.drawable.icone)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
    
    private void updateNotification(String title) {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, createNotification(title));
        } catch (SecurityException se) {
            Log.w(TAG, "Não foi possível atualizar notificação (perm. não concedida)", se);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (isRecording) {
            stopRecording();
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // Remover notificação
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    // Método estático para verificar se o serviço está gravando
    public static boolean isServiceRecording(android.content.Context context) {
        try {
            // Verificar se o serviço está rodando
            android.app.ActivityManager manager = (android.app.ActivityManager) context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (RecordingService.class.getName().equals(service.service.getClassName())) {
                    return service.foreground; // Se está em foreground, provavelmente está gravando
                }
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Erro ao verificar status do serviço", e);
        }
        return false;
    }
}
