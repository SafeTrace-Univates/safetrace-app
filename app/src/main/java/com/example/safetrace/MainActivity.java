package com.example.safetrace;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.safetrace.service.EmergenciaService;
import com.example.safetrace.service.LocationService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    
    private MaterialButton buttonEmergencia;
    private LinearLayout buttonPoliciaMilitar;
    private LinearLayout buttonDefesaMulher;
    private LinearLayout buttonSamu;
    private LinearLayout buttonBombeiros;
    private LinearLayout buttonPoliciaCivil;
    private LinearLayout buttonDefesaCivil;
    private ImageView imageViewMenu;
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private EmergenciaService emergenciaService;
    private LocationService locationService;
    private boolean emergenciaEmAndamento = false;
    private android.content.ServiceConnection serviceConnection;
    private com.example.safetrace.service.EmergenciaForegroundService foregroundService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verificar autenticação antes de continuar
        if (!verificarAutenticacao()) {
            // Se não estiver autenticado, redirecionar para LoginActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        try {
            emergenciaService = EmergenciaService.getInstance(this);
            locationService = LocationService.getInstance(this);
            
            // Verificar se já existe emergência em andamento através do serviço
            // Verificar se o foreground service está rodando
            android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(android.content.Context.ACTIVITY_SERVICE);
            boolean serviceRunning = false;
            for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (com.example.safetrace.service.EmergenciaForegroundService.class.getName().equals(service.service.getClassName())) {
                    serviceRunning = true;
                    break;
                }
            }
            emergenciaEmAndamento = serviceRunning || (emergenciaService != null && emergenciaService.isEmergenciaEmAndamento());
            
            // Verificar permissões
            verificarPermissoes();
        } catch (Exception e) {
            // Se houver erro ao inicializar serviços, continuar sem eles
            android.util.Log.e("MainActivity", "Erro ao inicializar serviços de emergência", e);
            emergenciaEmAndamento = false;
        }
        
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
        
        // Processar fila de dados pendentes quando app iniciar (se houver conexão)
        com.example.safetrace.service.SyncService syncService = com.example.safetrace.service.SyncService.getInstance(this);
        syncService.processPendingQueue();
        
        // Atualizar estado do botão APÓS inicializar as views
        if (buttonEmergencia != null) {
            atualizarBotaoEmergencia();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Marcar o item correto do menu quando voltar para esta tela
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_home);
        }
        
        // Processar fila de dados pendentes quando app voltar ao foreground (se houver conexão)
        com.example.safetrace.service.SyncService syncService = com.example.safetrace.service.SyncService.getInstance(this);
        syncService.processPendingQueue();
    }

    private void initializeViews() {
        buttonEmergencia = findViewById(R.id.buttonEmergencia);
        buttonPoliciaMilitar = findViewById(R.id.buttonPoliciaMilitar);
        buttonDefesaMulher = findViewById(R.id.buttonDefesaMulher);
        buttonSamu = findViewById(R.id.buttonSamu);
        buttonBombeiros = findViewById(R.id.buttonBombeiros);
        buttonPoliciaCivil = findViewById(R.id.buttonPoliciaCivil);
        buttonDefesaCivil = findViewById(R.id.buttonDefesaCivil);
        imageViewMenu = findViewById(R.id.imageViewMenu);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
    }
    
    private void setupDrawer() {
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        navigationView.setNavigationItemSelectedListener(this);
        
        // Marcar o item home como selecionado
        navigationView.setCheckedItem(R.id.nav_home);
    }

    private void setupClickListeners() {
        // Botão principal de emergência
        buttonEmergencia.setOnClickListener(v -> {
            try {
                if (emergenciaEmAndamento) {
                    // Finalizar emergência
                    mostrarDialogoFinalizarEmergencia();
                } else {
                    // Iniciar emergência
                    iniciarEmergencia();
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Erro ao clicar no botão de emergência", e);
                Toast.makeText(this, "Erro ao processar ação. Tente novamente.", Toast.LENGTH_SHORT).show();
            }
        });

        // Botões de serviços de emergência - abrem discador
        buttonPoliciaMilitar.setOnClickListener(v -> openDialer("190"));
        buttonDefesaMulher.setOnClickListener(v -> openDialer("180"));
        buttonSamu.setOnClickListener(v -> openDialer("192"));
        buttonBombeiros.setOnClickListener(v -> openDialer("193"));
        buttonPoliciaCivil.setOnClickListener(v -> openDialer("197"));
        buttonDefesaCivil.setOnClickListener(v -> openDialer("199"));

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
        } else if (id == R.id.nav_historico) {
            // Navegar para a tela de histórico
            Intent intent = new Intent(this, HistoricoActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_perfil) {
            Intent intent = new Intent(this, PerfilActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            // Limpar dados locais primeiro
            SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
            String token = prefs.getString("api_token", null);
            
            // Tentar fazer logout na API se houver token
            if (token != null && !token.isEmpty()) {
                APIService.getInstance(this).logout(this, new APIService.APIServiceCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        limparDadosUsuario();
                        Toast.makeText(MainActivity.this,
                                response.optString("message", "Desconectado com sucesso"),
                                Toast.LENGTH_SHORT).show();
                        redirecionarParaLogin();
                    }

                    @Override
                    public void onError(String error) {
                        // Mesmo com erro na API, limpar dados locais e deslogar
                        limparDadosUsuario();
                        Toast.makeText(MainActivity.this,
                                "Desconectado localmente",
                                Toast.LENGTH_SHORT).show();
                        redirecionarParaLogin();
                    }
                });
            } else {
                // Sem token, apenas limpar dados locais
                limparDadosUsuario();
                redirecionarParaLogin();
            }
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Não parar emergência aqui, deixar o usuário finalizar manualmente
    }
    
    private boolean verificarAutenticacao() {
        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
        String token = prefs.getString("api_token", null);
        String userId = prefs.getString("user_id", null);
        
        // Verificar se há token e user_id válidos
        if (token == null || token.isEmpty()) {
            android.util.Log.d("MainActivity", "Token não encontrado, redirecionando para login");
            return false;
        }
        
        if (userId == null || userId.isEmpty()) {
            android.util.Log.d("MainActivity", "User ID não encontrado, redirecionando para login");
            return false;
        }
        
        return true;
    }
    
    private void limparDadosUsuario() {
        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
        prefs.edit()
                .remove("api_token")
                .remove("user_id")
                .remove("user_name")
                .remove("user_email")
                .remove("user_phone")
                .apply();
    }
    
    private void redirecionarParaLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void verificarPermissoes() {
        List<String> permissoesNecessarias = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            permissoesNecessarias.add(Manifest.permission.RECORD_AUDIO);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissoesNecessarias.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissoesNecessarias.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissoesNecessarias.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!permissoesNecessarias.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissoesNecessarias.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    private void iniciarEmergencia() {
        android.util.Log.d("MainActivity", "iniciarEmergencia chamado");
        
        // Verificar se os serviços estão disponíveis
        if (emergenciaService == null || locationService == null) {
            android.util.Log.e("MainActivity", "Serviços não disponíveis");
            Toast.makeText(this, "Serviços de emergência não disponíveis. Reinicie o app.", Toast.LENGTH_LONG).show();
            return;
        }
        
        try {
            // Buscar contatos de confiança da API
            android.util.Log.d("MainActivity", "Buscando contatos da API...");
            APIService.getInstance(this).getContacts(this, true, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    android.util.Log.d("MainActivity", "Contatos recebidos com sucesso");
                    try {
                        JSONArray data = response.getJSONArray("data");
                        List<String> contatosIds = new ArrayList<>();
                        List<String> contatosNomes = new ArrayList<>();
                        
                        android.util.Log.d("MainActivity", "Processando " + data.length() + " contatos");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject contactJson = data.getJSONObject(i);
                            String nickname = contactJson.isNull("nickname") ? null : contactJson.getString("nickname");
                            JSONObject userJson = contactJson.optJSONObject("user");
                            
                            if (userJson != null) {
                                String userId = userJson.optString("id", null);
                                String userName = nickname != null && !nickname.isEmpty() 
                                    ? nickname 
                                    : userJson.optString("name", "Desconhecido");
                                
                                if (userId != null) {
                                    contatosIds.add(userId);
                                    contatosNomes.add(userName);
                                }
                            }
                        }
                        
                        // Obter informações do usuário atual
                        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                        String usuarioId = prefs.getString("user_id", "desconhecido");
                        String usuarioNome = prefs.getString("user_name", null);
                        
                        // Se não tiver nome salvo, tentar buscar do perfil novamente ANTES de iniciar
                        if (usuarioNome == null || usuarioNome.isEmpty() || usuarioNome.equals("Usuário")) {
                            android.util.Log.w("MainActivity", "Nome do usuário não encontrado, buscando do perfil...");
                            APIService.getInstance(MainActivity.this).getUserProfile(MainActivity.this, new APIService.APIServiceCallback() {
                                @Override
                                public void onSuccess(JSONObject userResponse) {
                                    // Buscar nome atualizado e iniciar emergência
                                    String nomeAtualizado = getSharedPreferences("safetrace_prefs", MODE_PRIVATE).getString("user_name", "Usuário");
                                    android.util.Log.d("MainActivity", "Nome atualizado após buscar perfil: " + nomeAtualizado);
                                    
                                    // Continuar com o processo de emergência usando o nome atualizado
                                    continuarIniciarEmergencia(contatosIds, contatosNomes, nomeAtualizado, usuarioId);
                                }

                                @Override
                                public void onError(String error) {
                                    android.util.Log.e("MainActivity", "Erro ao buscar perfil: " + error);
                                    // Continuar com nome padrão
                                    continuarIniciarEmergencia(contatosIds, contatosNomes, "Usuário", usuarioId);
                                }
                            });
                            return;
                        }
                        
                        continuarIniciarEmergencia(contatosIds, contatosNomes, usuarioNome, usuarioId);
                        
                            
                    } catch (JSONException e) {
                        android.util.Log.e("MainActivity", "Erro ao processar JSON de contatos", e);
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Erro ao carregar contatos", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Erro inesperado ao iniciar emergência", e);
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Erro ao iniciar emergência: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("MainActivity", "Erro ao buscar contatos: " + error);
                    Toast.makeText(MainActivity.this, "Erro ao carregar contatos: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Erro ao iniciar processo de emergência", e);
            e.printStackTrace();
            Toast.makeText(this, "Erro ao iniciar emergência: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void continuarIniciarEmergencia(List<String> contatosIds, List<String> contatosNomes, String usuarioNome, String usuarioId) {
        android.util.Log.d("MainActivity", "Continuando emergência para usuário: " + usuarioNome + " (ID: " + usuarioId + ")");
        
        // Verificar se temos o ID do usuário
        if (usuarioId == null || usuarioId.isEmpty() || usuarioId.equals("desconhecido")) {
            android.util.Log.e("MainActivity", "ID do usuário não disponível");
            Toast.makeText(this, "Erro: ID do usuário não disponível. Faça login novamente.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Verificar conectividade antes de tentar criar alerta na API
        boolean hasInternet = com.example.safetrace.util.NetworkUtils.isNetworkAvailable(this);
        
        if (hasInternet) {
            // Criar alerta na API conforme ER model: apenas ref_user (sem name, sem contacts)
            // Os contatos serão vinculados via tabela alert_contact separadamente
            android.util.Log.d("MainActivity", "Conexão disponível. Criando alerta na API com ref_user: " + usuarioId);
            APIService.getInstance(this).createAlert(this, usuarioId, null, new ArrayList<String>(), new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    android.util.Log.d("MainActivity", "Alerta criado com sucesso na API");
                    try {
                        // Extrair o ID do alerta criado da resposta
                        String alertId = null;
                        if (response.has("data")) {
                            JSONObject data = response.getJSONObject("data");
                            alertId = data.optString("id", null);
                        } else if (response.has("id")) {
                            alertId = response.optString("id", null);
                        }
                        
                        // Salvar ID do alerta para uso posterior (localizações, gravações, etc.)
                        if (alertId != null) {
                            SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                            prefs.edit().putString("current_alert_id", alertId).apply();
                            android.util.Log.d("MainActivity", "ID do alerta salvo: " + alertId);
                        }
                        
                        // Continuar com o processo de emergência local
                        iniciarServicoEmergencia(contatosIds, contatosNomes, usuarioNome, usuarioId, alertId);
                    } catch (JSONException e) {
                        android.util.Log.e("MainActivity", "Erro ao processar resposta do alerta", e);
                        // Continuar mesmo sem o ID do alerta
                        iniciarServicoEmergencia(contatosIds, contatosNomes, usuarioNome, usuarioId, null);
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("MainActivity", "Erro ao criar alerta na API: " + error);
                    // Se falhar, adicionar à fila pendente e continuar localmente
                    com.example.safetrace.service.SyncService syncService = com.example.safetrace.service.SyncService.getInstance(MainActivity.this);
                    syncService.addToPendingQueue(null, usuarioId, null, null, null);
                    Toast.makeText(MainActivity.this, "Sem conexão. Dados serão salvos localmente e enviados depois.", Toast.LENGTH_LONG).show();
                    // Continuar mesmo sem criar na API - será criado depois quando houver conexão
                    iniciarServicoEmergencia(contatosIds, contatosNomes, usuarioNome, usuarioId, null);
                }
            });
        } else {
            // Sem internet - adicionar à fila pendente e continuar localmente
            android.util.Log.d("MainActivity", "Sem conexão com internet. Salvando localmente e adicionando à fila pendente.");
            com.example.safetrace.service.SyncService syncService = com.example.safetrace.service.SyncService.getInstance(this);
            syncService.addToPendingQueue(null, usuarioId, null, null, null);
            Toast.makeText(this, "Sem conexão. Dados serão salvos localmente e enviados quando houver internet.", Toast.LENGTH_LONG).show();
            // Continuar com emergência local - alert será criado depois quando houver conexão
            iniciarServicoEmergencia(contatosIds, contatosNomes, usuarioNome, usuarioId, null);
        }
    }
    
    private void iniciarServicoEmergencia(List<String> contatosIds, List<String> contatosNomes, String usuarioNome, String usuarioId, String alertId) {
        // Salvar contatos temporariamente no SharedPreferences para o serviço acessar
        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
        org.json.JSONArray idsArray = new org.json.JSONArray();
        org.json.JSONArray nomesArray = new org.json.JSONArray();
        for (String id : contatosIds) {
            idsArray.put(id);
        }
        for (String nome : contatosNomes) {
            nomesArray.put(nome);
        }
        prefs.edit()
            .putString("temp_contatos_ids", idsArray.toString())
            .putString("temp_contatos_nomes", nomesArray.toString())
            .apply();
        
        // Salvar ID do alerta se disponível
        if (alertId != null) {
            prefs.edit().putString("current_alert_id", alertId).apply();
        }
        
        // Iniciar foreground service
        Intent serviceIntent = new Intent(this, com.example.safetrace.service.EmergenciaForegroundService.class);
        serviceIntent.setAction("START_EMERGENCY");
        serviceIntent.putExtra("usuario_id", usuarioId);
        serviceIntent.putExtra("usuario_nome", usuarioNome);
        if (alertId != null) {
            serviceIntent.putExtra("alert_id", alertId);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        emergenciaEmAndamento = true;
        atualizarBotaoEmergencia();
        
        Toast.makeText(MainActivity.this, "Emergência iniciada! Gravando em segundo plano.", 
            Toast.LENGTH_LONG).show();
    }
    
    private void finalizarEmergencia() {
        try {
            // Parar o foreground service
            Intent serviceIntent = new Intent(this, com.example.safetrace.service.EmergenciaForegroundService.class);
            serviceIntent.setAction("STOP_EMERGENCY");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            emergenciaEmAndamento = false;
            atualizarBotaoEmergencia();
            
            Toast.makeText(this, "Emergência finalizada e salva no histórico.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Erro ao finalizar emergência", e);
            Toast.makeText(this, "Erro ao finalizar emergência", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void atualizarBotaoEmergencia() {
        try {
            if (emergenciaEmAndamento) {
                buttonEmergencia.setText(getString(R.string.finish_emergency));
                // Usar cor específica para emergência ativa
                int color = ContextCompat.getColor(this, R.color.emergencia_ativa);
                android.content.res.ColorStateList colorStateList = android.content.res.ColorStateList.valueOf(color);
                buttonEmergencia.setBackgroundTintList(colorStateList);
            } else {
                buttonEmergencia.setText(getString(R.string.emergency));
                // Criar ColorStateList a partir da cor diretamente
                int color = ContextCompat.getColor(this, R.color.primaria);
                android.content.res.ColorStateList colorStateList = android.content.res.ColorStateList.valueOf(color);
                buttonEmergencia.setBackgroundTintList(colorStateList);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Erro ao atualizar botão de emergência", e);
            // Em caso de erro, apenas atualizar o texto
            if (emergenciaEmAndamento) {
                buttonEmergencia.setText(getString(R.string.finish_emergency));
            } else {
                buttonEmergencia.setText(getString(R.string.emergency));
            }
        }
    }
    
    private void mostrarDialogoFinalizarEmergencia() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Finalizar Emergência")
            .setMessage("Deseja finalizar a gravação da emergência?")
            .setPositiveButton("Sim", (dialog, which) -> finalizarEmergencia())
            .setNegativeButton("Não", null)
            .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean todasPermissoes = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    todasPermissoes = false;
                    break;
                }
            }
            if (!todasPermissoes) {
                Toast.makeText(this, "É necessário conceder todas as permissões para usar a funcionalidade de emergência.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

}