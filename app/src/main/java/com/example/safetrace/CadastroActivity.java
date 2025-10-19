package com.example.safetrace;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        initializeViews();
        setupClickListeners();
        setupFocusListeners();
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
    }

    private void setupClickListeners() {
        buttonCadastrar.setOnClickListener(v -> performCadastro());
    }

    private void setupFocusListeners() {
        // Limpar placeholders quando o usuário clicar nos campos
        setupFocusListener(editTextNome, getString(R.string.name_placeholder));
        setupFocusListener(editTextEmail, getString(R.string.email_placeholder));
        setupFocusListener(editTextCpf, getString(R.string.cpf_placeholder));
        setupFocusListener(editTextTelefone, getString(R.string.phone_placeholder));
        setupFocusListener(editTextSenha, getString(R.string.password_placeholder));
    }

    private void setupFocusListener(EditText editText, String placeholder) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && editText.getText().toString().equals(placeholder)) {
                editText.setText("");
                if (placeholder.equals(getString(R.string.cpf_placeholder))) {
                    editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                } else if (placeholder.equals(getString(R.string.phone_placeholder))) {
                    editText.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                }
            } else if (!hasFocus && TextUtils.isEmpty(editText.getText().toString())) {
                editText.setText(placeholder);
                editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            }
        });
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
                if (!text.isEmpty() && !text.equals(getString(R.string.cpf_placeholder))) {
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
                if (!text.isEmpty() && !text.equals(getString(R.string.phone_placeholder))) {
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
                Intent intent = new Intent(CadastroActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CadastroActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
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
