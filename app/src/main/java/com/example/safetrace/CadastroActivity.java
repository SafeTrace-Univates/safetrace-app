package com.example.safetrace;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

public class CadastroActivity extends AppCompatActivity {

    private EditText editTextNome;
    private EditText editTextEmail;
    private EditText editTextCpf;
    private EditText editTextTelefone;
    private EditText editTextSenha;
    private MaterialButton buttonCadastrar;
    private ImageView imageViewVoltar;
    private ImageView imageViewToggleSenha;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        initializeViews();
        setupClickListeners();
        setupTextWatchers();
        setupBackPressedCallback();
    }

    private void initializeViews() {
        editTextNome = findViewById(R.id.editTextNome);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextCpf = findViewById(R.id.editTextCpf);
        editTextTelefone = findViewById(R.id.editTextTelefone);
        editTextSenha = findViewById(R.id.editTextSenha);
        buttonCadastrar = findViewById(R.id.buttonCadastrar);
        imageViewVoltar = findViewById(R.id.imageView3);
        imageViewToggleSenha = findViewById(R.id.imageViewToggleSenha);
    }

    private void setupClickListeners() {
        buttonCadastrar.setOnClickListener(v -> performCadastro());
        
        // Botão voltar
        imageViewVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(CadastroActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
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


    private void setupTextWatchers() {
        // Formatação do CPF
        editTextCpf.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString();
                if (!text.isEmpty()) {
                    String formatted = formatCPF(text);
                    if (!formatted.equals(text)) {
                        editTextCpf.removeTextChangedListener(this);
                        editTextCpf.setText(formatted);
                        editTextCpf.setSelection(formatted.length());
                        editTextCpf.addTextChangedListener(this);
                    }
                }
            }
        });

        // Formatação do telefone
        editTextTelefone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString();
                if (!text.isEmpty()) {
                    String formatted = formatPhone(text);
                    if (!formatted.equals(text)) {
                        editTextTelefone.removeTextChangedListener(this);
                        editTextTelefone.setText(formatted);
                        editTextTelefone.setSelection(formatted.length());
                        editTextTelefone.addTextChangedListener(this);
                    }
                }
            }
        });
    }

    private String formatCPF(String text) {
        // Remove todos os caracteres não numéricos
        String numbers = text.replaceAll("[^0-9]", "");
        
        // Limita a 11 dígitos
        if (numbers.length() > 11) {
            numbers = numbers.substring(0, 11);
        }
        
        // Aplica a máscara do CPF
        if (numbers.length() <= 3) {
            return numbers;
        } else if (numbers.length() <= 6) {
            return numbers.substring(0, 3) + "." + numbers.substring(3);
        } else if (numbers.length() <= 9) {
            return numbers.substring(0, 3) + "." + numbers.substring(3, 6) + "." + numbers.substring(6);
        } else {
            return numbers.substring(0, 3) + "." + numbers.substring(3, 6) + "." + numbers.substring(6, 9) + "-" + numbers.substring(9);
        }
    }

    private String formatPhone(String text) {
        // Remove todos os caracteres não numéricos
        String numbers = text.replaceAll("[^0-9]", "");
        
        // Limita a 11 dígitos
        if (numbers.length() > 11) {
            numbers = numbers.substring(0, 11);
        }
        
        // Aplica a máscara do telefone
        if (numbers.length() <= 2) {
            return numbers;
        } else if (numbers.length() <= 7) {
            return "(" + numbers.substring(0, 2) + ") " + numbers.substring(2);
        } else if (numbers.length() <= 11) {
            return "(" + numbers.substring(0, 2) + ") " + numbers.substring(2, 7) + "-" + numbers.substring(7);
        } else {
            return "(" + numbers.substring(0, 2) + ") " + numbers.substring(2, 7) + "-" + numbers.substring(7, 11);
        }
    }

    private void performCadastro() {
        String nome = editTextNome.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String cpf = editTextCpf.getText().toString().trim();
        String telefone = editTextTelefone.getText().toString().trim();
        String senha = editTextSenha.getText().toString().trim();

        APIService.getInstance(this).register(this, nome, email, telefone, cpf, senha, new APIService.APIServiceCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                String token = response.optString("token");
                
                // Salvar nome do usuário no SharedPreferences
                SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                prefs.edit().putString("user_name", nome).apply();
                
                // Buscar perfil do usuário para obter ID
                if (token != null && !token.isEmpty()) {
                    APIService.getInstance(CadastroActivity.this).getUserProfile(CadastroActivity.this, new APIService.APIServiceCallback() {
                        @Override
                        public void onSuccess(JSONObject userResponse) {
                            // ID e nome já foram salvos
                            Intent intent = new Intent(CadastroActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            // Mesmo com erro, continuar
                            Intent intent = new Intent(CadastroActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                } else {
                    Intent intent = new Intent(CadastroActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CadastroActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Validações
//        if (!validateNome(nome)) return;
//        if (!validateEmail(email)) return;
//        if (!validateCPF(cpf)) return;
//        if (!validateTelefone(telefone)) return;
//        if (!validateSenha(senha)) return;




        // Se todas as validações passaram, cadastrar usuário
//        Toast.makeText(this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show();
//
//        // Redirecionar para tela de login
//        Intent intent = new Intent(CadastroActivity.this, LoginActivity.class);
//        startActivity(intent);
//        finish();
    }

    private boolean validateNome(String nome) {
        if (TextUtils.isEmpty(nome) || nome.equals(getString(R.string.name_placeholder))) {
            Toast.makeText(this, getString(R.string.enter_full_name), Toast.LENGTH_SHORT).show();
            editTextNome.requestFocus();
            return false;
        }
        if (nome.length() < 3) {
            Toast.makeText(this, getString(R.string.name_min_length), Toast.LENGTH_SHORT).show();
            editTextNome.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email) || email.equals(getString(R.string.email_placeholder))) {
            Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateCPF(String cpf) {
        if (TextUtils.isEmpty(cpf) || cpf.equals(getString(R.string.cpf_placeholder))) {
            Toast.makeText(this, getString(R.string.enter_cpf), Toast.LENGTH_SHORT).show();
            editTextCpf.requestFocus();
            return false;
        }
        // Remove caracteres não numéricos para validação
        String cpfNumbers = cpf.replaceAll("[^0-9]", "");
        if (cpfNumbers.length() != 11) {
            Toast.makeText(this, getString(R.string.cpf_length), Toast.LENGTH_SHORT).show();
            editTextCpf.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateTelefone(String telefone) {
        if (TextUtils.isEmpty(telefone) || telefone.equals(getString(R.string.phone_placeholder))) {
            Toast.makeText(this, getString(R.string.enter_phone), Toast.LENGTH_SHORT).show();
            editTextTelefone.requestFocus();
            return false;
        }
        // Remove caracteres não numéricos para validação
        String phoneNumbers = telefone.replaceAll("[^0-9]", "");
        if (phoneNumbers.length() < 10) {
            Toast.makeText(this, getString(R.string.phone_min_length), Toast.LENGTH_SHORT).show();
            editTextTelefone.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateSenha(String senha) {
        if (TextUtils.isEmpty(senha) || senha.equals(getString(R.string.password_placeholder))) {
            Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show();
            editTextSenha.requestFocus();
            return false;
        }
        if (senha.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_length), Toast.LENGTH_SHORT).show();
            editTextSenha.requestFocus();
            return false;
        }
        return true;
    }

    private void setupBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Voltar para tela de login
                Intent intent = new Intent(CadastroActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}
