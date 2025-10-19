package com.example.safetrace;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class AudioListActivity extends AppCompatActivity {

    private ListView listView;
    private final List<File> audioFiles = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_list);

        listView = findViewById(R.id.listViewAudios);

        loadFiles();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                buildDisplayList()
        );

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= audioFiles.size()) return;
                File file = audioFiles.get(position);
                try {
                    Uri uri = androidx.core.content.FileProvider.getUriForFile(
                            AudioListActivity.this,
                            "com.example.safetrace.fileprovider",
                            file
                    );
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "audio/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Abrir áudio"));
                } catch (Exception e) {
                    Toast.makeText(AudioListActivity.this, "Não foi possível abrir o áudio", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFiles();
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
        adapter.clear();
        adapter.addAll(buildDisplayList());
        adapter.notifyDataSetChanged();
    }

    private void loadFiles() {
        audioFiles.clear();
        File dir = new File(getExternalFilesDir(null), "audios");
        File[] list = dir.listFiles();
        if (list == null || list.length == 0) return;

        // Sort by last modified desc
        Arrays.sort(list, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.compare(o2.lastModified(), o1.lastModified());
            }
        });
        Collections.addAll(audioFiles, list);
    }

    private List<String> buildDisplayList() {
        List<String> names = new ArrayList<>();
        DateFormat df = android.text.format.DateFormat.getMediumDateFormat(this);
        DateFormat tf = android.text.format.DateFormat.getTimeFormat(this);
        for (File f : audioFiles) {
            String when = df.format(new Date(f.lastModified())) + " " + tf.format(new Date(f.lastModified()));
            String label = f.getName() + "\n" + when + "  •  " + readableSize(f.length());
            names.add(label);
        }
        if (names.isEmpty()) {
            names.add("Nenhuma gravação encontrada");
        }
        return names;
    }

    private String readableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp - 1) + "";
        return String.format(java.util.Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}


