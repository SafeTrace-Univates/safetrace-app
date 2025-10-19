package com.example.safetrace;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private MaterialButton buttonEmergencia;
    private MaterialButton buttonPoliciaMilitar;
    private MaterialButton buttonDefesaMulher;
    private MaterialButton buttonSamu;
    private MaterialButton buttonBombeiros;
    private MaterialButton buttonPoliciaCivil;
    private ImageView imageViewMenu;
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Aplicar edge-to-edge apenas ao conteúdo principal, não ao drawer
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Configurar o NavigationView para usar edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupDrawer();
        setupClickListeners();
        checkRecordingStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Verificar estado da gravação sempre que o app voltar ao foco
        checkRecordingStatus();
    }

    private void initializeViews() {
        buttonEmergencia = findViewById(R.id.buttonEmergencia);
        buttonPoliciaMilitar = findViewById(R.id.buttonPoliciaMilitar);
        buttonDefesaMulher = findViewById(R.id.buttonDefesaMulher);
        buttonSamu = findViewById(R.id.buttonSamu);
        buttonBombeiros = findViewById(R.id.buttonBombeiros);
        buttonPoliciaCivil = findViewById(R.id.buttonPoliciaCivil);
        imageViewMenu = findViewById(R.id.imageViewMenu);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
    }
    
    private void setupDrawer() {
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupClickListeners() {
        // Botão principal de emergência - gravação de áudio
        buttonEmergencia.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        // Botões de serviços de emergência - abrem discador
        buttonPoliciaMilitar.setOnClickListener(v -> openDialer("190"));
        buttonDefesaMulher.setOnClickListener(v -> openDialer("180"));
        buttonSamu.setOnClickListener(v -> openDialer("192"));
        buttonBombeiros.setOnClickListener(v -> openDialer("193"));
        buttonPoliciaCivil.setOnClickListener(v -> openDialer("197"));

        // Menu - abre o drawer lateral
        imageViewMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(navigationView);
        });
    }

    private void openDialer(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }
    
    private void startRecording() {
        if (checkPermissions()) {
            Intent serviceIntent = new Intent(this, RecordingService.class);
            serviceIntent.putExtra("action", "start_recording");
            // Ensure foreground service start path on modern Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            isRecording = true;
            updateEmergencyButton();
            Toast.makeText(this, "Gravação Iniciada", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissions();
        }
    }
    
    private void stopRecording() {
        Intent serviceIntent = new Intent(this, RecordingService.class);
        serviceIntent.putExtra("action", "stop_recording");
        startService(serviceIntent);
        
        isRecording = false;
        updateEmergencyButton();
        Toast.makeText(this, "Gravação Finalizada", Toast.LENGTH_SHORT).show();
    }
    
    private void updateEmergencyButton() {
        if (isRecording) {
            // Botão "pressionado" durante gravação - cor mais escura e texto diferente
            buttonEmergencia.setBackgroundTintList(getResources().getColorStateList(R.color.primaria));
            buttonEmergencia.setAlpha(0.7f);
            buttonEmergencia.setText("FINALIZAR EMERGÊNCIA");
            buttonEmergencia.setPressed(true);
        } else {
            // Botão normal
            buttonEmergencia.setBackgroundTintList(getResources().getColorStateList(R.color.primaria));
            buttonEmergencia.setAlpha(1.0f);
            buttonEmergencia.setText("EMERGÊNCIA");
            buttonEmergencia.setPressed(false);
        }
    }
    
    private boolean checkPermissions() {
        boolean hasRecord = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        // Storage permission is not needed for getExternalFilesDir on modern Android, but keep for older devices
        boolean needsStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
        boolean hasStorage = !needsStorage || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        boolean needsNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
        boolean hasNotif = !needsNotif || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        return hasRecord && hasStorage && hasNotif;
    }
    
    private void requestPermissions() {
        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startRecording();
            } else {
                Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void checkRecordingStatus() {
        // Verificar se há uma gravação em andamento
        isRecording = RecordingService.isServiceRecording(this);
        updateEmergencyButton();
        
        if (isRecording) {
            Toast.makeText(this, "Gravação em andamento detectada", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            // Já estamos na tela principal, apenas fechar o drawer
            drawerLayout.closeDrawer(navigationView);
        } else if (id == R.id.nav_contatos_confianca) {
            // Navegar para a tela de contatos de confiança
            Intent intent = new Intent(this, CadastroContatosActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_audios) {
            Intent intent = new Intent(this, AudioListActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            APIService.getInstance(this).logout(this, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Optionally clear token on success
                    SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                    prefs.edit().remove("api_token").apply();
                    Toast.makeText(MainActivity.this,
                            response.optString("message", "Desconectado com sucesso"),
                            Toast.LENGTH_SHORT).show();

                    // Navigate to LoginActivity, clear task/history
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this,
                            "Erro ao desconectar: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
            drawerLayout.closeDrawer(navigationView);
        }
        
        drawerLayout.closeDrawer(navigationView);
        return true;
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }

}