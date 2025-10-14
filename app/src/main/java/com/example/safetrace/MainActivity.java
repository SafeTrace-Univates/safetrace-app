package com.example.safetrace;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    
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
        // Botão principal de emergência - desabilitado por enquanto
        buttonEmergencia.setOnClickListener(v -> {
            // Por enquanto não faz nada quando clicar no botão de emergência
            Toast.makeText(MainActivity.this, "Funcionalidade em desenvolvimento", Toast.LENGTH_SHORT).show();
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
        } else if (id == R.id.nav_logout) {
            // Fazer logout - voltar para tela de login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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