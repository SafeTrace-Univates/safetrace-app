package com.example.safetrace.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.example.safetrace.R;
import com.example.safetrace.service.EmergenciaForegroundService;

public class EmergencyWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "EmergencyWidgetProvider";
    private static final String ACTION_WIDGET_EMERGENCY = "com.example.safetrace.ACTION_WIDGET_EMERGENCY";
    public static final String ACTION_EMERGENCY_STATUS = "com.example.safetrace.ACTION_EMERGENCY_STATUS";
    public static final String EXTRA_EMERGENCY_ACTIVE = "emergency_active";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_emergency);

            Intent intent = new Intent(context, EmergencyWidgetProvider.class);
            intent.setAction(ACTION_WIDGET_EMERGENCY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            views.setOnClickPendingIntent(R.id.widgetButtonEmergency, pendingIntent);

            // Ajustar texto conforme status salvo
            SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
            boolean active = prefs.getBoolean("emergency_active", false);
            views.setTextViewText(R.id.widgetButtonEmergency, active ? "Em andamento" : "Emergência");

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_WIDGET_EMERGENCY.equals(intent.getAction())) {
            try {
                SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
                boolean active = prefs.getBoolean("emergency_active", false);

                if (active) {
                    // Solicitar parada
                    Intent stopIntent = new Intent(context, EmergenciaForegroundService.class);
                    stopIntent.setAction("STOP_EMERGENCY");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(stopIntent);
                    } else {
                        context.startService(stopIntent);
                    }

                    // Atualizar estado local e UI do widget
                    prefs.edit().putBoolean("emergency_active", false).apply();
                    updateAllWidgets(context, false);
                    Toast.makeText(context, "Emergência finalizada", Toast.LENGTH_SHORT).show();
                } else {
                    String userId = prefs.getString("user_id", null);
                    String userName = prefs.getString("user_name", "Usuário");

                    if (userId == null || userId.isEmpty()) {
                        Toast.makeText(context, "Faça login para iniciar emergência", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "user_id ausente ao iniciar emergência pelo widget");
                        return;
                    }

                    Intent serviceIntent = new Intent(context, EmergenciaForegroundService.class);
                    serviceIntent.setAction("START_EMERGENCY");
                    serviceIntent.putExtra("usuario_id", userId);
                    serviceIntent.putExtra("usuario_nome", userName);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                    } else {
                        context.startService(serviceIntent);
                    }

                    // Atualiza estado local e UI do widget
                    prefs.edit().putBoolean("emergency_active", true).apply();
                    updateAllWidgets(context, true);
                    Toast.makeText(context, "Emergência iniciada", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao acionar emergência pelo widget", e);
                Toast.makeText(context, "Erro ao iniciar emergência", Toast.LENGTH_SHORT).show();
            }
        } else if (intent != null && ACTION_EMERGENCY_STATUS.equals(intent.getAction())) {
            boolean active = intent.getBooleanExtra(EXTRA_EMERGENCY_ACTIVE, false);
            // Persistir e atualizar widgets
            SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("emergency_active", active).apply();
            updateAllWidgets(context, active);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, android.os.Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        SharedPreferences prefs = context.getSharedPreferences("safetrace_prefs", Context.MODE_PRIVATE);
        boolean active = prefs.getBoolean("emergency_active", false);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_emergency);
        // Para 1x1 (aprox < 100dp), usar texto curto
        if (minWidth < 100 || minHeight < 60) {
            views.setTextViewText(R.id.widgetButtonEmergency, active ? "Ativo" : "SOS");
            views.setTextViewTextSize(R.id.widgetButtonEmergency, android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        } else {
            views.setTextViewText(R.id.widgetButtonEmergency, active ? "Em andamento" : "Emergência");
            views.setTextViewTextSize(R.id.widgetButtonEmergency, android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void updateAllWidgets(Context context, boolean active) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, EmergencyWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_emergency);
            views.setTextViewText(R.id.widgetButtonEmergency, active ? "Em andamento" : "Emergência");
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }
}


