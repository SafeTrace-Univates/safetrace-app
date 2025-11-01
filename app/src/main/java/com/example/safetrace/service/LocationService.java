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
    private static LocationService instance;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false;
    private Context context;
    
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
            
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(3000)
                    .setMaxUpdateDelayMillis(5000)
                    .build();
            
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            float precisao = location.getAccuracy();
                            EmergenciaService emergenciaService = EmergenciaService.getInstance(context);
                            emergenciaService.adicionarLocalizacao(
                                location.getLatitude(),
                                location.getLongitude(),
                                precisao
                            );
                            Log.d(TAG, "Localização atualizada: " + location.getLatitude() + ", " + location.getLongitude());
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
    
    public void pararRastreamento() {
        if (!isTracking || locationCallback == null) {
            return;
        }
        
        fusedLocationClient.removeLocationUpdates(locationCallback);
        locationCallback = null;
        isTracking = false;
        Log.d(TAG, "Rastreamento de localização parado");
    }
    
    public boolean isTracking() {
        return isTracking;
    }
}

