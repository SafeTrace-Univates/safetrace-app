package com.example.safetrace.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;

import com.example.safetrace.service.SyncService;
import com.example.safetrace.util.NetworkUtils;

/**
 * BroadcastReceiver para detectar mudanças na conectividade de rede
 * e processar a fila de dados pendentes quando a conexão for restaurada
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        
        // Para Android 7.0+ (API 24+), usar verificação direta
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                Log.d(TAG, "Conexão com internet detectada. Processando fila pendente...");
                // Usar postDelayed para garantir que o contexto está pronto
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    SyncService.getInstance(context).processPendingQueue();
                }, 1000); // Aguardar 1 segundo para garantir que a conexão está estável
            }
        } else {
            // Para versões antigas, usar a ação de conectividade
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    Log.d(TAG, "Conexão com internet restaurada. Processando fila pendente...");
                    SyncService.getInstance(context).processPendingQueue();
                } else {
                    Log.d(TAG, "Conexão com internet perdida. Dados serão salvos localmente.");
                }
            }
        }
    }
}

