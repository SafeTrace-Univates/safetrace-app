package com.example.safetrace;

import android.content.Intent;
import android.content.SharedPreferences;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CadastroContatosActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText editTextCodigo;
    private MaterialButton buttonSalvar;
    private ImageView imageViewMenu;
    private ScrollView scrollViewContatos;
    private LinearLayout layoutContatos;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    // Lista dinâmica de contatos cadastrados manualmente
    private List<String> contatos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_contatos);

        initializeViews();
        setupDrawer();
        setupClickListeners();

        // Load contacts from API and then update full contact list UI
        loadContactsFromApi();

        // Also update UI for manually added contacts
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
        imageViewMenu.setOnClickListener(v -> drawerLayout.openDrawer(navigationView));
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

        // Adicionar contato à lista
        contatos.add(codigo);
        Toast.makeText(this, "Contato salvo: " + codigo, Toast.LENGTH_SHORT).show();

        editTextCodigo.setText("");
        updateContactList();

        // Manter o foco no campo de código para facilitar cadastro de mais contatos
        editTextCodigo.requestFocus();
    }

    private void updateContactList() {
        if (layoutContatos == null || scrollViewContatos == null) return;

        layoutContatos.removeAllViews();

        // Append manually added contacts first
        for (String contato : contatos) {
            if (contato != null && !contato.isEmpty()) {
                MaterialButton button = createContactButton(contato);
                layoutContatos.addView(button);
            }
        }
        // scrollView should be visible if there is any contact
        scrollViewContatos.setVisibility(contatos.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void loadContactsFromApi() {
        APIService.getInstance(this).getContacts(this, true, new APIService.APIServiceCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray data = response.getJSONArray("data");

                    // Show scroll view and clear previous API loaded buttons if any
                    scrollViewContatos.setVisibility(View.VISIBLE);

                    // Create and add buttons for contacts coming from API
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject contactJson = data.getJSONObject(i);
                        String nickname = contactJson.isNull("nickname") ? null : contactJson.getString("nickname");
                        JSONObject userJson = contactJson.optJSONObject("user");
                        String displayName;

                        if (nickname != null && !nickname.isEmpty()) {
                            displayName = nickname;
                        } else if (userJson != null) {
                            displayName = userJson.optString("name", "Desconhecido");
                        } else {
                            displayName = "Desconhecido";
                        }

                        MaterialButton btn = createContactButton(displayName);
                        layoutContatos.addView(btn);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(CadastroContatosActivity.this, "Erro ao processar contatos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CadastroContatosActivity.this, "Erro ao carregar contatos: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MaterialButton createContactButton(String phoneNumber) {
        MaterialButton button = new MaterialButton(this);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return button;
        }

        // Layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (250 * getResources().getDisplayMetrics().density), // 250dp
                (int) (61 * getResources().getDisplayMetrics().density)   // 61dp
        );
        params.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (8 * getResources().getDisplayMetrics().density));
        button.setLayoutParams(params);

        // Appearance
        button.setText(phoneNumber);
        button.setTextSize(18);
        button.setTextColor(getResources().getColor(R.color.primaria));
        button.setBackgroundTintList(getResources().getColorStateList(android.R.color.transparent));
        button.setStrokeColor(getResources().getColorStateList(R.color.primaria));
        button.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
        button.setCornerRadius((int) (6 * getResources().getDisplayMetrics().density));
        button.setRippleColor(getResources().getColorStateList(R.color.primaria));
        button.setAllCaps(false);

        button.setElevation(0);
        button.setStateListAnimator(null);

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
            super.onBackPressed();
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
            drawerLayout.closeDrawer(navigationView);
        } else if (id == R.id.nav_logout) {
            APIService.getInstance(this).logout(this, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
                    prefs.edit().remove("api_token").apply();
                    Toast.makeText(CadastroContatosActivity.this,
                            response.optString("message", "Desconectado com sucesso"),
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CadastroContatosActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(CadastroContatosActivity.this,
                            "Erro ao desconectar: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
            drawerLayout.closeDrawer(navigationView);
        }

        drawerLayout.closeDrawer(navigationView);
        return true;
    }
}
