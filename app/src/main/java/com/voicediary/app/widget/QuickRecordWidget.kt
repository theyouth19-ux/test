package com.voicediary.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.voicediary.app.R
import com.voicediary.app.ui.MainActivity

class QuickRecordWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_TYPE_PREFIX = "widget_type_"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val type = getWidgetType(context, appWidgetId)
            val typeLabel = if (type == "diary") "일기 녹음" else "메모 녹음"
            val typeIcon = if (type == "diary") R.drawable.ic_widget_diary else R.drawable.ic_widget_memo

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("navigate_to", "record")
                putExtra("record_type", type)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_quick_record).apply {
                setTextViewText(R.id.widget_title, "Voice Diary")
                setTextViewText(R.id.widget_type_label, typeLabel)
                setImageViewResource(R.id.widget_icon, typeIcon)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun saveWidgetType(context: Context, appWidgetId: Int, type: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("$KEY_TYPE_PREFIX$appWidgetId", type)
                .apply()
        }

        fun getWidgetType(context: Context, appWidgetId: Int): String {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("$KEY_TYPE_PREFIX$appWidgetId", "diary") ?: "diary"
        }

        fun deleteWidgetType(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove("$KEY_TYPE_PREFIX$appWidgetId")
                .apply()
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deleteWidgetType(context, appWidgetId)
        }
    }
}
