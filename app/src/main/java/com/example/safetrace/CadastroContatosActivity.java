package com.example.safetrace;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

public class CadastroContatosActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText editTextCodigo;
    private MaterialButton buttonSalvar;
    private ImageView imageViewMenu;
    private ScrollView scrollViewContatos;
    private LinearLayout layoutContatos;
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    // Lista dinâmica de contatos
    private List<String> contatos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_contatos);

        initializeViews();
        setupDrawer();
        setupClickListeners();
        updateContactList();
    }

    private void initializeViews() {
        editTextCodigo = findViewById(R.id.editTextCodigo);
        buttonSalvar = findViewById(R.id.buttonSalvar);
        imageViewMenu = findViewById(R.id.imageViewMenu);
        scrollViewContatos = findViewById(R.id.scrollViewContatos);
        layoutContatos = findViewById(R.id.layoutContatos);
        
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
        buttonSalvar.setOnClickListener(v -> saveContact());
        
        // Menu - abre o drawer lateral
        imageViewMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(navigationView);
        });
    }


    private void saveContact() {
        String codigo = editTextCodigo.getText().toString().trim();
        
        if (TextUtils.isEmpty(codigo)) {
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

        // Verificar se o contato já existe
        if (contatos.contains(codigo)) {
            Toast.makeText(this, "Este contato já foi cadastrado", Toast.LENGTH_SHORT).show();
            editTextCodigo.requestFocus();
            return;
        }

        // Adicionar contato à lista
        contatos.add(codigo);
        Toast.makeText(this, "Contato salvo: " + codigo, Toast.LENGTH_SHORT).show();

        editTextCodigo.setText("");
        updateContactList();
        
        // Manter o foco no campo de código para facilitar cadastro de mais contatos
        editTextCodigo.requestFocus();
    }

    private void updateContactList() {
        // Verificar se os componentes estão inicializados
        if (layoutContatos == null || scrollViewContatos == null) {
            return;
        }
        
        // Limpar a lista atual
        layoutContatos.removeAllViews();
        
        // Mostrar lista apenas se houver contatos cadastrados
        if (contatos.isEmpty()) {
            scrollViewContatos.setVisibility(View.GONE);
            return;
        }
        
        scrollViewContatos.setVisibility(View.VISIBLE);
        
        // Criar botões dinamicamente para cada contato
        for (int i = 0; i < contatos.size(); i++) {
            String contato = contatos.get(i);
            if (contato != null && !contato.isEmpty()) {
                MaterialButton button = createContactButton(contato, i);
                layoutContatos.addView(button);
            }
        }
    }
    
    private MaterialButton createContactButton(String phoneNumber, int index) {
        MaterialButton button = new MaterialButton(this);
        
        // Verificar se o número de telefone é válido
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return button;
        }
        
        // Configurar layout
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            (int) (250 * getResources().getDisplayMetrics().density), // 250dp
            (int) (61 * getResources().getDisplayMetrics().density)   // 61dp
        );
        params.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));
        button.setLayoutParams(params);
        
        // Configurar aparência
        button.setText(phoneNumber);
        button.setTextSize(18);
        button.setTextColor(getResources().getColor(R.color.primaria));
        button.setBackgroundTintList(getResources().getColorStateList(android.R.color.transparent));
        button.setStrokeColor(getResources().getColorStateList(R.color.primaria));
        button.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
        button.setCornerRadius((int) (6 * getResources().getDisplayMetrics().density));
        button.setRippleColor(getResources().getColorStateList(R.color.primaria));
        button.setAllCaps(false);
        
        // Remover efeitos de sombra
        button.setElevation(0);
        button.setStateListAnimator(null);
        
        // Configurar click listener
        button.setOnClickListener(v -> makeCall(phoneNumber));
        
        return button;
    }

    private void makeCall(String phoneNumber) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(dialIntent);
    }

    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            // Usar comportamento padrão do Android
            super.onBackPressed();
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            // Navegar para a tela principal
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.nav_contatos_confianca) {
            // Já estamos na tela de contatos de confiança, apenas fechar o drawer
            drawerLayout.closeDrawer(navigationView);
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
}
