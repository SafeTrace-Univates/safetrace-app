package com.example.safetrace;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.safetrace.APIService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

public class PerfilActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText editTextNome;
    private EditText editTextEmail;
    private EditText editTextTelefone;
    private EditText editTextSenha;
    private EditText editTextConfirmarSenha;
    private MaterialButton buttonSalvarPerfil;
    private ImageView imageViewMenu;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verificar autenticação antes de continuar
        if (!verificarAutenticacao()) {
            redirecionarParaLogin();
            return;
        }
        
        setContentView(R.layout.activity_perfil);

        editTextNome = findViewById(R.id.editTextNome);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextTelefone = findViewById(R.id.editTextTelefone);
        editTextSenha = findViewById(R.id.editTextSenha);
        editTextConfirmarSenha = findViewById(R.id.editTextConfirmarSenha);
        buttonSalvarPerfil = findViewById(R.id.buttonSalvarPerfil);
        imageViewMenu = findViewById(R.id.imageViewMenu);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (drawerLayout != null) {
            toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.setCheckedItem(R.id.nav_perfil);
        }

        if (imageViewMenu != null) {
            imageViewMenu.setOnClickListener(v -> {
                if (drawerLayout != null && navigationView != null) {
                    drawerLayout.openDrawer(navigationView);
                }
            });
        }

        carregarPerfilLocal();
        carregarPerfilApi();

        buttonSalvarPerfil.setOnClickListener(v -> salvarPerfilLocal());
    }

    private void carregarPerfilLocal() {
        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
        String nome = prefs.getString("user_name", "");
        String email = prefs.getString("user_email", "");
        String telefone = prefs.getString("user_phone", "");
        if (!TextUtils.isEmpty(nome)) {
            editTextNome.setText(nome);
        }
        if (!TextUtils.isEmpty(email)) {
            editTextEmail.setText(email);
        }
        if (!TextUtils.isEmpty(telefone)) {
            editTextTelefone.setText(telefone);
        }
    }

    private void carregarPerfilApi() {
        // Não altera APIService: apenas consome e salva localmente o que vier
        APIService.getInstance(this).getUserProfile(this, new APIService.APIServiceCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    String nome = response.optString("name", null);
                    String email = response.optString("email", null);
                    String telefone = response.optString("phone", null);
                    if (!TextUtils.isEmpty(nome)) {
                        editTextNome.setText(nome);
                    }
                    if (!TextUtils.isEmpty(email)) {
                        editTextEmail.setText(email);
                    }
                    if (!TextUtils.isEmpty(telefone)) {
                        editTextTelefone.setText(telefone);
                    }
                    // Persistir localmente para o app usar
                    SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    if (!TextUtils.isEmpty(nome)) editor.putString("user_name", nome);
                    if (!TextUtils.isEmpty(email)) editor.putString("user_email", email);
                    if (!TextUtils.isEmpty(telefone)) editor.putString("user_phone", telefone);
                    editor.apply();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onError(String error) {
                // Silencioso: permanece com dados locais
            }
        });
    }

    private void salvarPerfilLocal() {
        String nome = editTextNome.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String telefone = editTextTelefone.getText().toString().trim();
        String senha = editTextSenha.getText().toString();
        String confirmar = editTextConfirmarSenha.getText().toString();

        if (TextUtils.isEmpty(nome)) {
            Toast.makeText(this, "Informe o nome", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "E-mail inválido", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(senha) || !TextUtils.isEmpty(confirmar)) {
            if (!senha.equals(confirmar)) {
                Toast.makeText(this, "As senhas não conferem", Toast.LENGTH_SHORT).show();
                return;
            }
            if (senha.length() < 6) {
                Toast.makeText(this, "A senha deve ter ao menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Verificar conectividade
        boolean hasInternet = com.example.safetrace.util.NetworkUtils.isNetworkAvailable(this);
        
        if (hasInternet) {
            // Enviar à API
            String senhaParaEnviar = TextUtils.isEmpty(senha) ? null : senha;
            APIService.getInstance(this).updateUserProfile(this, nome, email, telefone, senhaParaEnviar, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Dados já foram atualizados localmente pelo APIService
                    Toast.makeText(PerfilActivity.this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(PerfilActivity.this, "Erro ao atualizar perfil: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Sem conexão - salvar apenas localmente
            SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
            prefs.edit()
                    .putString("user_name", nome)
                    .putString("user_email", email)
                    .putString("user_phone", telefone)
                    .putString("user_password", TextUtils.isEmpty(senha) ? prefs.getString("user_password", "") : senha)
                    .apply();

            Toast.makeText(this, "Sem conexão. Dados salvos apenas localmente.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            startActivity(new android.content.Intent(this, MainActivity.class));
            finish();
        } else if (id == R.id.nav_contatos_confianca) {
            startActivity(new android.content.Intent(this, CadastroContatosActivity.class));
            finish();
        } else if (id == R.id.nav_historico) {
            startActivity(new android.content.Intent(this, HistoricoActivity.class));
            finish();
        } else if (id == R.id.nav_perfil) {
            if (drawerLayout != null && navigationView != null) drawerLayout.closeDrawer(navigationView);
        } else if (id == R.id.nav_logout) {
            APIService.getInstance(this).logout(this, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                    prefs.edit().remove("api_token").apply();
                    Toast.makeText(PerfilActivity.this,
                            response.optString("message", "Desconectado com sucesso"),
                            Toast.LENGTH_SHORT).show();
                    android.content.Intent intent = new android.content.Intent(PerfilActivity.this, LoginActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(PerfilActivity.this, "Erro ao desconectar: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (drawerLayout != null && navigationView != null) drawerLayout.closeDrawer(navigationView);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_perfil);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && navigationView != null && drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }
    
    private boolean verificarAutenticacao() {
        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
        String token = prefs.getString("api_token", null);
        String userId = prefs.getString("user_id", null);
        return token != null && !token.isEmpty() && userId != null && !userId.isEmpty();
    }
    
    private void redirecionarParaLogin() {
        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


