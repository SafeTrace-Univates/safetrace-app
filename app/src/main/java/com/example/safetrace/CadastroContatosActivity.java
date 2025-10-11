package com.example.safetrace;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class CadastroContatosActivity extends AppCompatActivity {

    private EditText editTextCodigo;
    private MaterialButton buttonSalvar;
    private MaterialButton buttonContato1;
    private MaterialButton buttonContato2;
    private MaterialButton buttonContato3;

    // Simulação de contatos salvos (pode ser expandido com banco de dados)
    private String contato1 = "";
    private String contato2 = "";
    private String contato3 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_contatos);

        initializeViews();
        setupClickListeners();
        setupFocusListener();
        setupBackPressedCallback();
        updateContactButtons();
    }

    private void initializeViews() {
        editTextCodigo = findViewById(R.id.editTextCodigo);
        buttonSalvar = findViewById(R.id.buttonSalvar);
        buttonContato1 = findViewById(R.id.buttonContato1);
        buttonContato2 = findViewById(R.id.buttonContato2);
        buttonContato3 = findViewById(R.id.buttonContato3);
    }

    private void setupClickListeners() {
        buttonSalvar.setOnClickListener(v -> saveContact());

        buttonContato1.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(contato1)) {
                makeCall(contato1);
            } else {
                Toast.makeText(CadastroContatosActivity.this, getString(R.string.contact1_not_configured), Toast.LENGTH_SHORT).show();
            }
        });

        buttonContato2.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(contato2)) {
                makeCall(contato2);
            } else {
                Toast.makeText(CadastroContatosActivity.this, getString(R.string.contact2_not_configured), Toast.LENGTH_SHORT).show();
            }
        });

        buttonContato3.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(contato3)) {
                makeCall(contato3);
            } else {
                Toast.makeText(CadastroContatosActivity.this, getString(R.string.contact3_not_configured), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFocusListener() {
        editTextCodigo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && editTextCodigo.getText().toString().equals(getString(R.string.code_placeholder))) {
                editTextCodigo.setText("");
            } else if (!hasFocus && TextUtils.isEmpty(editTextCodigo.getText().toString())) {
                editTextCodigo.setText(getString(R.string.code_placeholder));
            }
        });
    }

    private void saveContact() {
        String codigo = editTextCodigo.getText().toString().trim();
        
        if (TextUtils.isEmpty(codigo) || codigo.equals(getString(R.string.code_placeholder))) {
            Toast.makeText(this, getString(R.string.enter_contact_code), Toast.LENGTH_SHORT).show();
            editTextCodigo.requestFocus();
            return;
        }

        // Remove caracteres não numéricos
        codigo = codigo.replaceAll("[^0-9]", "");
        
        if (codigo.length() < 10) {
            Toast.makeText(this, getString(R.string.code_min_length), Toast.LENGTH_SHORT).show();
            editTextCodigo.requestFocus();
            return;
        }

        // Salvar no primeiro slot vazio
        if (TextUtils.isEmpty(contato1)) {
            contato1 = codigo;
            Toast.makeText(this, getString(R.string.contact1_saved, codigo), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(contato2)) {
            contato2 = codigo;
            Toast.makeText(this, getString(R.string.contact2_saved, codigo), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(contato3)) {
            contato3 = codigo;
            Toast.makeText(this, getString(R.string.contact3_saved, codigo), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.all_contacts_filled), Toast.LENGTH_SHORT).show();
            return;
        }

        editTextCodigo.setText(getString(R.string.code_placeholder));
        updateContactButtons();
    }

    private void updateContactButtons() {
        buttonContato1.setText(TextUtils.isEmpty(contato1) ? getString(R.string.contact1) : getString(R.string.contact1) + "\n" + contato1);
        buttonContato2.setText(TextUtils.isEmpty(contato2) ? getString(R.string.contact2) : getString(R.string.contact2) + "\n" + contato2);
        buttonContato3.setText(TextUtils.isEmpty(contato3) ? getString(R.string.contact3) : getString(R.string.contact3) + "\n" + contato3);
    }

    private void makeCall(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    private void setupBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Voltar para MainActivity
                Intent intent = new Intent(CadastroContatosActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}
