package com.sam_chordas.android.stockhawk.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockTaskService;



public class AppWidgetStock extends AppWidgetProvider {
    static final String TAG = "PKT"; //AppWidgetStock.class.getSimpleName();

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        //Log.i(TAG, "AppWidgetStock -> updateAppWidget -> " + appWidgetId);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget_stock);

        Intent intent = new Intent(context, MyStocksActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.listView_widget_list_empty, pendingIntent);
        views.setOnClickPendingIntent(R.id.textView_widget_title, pendingIntent);

        Intent clickIntent = new Intent(context, MyChartActivity.class);
        PendingIntent clickPendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(clickIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setPendingIntentTemplate(R.id.listView_widget_list, clickPendingIntent);

        views.setRemoteAdapter(R.id.listView_widget_list, new Intent(context, AppWidgetStockService.class));
        views.setEmptyView(R.id.listView_widget_list, R.id.listView_widget_list_empty);


        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }




    static void partiallyUpdateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                         int appWidgetId)
    {
        //Log.i(TAG, "AppWidgetStock -> partiallyUpdateAppWidget -> " + appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget_stock);

        SharedPreferences cfg = PreferenceManager.getDefaultSharedPreferences(context);
        views.setTextViewText(R.id.textView_widget_title, cfg.getLong("LastUpdateTS", 0L) + "");

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if(StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listView_widget_list);


            //for(int appWidgetId: appWidgetIds) {
            //    partiallyUpdateAppWidget(context, appWidgetManager, appWidgetId);
            //}
        }

    }
}

