package com.example.safetrace;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CadastroContatosActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private EditText editTextCodigo;
    private MaterialButton buttonSalvar;
    private MaterialButton buttonBordaGerarQr;
    private MaterialButton buttonBordaAdicionarQr;
    private ImageView imageViewMenu;
    private ScrollView scrollViewContatos;
    private LinearLayout layoutContatos;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    // Lista de contatos será carregada apenas da API
    
    // ActivityResultLauncher para receber resultado do QR Code
    private ActivityResultLauncher<Intent> qrCodeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_contatos);

        initializeViews();
        setupDrawer();
        setupClickListeners();
        setupQrCodeLauncher();

        // Carregar contatos da API
        loadContactsFromApi();
    }

    private void setupQrCodeLauncher() {
        qrCodeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String qrCodeText = result.getData().getStringExtra("qr_code_result");
                            if (qrCodeText != null && !qrCodeText.isEmpty()) {
                                processQRCodeResult(qrCodeText);
                            }
                        }
                    }
                }
        );
    }

    private void processQRCodeResult(String qrCodeText) {
        saveContactApi( qrCodeText );
    }
    
    private void showQRCodeDialog() {
        // Buscar user_id do SharedPreferences
        SharedPreferences prefs = getSharedPreferences("safetrace_prefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        
        if (userId == null || userId.isEmpty()) {

            APIService.getInstance(this).getUserProfile(this, new APIService.APIServiceCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    // Tentar novamente agora que o user_id deve estar salvo
                    String userId = getSharedPreferences("safetrace_prefs", MODE_PRIVATE).getString("user_id", null);
                    if (userId != null && !userId.isEmpty()) {
                        showQRCodeDialogWithId(userId);
                    } else {
                        Toast.makeText(CadastroContatosActivity.this, "Erro ao obter ID do usuário", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(CadastroContatosActivity.this, "Erro ao buscar perfil: " + error, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        
        showQRCodeDialogWithId(userId);
    }
    
    private void showQRCodeDialogWithId(String userCode) {
        // Criar dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_code, null);
        
        ImageView imageViewQRCode = dialogView.findViewById(R.id.imageViewQRCode);
        TextView textViewCodigo = dialogView.findViewById(R.id.textViewCodigo);
        MaterialButton buttonCopiar = dialogView.findViewById(R.id.buttonCopiar);
        ImageView imageViewFechar = dialogView.findViewById(R.id.imageViewFechar);
        
        // Gerar QR Code
        try {
            Bitmap qrCodeBitmap = generateQRCode(userCode);
            imageViewQRCode.setImageBitmap(qrCodeBitmap);
        } catch (WriterException e) {
            Toast.makeText(this, "Erro ao gerar QR Code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Exibir código em texto
        textViewCodigo.setText(userCode);
        
        // Criar dialog
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true);
        
        androidx.appcompat.app.AlertDialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Botão copiar
        buttonCopiar.setOnClickListener(v -> {
            copyToClipboard(userCode);
            Toast.makeText(this, "Código copiado!", Toast.LENGTH_SHORT).show();
        });
        
        // Botão fechar
        imageViewFechar.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private Bitmap generateQRCode(String text) throws WriterException {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, 512, 512);
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        return barcodeEncoder.createBitmap(bitMatrix);
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Código SafeTrace", text);
        clipboard.setPrimaryClip(clip);
    }

    private void initializeViews() {
        editTextCodigo = findViewById(R.id.editTextCodigo);
        buttonSalvar = findViewById(R.id.buttonSalvar);
        buttonBordaGerarQr = findViewById(R.id.buttonBordaGerarQr);
        buttonBordaAdicionarQr = findViewById(R.id.buttonBordaAdicionarQr);
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
        
        // Marcar o item de contatos como selecionado
        navigationView.setCheckedItem(R.id.nav_contatos_confianca);
    }

    private void setupClickListeners() {
        buttonSalvar.setOnClickListener(v -> saveContact());
        
        // Botão gerar QR Code
        buttonBordaGerarQr.setOnClickListener(v -> {
            showQRCodeDialog();
        });
        
        // Botão adicionar contato por QR Code
        buttonBordaAdicionarQr.setOnClickListener(v -> {
            // Abrir tela de escanear QR Code
            Intent intent = new Intent(CadastroContatosActivity.this, ScanQRActivity.class);
            qrCodeLauncher.launch(intent);
        });

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

        codigo = codigo.replaceAll("\\s+", "");

        saveContactApi( codigo );
    }

    private void saveContactApi( String code )
    {
        APIService.getInstance(this).addContact(this, code, new APIService.APIServiceCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Toast.makeText(CadastroContatosActivity.this, "Contato adicionado com sucesso!", Toast.LENGTH_SHORT).show();
                editTextCodigo.setText("");
                loadContactsFromApi(); // Refresh the contact list
            }
            @Override
            public void onError(String error) {
                Toast.makeText(CadastroContatosActivity.this, "Erro ao adicionar contato: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadContactsFromApi() {
        if (layoutContatos == null || scrollViewContatos == null) return;
        
        // Limpar lista atual
        layoutContatos.removeAllViews();
        
        APIService.getInstance(this).getContacts(this, true, new APIService.APIServiceCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray data = response.getJSONArray("data");

                    // Exibir scroll view se houver contatos
                    if (data.length() > 0) {
                        scrollViewContatos.setVisibility(View.VISIBLE);

                        // Criar botões para cada contato da API, mostrando o NOME
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject contactJson = data.getJSONObject(i);
                            String nickname = contactJson.isNull("nickname") ? null : contactJson.getString("nickname");
                            JSONObject userJson = contactJson.optJSONObject("user");
                            String displayName;

                            // Priorizar nickname, depois nome do usuário
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
                    } else {
                        scrollViewContatos.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(CadastroContatosActivity.this, "Erro ao processar contatos", Toast.LENGTH_SHORT).show();
                    scrollViewContatos.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CadastroContatosActivity.this, "Erro ao carregar contatos: " + error, Toast.LENGTH_SHORT).show();
                scrollViewContatos.setVisibility(View.GONE);
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
        } else if (id == R.id.nav_historico) {
            // Navegar para a tela de histórico
            Intent intent = new Intent(this, HistoricoActivity.class);
            startActivity(intent);
            finish();
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
    
    @Override
    protected void onResume() {
        super.onResume();
        // Marcar o item correto do menu quando voltar para esta tela
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_contatos_confianca);
        }
    }
}
