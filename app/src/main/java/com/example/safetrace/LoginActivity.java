package com.example.safetrace;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextLogin;
    private EditText editTextSenha;
    private TextView textViewCadastro;
    private MaterialButton buttonLogin;
    private ImageView imageViewToggleSenha;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verificar se já existe uma sessão ativa (token salvo)
        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
        String token = prefs.getString("api_token", null);
        String userId = prefs.getString("user_id", null);
        
        if (token != null && !token.isEmpty() && userId != null && !userId.isEmpty()) {
            // Validar token fazendo uma chamada à API para verificar se ainda é válido
            android.util.Log.d("LoginActivity", "Token encontrado, validando com a API...");
            APIService.getInstance(this).getUserProfile(this, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Token válido, redirecionar para MainActivity
                    android.util.Log.d("LoginActivity", "Token válido, redirecionando para MainActivity");
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    // Token inválido ou expirado, limpar dados e mostrar tela de login
                    android.util.Log.d("LoginActivity", "Token inválido ou expirado: " + error);
                    limparDadosUsuario();
                    mostrarTelaLogin();
                }
            });
        } else {
            // Sem token ou user_id, mostrar tela de login
            mostrarTelaLogin();
        }
    }
    
    private void mostrarTelaLogin() {
        setContentView(R.layout.activity_login);
        initializeViews();
        setupClickListeners();
        setupBackPressedCallback();
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

    private void initializeViews() {
        editTextLogin = findViewById(R.id.editTextLogin);
        editTextSenha = findViewById(R.id.editTextSenha);
        textViewCadastro = findViewById(R.id.textViewCadastro);
        buttonLogin = findViewById(R.id.buttonLogin);
        imageViewToggleSenha = findViewById(R.id.imageViewToggleSenha);
    }

    private void setupClickListeners() {
        // Botão de login
        buttonLogin.setOnClickListener(v -> performLogin());

        // Navegar para tela de cadastro
        textViewCadastro.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CadastroActivity.class);
            startActivity(intent);
        });

        // Implementar login ao pressionar Enter no campo de senha
        editTextSenha.setOnEditorActionListener((v, actionId, event) -> {
            performLogin();
            return true;
        });

        // Toggle mostrar/ocultar senha
        imageViewToggleSenha.setOnClickListener(v -> togglePasswordVisibility());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Ocultar senha
            editTextSenha.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imageViewToggleSenha.setImageResource(R.drawable.baseline_visibility_off_24);
            isPasswordVisible = false;
        } else {
            // Mostrar senha
            editTextSenha.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            imageViewToggleSenha.setImageResource(R.drawable.baseline_visibility_24);
            isPasswordVisible = true;
        }
        
        // Mover cursor para o final
        editTextSenha.setSelection(editTextSenha.getText().length());
    }

    private void performLogin() {
        String login = editTextLogin.getText().toString().trim();
        String senha = editTextSenha.getText().toString().trim();

        // Validação básica
        if (TextUtils.isEmpty(login)) {
            Toast.makeText(this, "Por favor, informe seu email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(senha)) {
            Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show();
            return;
        }

        APIService.getInstance(this).login(this, login, senha, new APIService.APIServiceCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                String token = response.optString("token");
                
                // Buscar informações do usuário para obter o ID e nome
                if (token != null && !token.isEmpty()) {
                    // Tentar buscar perfil, mas não bloquear login se falhar
                    APIService.getInstance(LoginActivity.this).getUserProfile(LoginActivity.this, new APIService.APIServiceCallback() {
                        @Override
                        public void onSuccess(JSONObject userResponse) {
                            // Verificar se o nome foi salvo
                            SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                            String nomeSalvo = prefs.getString("user_name", null);
                            android.util.Log.d("LoginActivity", "Nome salvo após getUserProfile: " + nomeSalvo);
                            
                            // Usuário logado com sucesso, ID e nome já foram salvos pelo APIService
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.w("LoginActivity", "Erro ao buscar perfil do usuário (pode ser esperado): " + error);
                            // Mesmo com erro ao buscar perfil, permitir login
                            // O nome pode estar na resposta do login
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                } else {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                // Remover System.out.println em produção
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Voltar para MainActivity se pressionar o botão voltar
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}
