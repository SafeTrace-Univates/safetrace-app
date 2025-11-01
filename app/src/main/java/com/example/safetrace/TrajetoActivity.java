package com.example.safetrace;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.File;

import com.example.safetrace.model.Emergencia;
import com.example.safetrace.model.Localizacao;
import com.example.safetrace.service.EmergenciaService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TrajetoActivity extends AppCompatActivity {

    private static final String TAG = "TrajetoActivity";
    private Object mMap; // Será GoogleMap se disponível
    private Emergencia emergencia;
    private MediaPlayer mediaPlayer;
    private MaterialButton buttonPlayAudio;
    private boolean isPlaying = false;
    private TextView textViewDados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa_trajeto);

        String emergenciaId = getIntent().getStringExtra("emergencia_id");
        if (emergenciaId == null) {
            Toast.makeText(this, "Erro ao carregar emergência", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Carregar emergência
        EmergenciaService emergenciaService = EmergenciaService.getInstance(this);
        List<Emergencia> emergencias = emergenciaService.carregarEmergencias();
        
        for (Emergencia e : emergencias) {
            if (e.getId().equals(emergenciaId)) {
                emergencia = e;
                break;
            }
        }

        if (emergencia == null) {
            Toast.makeText(this, "Emergência não encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Emergência carregada: " + emergencia.getId());
        Log.d(TAG, "Número de localizações: " + emergencia.getLocalizacoes().size());
        if (!emergencia.getLocalizacoes().isEmpty()) {
            Localizacao primeira = emergencia.getLocalizacoes().get(0);
            Log.d(TAG, "Primeira localização: " + primeira.getLatitude() + ", " + primeira.getLongitude());
        }

        initializeViews();
        setupClickListeners();
        exibirDados();
        
        // Configurar mapa programaticamente para evitar erro no layout
        try {
            Log.d(TAG, "Iniciando configuração do mapa...");
            
            // Verificar se Google Maps está disponível usando reflection
            Class<?> supportMapFragmentClass = Class.forName("com.google.android.gms.maps.SupportMapFragment");
            Log.d(TAG, "SupportMapFragment classe encontrada");
            
            Object mapFragment = supportMapFragmentClass.getMethod("newInstance").invoke(null);
            Log.d(TAG, "MapFragment criado");
            
            Class<?> fragmentManagerClass = getSupportFragmentManager().getClass();
            Object transaction = fragmentManagerClass.getMethod("beginTransaction").invoke(getSupportFragmentManager());
            Log.d(TAG, "Transaction iniciada");
            
            // Adicionar fragment ao container
            transaction.getClass().getMethod("replace", int.class, androidx.fragment.app.Fragment.class)
                .invoke(transaction, R.id.mapContainer, (androidx.fragment.app.Fragment) mapFragment);
            Log.d(TAG, "Fragment adicionado ao container");
            
            transaction.getClass().getMethod("commit").invoke(transaction);
            Log.d(TAG, "Transaction commitada");
            
            // Aguardar um pouco para o fragment ser adicionado antes de chamar getMapAsync
            // Usar um Handler para garantir que estamos na thread principal
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Log.d(TAG, "Chamando getMapAsync...");
                    
                    // Configurar callback do mapa
                    Class<?> onMapReadyCallbackClass = Class.forName("com.google.android.gms.maps.OnMapReadyCallback");
                    Log.d(TAG, "OnMapReadyCallback classe encontrada");
                    
                    Object callback = java.lang.reflect.Proxy.newProxyInstance(
                        Thread.currentThread().getContextClassLoader(),
                        new Class[]{onMapReadyCallbackClass},
                        (proxy, method, args) -> {
                            Log.d(TAG, "Método chamado no proxy: " + method.getName());
                            if (method.getName().equals("onMapReady")) {
                                Log.d(TAG, "onMapReady chamado");
                                mMap = args[0];
                                if (mMap != null) {
                                    Log.d(TAG, "Mapa inicializado com sucesso");
                                    
                                    // Tentar adicionar listener de erro de autorização
                                    try {
                                        Class<?> onErrorCallbackClass = Class.forName("com.google.android.gms.maps.GoogleMap$OnErrorCallback");
                                        Object errorCallback = java.lang.reflect.Proxy.newProxyInstance(
                                            Thread.currentThread().getContextClassLoader(),
                                            new Class[]{onErrorCallbackClass},
                                            (proxy2, method2, args2) -> {
                                                if (method2.getName().equals("onError")) {
                                                    Object exception = args2[0];
                                                    String errorMsg = exception != null ? exception.toString() : "Erro desconhecido";
                                                    Log.e(TAG, "Erro no mapa: " + errorMsg);
                                                    
                                                    // Verificar se é erro de autorização
                                                    if (errorMsg.contains("Authorization") || errorMsg.contains("API key") || errorMsg.contains("authorization")) {
                                                        runOnUiThread(() -> {
                                                            mostrarErroMapa("Erro de autorização do Google Maps. Verifique se a chave da API está configurada corretamente no Google Cloud Console.");
                                                        });
                                                    } else {
                                                        runOnUiThread(() -> {
                                                            mostrarErroMapa("Erro ao carregar o mapa: " + errorMsg);
                                                        });
                                                    }
                                                    return null;
                                                }
                                                return null;
                                            }
                                        );
                                        mMap.getClass().getMethod("setOnErrorListener", onErrorCallbackClass).invoke(mMap, errorCallback);
                                        Log.d(TAG, "Listener de erro do mapa adicionado");
                                    } catch (Exception e) {
                                        Log.w(TAG, "Não foi possível adicionar listener de erro do mapa", e);
                                    }
                                    
                                    // Executar configuração do mapa na thread principal
                                    runOnUiThread(() -> {
                                        configurarMapa();
                                    });
                                } else {
                                    Log.e(TAG, "Mapa é null após onMapReady");
                                    runOnUiThread(() -> {
                                        mostrarErroMapa("Falha ao inicializar o mapa. Verifique a configuração da API do Google Maps.");
                                    });
                                }
                                return null;
                            }
                            return null;
                        }
                    );
                    
                    mapFragment.getClass().getMethod("getMapAsync", onMapReadyCallbackClass)
                        .invoke(mapFragment, callback);
                    Log.d(TAG, "getMapAsync chamado com sucesso");
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao chamar getMapAsync", e);
                    e.printStackTrace();
                    mostrarErroMapa("Erro ao inicializar o mapa. Verifique a configuração.");
                }
            }, 500); // Aguardar 500ms para garantir que o fragment foi adicionado
                
        } catch (Exception e) {
            // Google Maps não disponível - mostrar mensagem e continuar sem mapa
            Log.e(TAG, "Erro ao inicializar mapa", e);
            e.printStackTrace();
            mostrarErroMapa("Erro ao carregar mapa. Verifique sua conexão ou configure a chave da API do Google Maps.");
        }
    }

    private void initializeViews() {
        buttonPlayAudio = findViewById(R.id.buttonPlayAudio);
        textViewDados = findViewById(R.id.textViewDados);
        
        ImageView imageViewVoltar = findViewById(R.id.imageViewVoltar);
        if (imageViewVoltar != null) {
            imageViewVoltar.setOnClickListener(v -> finish());
        }
        
        // Verificar se há áudio disponível e habilitar/desabilitar botão
        if (emergencia != null && emergencia.getCaminhoAudio() != null && !emergencia.getCaminhoAudio().isEmpty()) {
            File audioFile = new File(emergencia.getCaminhoAudio());
            if (!audioFile.exists()) {
                if (buttonPlayAudio != null) {
                    buttonPlayAudio.setEnabled(false);
                    buttonPlayAudio.setText(getString(R.string.audio_not_available));
                }
            }
        } else {
            if (buttonPlayAudio != null) {
                buttonPlayAudio.setEnabled(false);
                buttonPlayAudio.setText(getString(R.string.audio_not_available));
            }
        }
    }

    private void setupClickListeners() {
        if (buttonPlayAudio != null) {
            buttonPlayAudio.setOnClickListener(v -> toggleAudio());
        }
    }

    private void exibirDados() {
        if (emergencia == null || textViewDados == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        
        StringBuilder dados = new StringBuilder();
        dados.append("Data/Hora Início: ").append(dateFormat.format(emergencia.getDataInicio())).append("\n\n");
        
        if (emergencia.getDataFim() != null) {
            dados.append("Data/Hora Fim: ").append(dateFormat.format(emergencia.getDataFim())).append("\n");
            long duracao = emergencia.getDataFim().getTime() - emergencia.getDataInicio().getTime();
            long minutos = duracao / 60000;
            long segundos = (duracao % 60000) / 1000;
            dados.append("Duração: ").append(minutos).append(" min ").append(segundos).append(" s\n\n");
        } else {
            dados.append("Emergência ainda em andamento\n\n");
        }
        
        dados.append("Usuário: ").append(emergencia.getUsuarioNome()).append("\n\n");
        dados.append("Contatos Notificados: ");
        
        if (emergencia.getNotificadosNomes().isEmpty()) {
            dados.append("Nenhum");
        } else {
            for (int i = 0; i < emergencia.getNotificadosNomes().size(); i++) {
                if (i > 0) dados.append(", ");
                dados.append(emergencia.getNotificadosNomes().get(i));
            }
        }
        
        dados.append("\n\nLocalizações registradas: ").append(emergencia.getLocalizacoes().size());
        
        // Verificar se há áudio disponível
        if (emergencia.getCaminhoAudio() != null && !emergencia.getCaminhoAudio().isEmpty()) {
            File audioFile = new File(emergencia.getCaminhoAudio());
            if (audioFile.exists()) {
                long tamanhoMB = audioFile.length() / (1024 * 1024);
                long tamanhoKB = audioFile.length() / 1024;
                if (tamanhoMB > 0) {
                    dados.append("\nÁudio: ").append(tamanhoMB).append(" MB");
                } else {
                    dados.append("\nÁudio: ").append(tamanhoKB).append(" KB");
                }
            } else {
                dados.append("\nÁudio: Não encontrado");
            }
        } else {
            dados.append("\nÁudio: Não disponível");
        }
        
        textViewDados.setText(dados.toString());
    }

    private void configurarMapa() {
        Log.d(TAG, "configurarMapa chamado");
        
        if (mMap == null) {
            Log.w(TAG, "Mapa não inicializado");
            return;
        }
        
        if (emergencia == null) {
            Log.w(TAG, "Emergencia é null");
            return;
        }
        
        List<Localizacao> localizacoes = emergencia.getLocalizacoes();
        Log.d(TAG, "Tentando configurar mapa com " + localizacoes.size() + " localizações");
        
        if (localizacoes.isEmpty()) {
            Log.w(TAG, "Nenhuma localização disponível para esta emergência");
            mostrarMensagemMapa("Nenhuma localização registrada para esta emergência");
            return;
        }

        try {
            
            // Usar reflection para acessar métodos do GoogleMap
            Class<?> polylineOptionsClass = Class.forName("com.google.android.gms.maps.model.PolylineOptions");
            Object polylineOptions = polylineOptionsClass.getConstructor().newInstance();
            polylineOptionsClass.getMethod("color", int.class).invoke(polylineOptions, 0xFFFF0000);
            polylineOptionsClass.getMethod("width", float.class).invoke(polylineOptions, 5f);
            
            Class<?> latLngClass = Class.forName("com.google.android.gms.maps.model.LatLng");
            
            for (Localizacao loc : localizacoes) {
                Log.d(TAG, "Adicionando localização ao trajeto: " + loc.getLatitude() + ", " + loc.getLongitude());
                Object point = latLngClass.getConstructor(double.class, double.class)
                    .newInstance(loc.getLatitude(), loc.getLongitude());
                polylineOptionsClass.getMethod("add", latLngClass).invoke(polylineOptions, point);
            }
            
            Object polyline = mMap.getClass().getMethod("addPolyline", polylineOptionsClass).invoke(mMap, polylineOptions);
            Log.d(TAG, "Polyline adicionada ao mapa: " + (polyline != null ? "sucesso" : "falha"));
            
            // Adicionar marcadores
            if (!localizacoes.isEmpty()) {
                Localizacao inicio = localizacoes.get(0);
                Localizacao fim = localizacoes.get(localizacoes.size() - 1);
                
                Class<?> markerOptionsClass = Class.forName("com.google.android.gms.maps.model.MarkerOptions");
                
                Object inicioPoint = latLngClass.getConstructor(double.class, double.class)
                    .newInstance(inicio.getLatitude(), inicio.getLongitude());
                Object inicioMarker = markerOptionsClass.getConstructor().newInstance();
                markerOptionsClass.getMethod("position", latLngClass).invoke(inicioMarker, inicioPoint);
                markerOptionsClass.getMethod("title", String.class).invoke(inicioMarker, "Início");
                mMap.getClass().getMethod("addMarker", markerOptionsClass).invoke(mMap, inicioMarker);
                
                Object fimPoint = latLngClass.getConstructor(double.class, double.class)
                    .newInstance(fim.getLatitude(), fim.getLongitude());
                Object fimMarker = markerOptionsClass.getConstructor().newInstance();
                markerOptionsClass.getMethod("position", latLngClass).invoke(fimMarker, fimPoint);
                markerOptionsClass.getMethod("title", String.class).invoke(fimMarker, "Fim");
                mMap.getClass().getMethod("addMarker", markerOptionsClass).invoke(mMap, fimMarker);
                
                // Calcular bounds para centralizar mapa mostrando todas as localizações
                try {
                    Class<?> boundsBuilderClass = Class.forName("com.google.android.gms.maps.model.LatLngBounds$Builder");
                    Object boundsBuilder = boundsBuilderClass.getConstructor().newInstance();
                    
                    for (Localizacao loc : localizacoes) {
                        Object point = latLngClass.getConstructor(double.class, double.class)
                            .newInstance(loc.getLatitude(), loc.getLongitude());
                        boundsBuilderClass.getMethod("include", latLngClass).invoke(boundsBuilder, point);
                    }
                    
                    Object bounds = boundsBuilderClass.getMethod("build").invoke(boundsBuilder);
                    Class<?> cameraUpdateFactoryClass = Class.forName("com.google.android.gms.maps.CameraUpdateFactory");
                    Object cameraUpdate = cameraUpdateFactoryClass.getMethod("newLatLngBounds", bounds.getClass(), int.class)
                        .invoke(null, bounds, 100); // padding de 100px
                    mMap.getClass().getMethod("moveCamera", cameraUpdate.getClass()).invoke(mMap, cameraUpdate);
                } catch (Exception e) {
                    // Fallback: centralizar na primeira localização
                    Log.w(TAG, "Erro ao calcular bounds, usando fallback", e);
                    Class<?> cameraUpdateFactoryClass = Class.forName("com.google.android.gms.maps.CameraUpdateFactory");
                    Object cameraUpdate = cameraUpdateFactoryClass.getMethod("newLatLngZoom", latLngClass, float.class)
                        .invoke(null, inicioPoint, 15f);
                    mMap.getClass().getMethod("moveCamera", cameraUpdate.getClass()).invoke(mMap, cameraUpdate);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao configurar mapa", e);
            Toast.makeText(this, "Erro ao configurar mapa: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void toggleAudio() {
        if (emergencia == null || emergencia.getCaminhoAudio() == null || emergencia.getCaminhoAudio().isEmpty()) {
            Toast.makeText(this, "Áudio não disponível", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verificar se o arquivo existe
        File audioFile = new File(emergencia.getCaminhoAudio());
        if (!audioFile.exists()) {
            Toast.makeText(this, "Arquivo de áudio não encontrado", Toast.LENGTH_SHORT).show();
            buttonPlayAudio.setEnabled(false);
            return;
        }

        if (isPlaying) {
            pararAudio();
        } else {
            iniciarAudio();
        }
    }

    private void iniciarAudio() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            
            File audioFile = new File(emergencia.getCaminhoAudio());
            if (!audioFile.exists()) {
                Toast.makeText(this, "Arquivo de áudio não encontrado", Toast.LENGTH_SHORT).show();
                buttonPlayAudio.setEnabled(false);
                return;
            }
            
            Log.d(TAG, "Iniciando reprodução de áudio: " + emergencia.getCaminhoAudio());
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(emergencia.getCaminhoAudio());
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            isPlaying = true;
            buttonPlayAudio.setText(getString(R.string.pause_audio));
            buttonPlayAudio.setEnabled(true);
            
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Áudio finalizado");
                pararAudio();
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Erro ao reproduzir áudio: what=" + what + ", extra=" + extra);
                Toast.makeText(TrajetoActivity.this, "Erro ao reproduzir áudio", Toast.LENGTH_SHORT).show();
                pararAudio();
                return true;
            });
            
            Toast.makeText(this, "Reproduzindo áudio...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Erro ao iniciar reprodução de áudio", e);
            Toast.makeText(this, "Erro ao reproduzir áudio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            buttonPlayAudio.setEnabled(false);
        } catch (Exception e) {
            Log.e(TAG, "Erro inesperado ao iniciar áudio", e);
            Toast.makeText(this, "Erro ao reproduzir áudio", Toast.LENGTH_SHORT).show();
            buttonPlayAudio.setEnabled(false);
        }
    }

    private void pararAudio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao parar áudio", e);
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        buttonPlayAudio.setText(getString(R.string.play_audio));
        buttonPlayAudio.setEnabled(true);
    }
    
    private void mostrarErroMapa(String mensagem) {
        android.view.ViewGroup container = findViewById(R.id.mapContainer);
        if (container != null) {
            container.removeAllViews();
            TextView errorText = new TextView(this);
            errorText.setText(mensagem);
            errorText.setPadding(32, 32, 32, 32);
            errorText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            errorText.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
            errorText.setTextSize(14);
            container.addView(errorText);
        }
    }
    
    private void mostrarMensagemMapa(String mensagem) {
        android.view.ViewGroup container = findViewById(R.id.mapContainer);
        if (container != null) {
            container.removeAllViews();
            TextView messageText = new TextView(this);
            messageText.setText(mensagem);
            messageText.setPadding(32, 32, 32, 32);
            messageText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            messageText.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
            messageText.setTextSize(14);
            container.addView(messageText);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pararAudio();
    }
}


