package com.example.safetrace.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService {
    private static final String TAG = "LocationService";
    private static final float PRECISAO_MINIMA_METROS = 25.0f; // Filtrar localizações com precisão pior que 25 metros
    private static LocationService instance;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false;
    private Context context;
    private Location ultimaLocalizacaoValida; // Armazenar a melhor localização disponível
    
    private LocationService(Context context) {
        this.context = context.getApplicationContext();
        try {
            this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao inicializar FusedLocationProviderClient", e);
            // Continuar sem o cliente de localização
        }
    }
    
    public static synchronized LocationService getInstance(Context context) {
        if (instance == null) {
            instance = new LocationService(context);
        }
        return instance;
    }
    
    public void iniciarRastreamento() {
        if (isTracking) {
            Log.w(TAG, "Rastreamento já está ativo");
            return;
        }
        
        if (fusedLocationClient == null) {
            Log.e(TAG, "FusedLocationProviderClient não disponível");
            return;
        }
        
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED 
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permissão de localização não concedida");
                return;
            }
            
            // Configuração otimizada para máxima precisão:
            // - PRIORITY_HIGH_ACCURACY: Usa GPS para máxima precisão
            // - setWaitForAccurateLocation(true): Espera por localização precisa antes de enviar
            // - setMinUpdateIntervalMillis(2000): Atualiza no mínimo a cada 2 segundos
            // - setMaxUpdateDelayMillis(3500): Máximo de 3,5 segundos de atraso
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3500)
                    .setWaitForAccurateLocation(true) // Esperar por localização precisa
                    .setMinUpdateIntervalMillis(2000) // Atualizar no mínimo a cada 2 segundos
                    .setMaxUpdateDelayMillis(3500) // Máximo de 3,5 segundos de atraso
                    .build();
            
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        Location location = locationResult.getLastLocation();
                        float precisao = location.getAccuracy();
                        
                        // Verificar se a localização tem precisão válida
                        if (precisao <= 0) {
                            // Se não tem informação de precisão, considerar válida
                            processarLocalizacao(location);
                        } else if (precisao <= PRECISAO_MINIMA_METROS) {
                            // Localização com boa precisão (≤ 25 metros)
                            processarLocalizacao(location);
                            Log.d(TAG, "Localização precisa aceita: " + location.getLatitude() + ", " + location.getLongitude() + " (precisão: " + precisao + "m)");
                        } else {
                            // Localização com baixa precisão (> 25 metros)
                            Log.w(TAG, "Localização rejeitada por baixa precisão: " + precisao + "m (máximo: " + PRECISAO_MINIMA_METROS + "m)");
                            
                            // Se não temos uma localização válida ainda, usar a melhor disponível
                            if (ultimaLocalizacaoValida == null || location.getAccuracy() < ultimaLocalizacaoValida.getAccuracy()) {
                                Log.d(TAG, "Usando melhor localização disponível mesmo com precisão reduzida: " + location.getAccuracy() + "m");
                                processarLocalizacao(location);
                            }
                        }
                    }
                }
            };
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
            
            isTracking = true;
            Log.d(TAG, "Rastreamento de localização iniciado");
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar rastreamento", e);
        }
    }
    
    /**
     * Processa uma localização válida:
     * - Compara com a última localização válida e mantém a melhor (menor precisão = melhor)
     * - Adiciona ao EmergenciaService
     */
    private void processarLocalizacao(Location location) {
        // Se temos uma localização válida anterior, comparar e manter a melhor
        if (ultimaLocalizacaoValida != null) {
            // Se a nova localização tem melhor precisão (menor valor = melhor), usar ela
            if (location.getAccuracy() > 0 && location.getAccuracy() < ultimaLocalizacaoValida.getAccuracy()) {
                ultimaLocalizacaoValida = location;
                Log.d(TAG, "Nova melhor localização encontrada: precisão " + location.getAccuracy() + "m");
            } else if (ultimaLocalizacaoValida.getAccuracy() <= 0 || location.getAccuracy() <= ultimaLocalizacaoValida.getAccuracy()) {
                // Nova localização é melhor ou igual, atualizar
                ultimaLocalizacaoValida = location;
            } else {
                // Usar a localização anterior que era melhor
                location = ultimaLocalizacaoValida;
                Log.d(TAG, "Usando localização anterior melhor (precisão: " + ultimaLocalizacaoValida.getAccuracy() + "m)");
            }
        } else {
            // Primeira localização válida
            ultimaLocalizacaoValida = location;
        }
        
        float precisao = location.getAccuracy();
        EmergenciaService emergenciaService = EmergenciaService.getInstance(context);
        emergenciaService.adicionarLocalizacao(
            location.getLatitude(),
            location.getLongitude(),
            precisao
        );
        Log.d(TAG, "Localização adicionada: " + location.getLatitude() + ", " + location.getLongitude() + " (precisão: " + precisao + "m)");
    }
    
    public void pararRastreamento() {
        if (!isTracking || locationCallback == null) {
            return;
        }
        
        fusedLocationClient.removeLocationUpdates(locationCallback);
        locationCallback = null;
        isTracking = false;
        ultimaLocalizacaoValida = null; // Limpar última localização ao parar
        Log.d(TAG, "Rastreamento de localização parado");
    }
    
    public boolean isTracking() {
        return isTracking;
    }
}

