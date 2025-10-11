package com.example.safetrace;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextLogin;
    private EditText editTextSenha;
    private TextView textViewCadastro;
    private MaterialButton buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupClickListeners();
        setupBackPressedCallback();
    }

    private void initializeViews() {
        editTextLogin = findViewById(R.id.editTextLogin);
        editTextSenha = findViewById(R.id.editTextSenha);
        textViewCadastro = findViewById(R.id.textViewCadastro);
        buttonLogin = findViewById(R.id.buttonLogin);
    }

    private void setupClickListeners() {
        // Limpar placeholder quando o usuário clicar no campo de login
        editTextLogin.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && editTextLogin.getText().toString().equals(getString(R.string.login_placeholder))) {
                editTextLogin.setText("");
            } else if (!hasFocus && TextUtils.isEmpty(editTextLogin.getText().toString())) {
                editTextLogin.setText(getString(R.string.login_placeholder));
            }
        });

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
    }

    private void performLogin() {
        String login = editTextLogin.getText().toString().trim();
        String senha = editTextSenha.getText().toString().trim();

        // Validação básica
        if (TextUtils.isEmpty(login) || login.equals(getString(R.string.login_placeholder))) {
            Toast.makeText(this, getString(R.string.enter_login), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(senha)) {
            Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show();
            return;
        }

        // Validação simples (pode ser expandida com banco de dados)
        if (login.length() >= 3 && senha.length() >= 4) {
            Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.invalid_login), Toast.LENGTH_SHORT).show();
        }
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
