package com.example.safetrace;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.safetrace.model.Emergencia;
import com.example.safetrace.service.EmergenciaService;
import com.google.android.material.navigation.NavigationView;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoricoActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ImageView imageViewMenu;
    private LinearLayout containerChamadas;
    private EmergenciaService emergenciaService;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        emergenciaService = EmergenciaService.getInstance(this);
        
        initializeViews();
        setupDrawer();
        setupClickListeners();
        carregarEmergencias();
    }

    private void initializeViews() {
        imageViewMenu = findViewById(R.id.imageViewMenu);
        containerChamadas = findViewById(R.id.containerChamadas);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
    }
    
    private void setupDrawer() {
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
        
        // Marcar o item do histórico como selecionado
        navigationView.setCheckedItem(R.id.nav_historico);
    }

    private void setupClickListeners() {
        // Menu - abre o drawer lateral
        if (imageViewMenu != null) {
            imageViewMenu.setOnClickListener(v -> {
                if (drawerLayout != null && navigationView != null) {
                    drawerLayout.openDrawer(navigationView);
                }
            });
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_contatos_confianca) {
            Intent intent = new Intent(this, CadastroContatosActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_historico) {
            // Já estamos na tela de histórico, apenas fechar o drawer
            drawerLayout.closeDrawer(navigationView);
        } else if (id == R.id.nav_perfil) {
            Intent intent = new Intent(this, PerfilActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_logout) {
            APIService.getInstance(this).logout(this, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                    prefs.edit().remove("api_token").apply();
                    Toast.makeText(HistoricoActivity.this,
                            response.optString("message", "Desconectado com sucesso"),
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(HistoricoActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(HistoricoActivity.this,
                            "Erro ao desconectar: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        return true;
    }
    
    private void carregarEmergencias() {
        List<Emergencia> emergencias = emergenciaService.carregarEmergencias();
        
        containerChamadas.removeAllViews();
        
        if (emergencias.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(getString(R.string.no_emergencies_registered));
            emptyText.setTextSize(16);
            emptyText.setPadding(32, 32, 32, 32);
            emptyText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            emptyText.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            );
            emptyText.setLayoutParams(params);
            containerChamadas.addView(emptyText);
            return;
        }
        
        // Ordenar por hora de início do alert: mais novas primeiro (ordem decrescente)
        emergencias.sort(new Comparator<Emergencia>() {
            @Override
            public int compare(Emergencia e1, Emergencia e2) {
                // Comparar por hora de início do alert (mais recente primeiro = ordem decrescente)
                Date hora1 = e1.getHoraInicioAlerta();
                Date hora2 = e2.getHoraInicioAlerta();
                if (hora1 == null) hora1 = e1.getDataInicio();
                if (hora2 == null) hora2 = e2.getDataInicio();
                return hora2.compareTo(hora1);
            }
        });
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        for (Emergencia emergencia : emergencias) {
            View cardView = criarCardEmergencia(emergencia, dateFormat, timeFormat);
            containerChamadas.addView(cardView);
        }
    }
    
    private View criarCardEmergencia(Emergencia emergencia, SimpleDateFormat dateFormat, SimpleDateFormat timeFormat) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.card_emergencia, containerChamadas, false);
        
        TextView txtData = cardView.findViewById(R.id.txtData);
        TextView txtHoraInicio = cardView.findViewById(R.id.txtHoraInicio);
        TextView txtHoraFim = cardView.findViewById(R.id.txtHoraFim);
        TextView txtNomeAcionador = cardView.findViewById(R.id.txtNomeAcionador);
        TextView txtNomeAcionados = cardView.findViewById(R.id.txtNomeAcionados);
        View btnVerMapa = cardView.findViewById(R.id.btnVerMapa);
        
        // Usar hora de início do alert (created_at do alert)
        Date horaInicio = emergencia.getHoraInicioAlerta();
        if (horaInicio == null) {
            horaInicio = emergencia.getDataInicio(); // Fallback
        }
        
        txtData.setText(dateFormat.format(horaInicio));
        txtHoraInicio.setText(getString(R.string.start_time, timeFormat.format(horaInicio)));
        
        // Usar hora final da última localização (created_at da última location)
        Date horaFim = emergencia.getHoraFimUltimaLocalizacao();
        if (horaFim != null) {
            txtHoraFim.setText(getString(R.string.end_time, timeFormat.format(horaFim)));
        } else {
            // Se não houver localizações, verificar se tem dataFim (compatibilidade)
            if (emergencia.getDataFim() != null) {
                txtHoraFim.setText(getString(R.string.end_time, timeFormat.format(emergencia.getDataFim())));
            } else {
                txtHoraFim.setText(getString(R.string.end_time_in_progress));
            }
        }
        
        txtNomeAcionador.setText(emergencia.getUsuarioNome());
        
        // Concatenar nomes dos notificados
        StringBuilder nomesNotificados = new StringBuilder();
        for (int i = 0; i < emergencia.getNotificadosNomes().size(); i++) {
            if (i > 0) nomesNotificados.append(", ");
            nomesNotificados.append(emergencia.getNotificadosNomes().get(i));
        }
        txtNomeAcionados.setText(nomesNotificados.length() > 0 ? nomesNotificados.toString() : "Nenhum");
        
        btnVerMapa.setOnClickListener(v -> {
            Intent intent = new Intent(this, TrajetoActivity.class);
            intent.putExtra("emergencia_id", emergencia.getId());
            startActivity(intent);
        });
        
        return cardView;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && navigationView != null && drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recarregar emergências ao voltar para a tela
        carregarEmergencias();
        // Marcar o item correto do menu quando voltar para esta tela
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_historico);
        }
    }
}
