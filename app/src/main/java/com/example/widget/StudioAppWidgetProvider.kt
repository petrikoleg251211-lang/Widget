package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.StudioDatabase
import com.example.data.StudioRepository
import com.example.engine.script.WidgetScriptEngine
import com.example.model.StudioJsonSerializer
import com.example.model.WidgetGridSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class StudioAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TAP = "com.example.widget.ACTION_WIDGET_TAP"
        const val ACTION_WIDGET_REFRESH = "com.example.widget.ACTION_WIDGET_REFRESH"
        const val EXTRA_PROJECT_ID = "extra_project_id"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, StudioAppWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            context.sendBroadcast(intent)
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                    val provider = ComponentName(context, StudioAppWidgetProvider::class.java)
                    val successCallback = PendingIntent.getActivity(
                        context,
                        101,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    return appWidgetManager.requestPinAppWidget(provider, null, successCallback)
                }
            }
            updateAllWidgets(context)
            return false
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderAndPushAll(context, appWidgetManager, appWidgetIds, triggerTap = false)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val act = intent.action ?: return
        if (act == ACTION_WIDGET_TAP || act == ACTION_WIDGET_REFRESH) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val mgr = AppWidgetManager.getInstance(context)
                    val ids = mgr.getAppWidgetIds(ComponentName(context, StudioAppWidgetProvider::class.java))
                    renderAndPushAll(context, mgr, ids, triggerTap = (act == ACTION_WIDGET_TAP))
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun renderAndPushAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        triggerTap: Boolean
    ) {
        val db = StudioDatabase.getInstance(context)
        val repo = StudioRepository(db.studioDao())
        repo.ensureSeedData()

        val activeProject = repo.getPinnedOrLatestProject() ?: return
        val telemetry = WidgetBitmapRenderer.readDeviceTelemetry(context)

        // Parse local storage map
        val storageMap = mutableMapOf<String, Any>()
        runCatching {
            val json = JSONObject(activeProject.localStorageJson)
            for (k in json.keys()) {
                storageMap[k] = json.get(k)
            }
        }

        val files = StudioJsonSerializer.filesFromJson(activeProject.filesJson)
        val entryScript = files.firstOrNull { it.isEntry }?.content ?: files.firstOrNull()?.content ?: ""
        val engine = WidgetScriptEngine()
        val execResult = engine.execute(
            script = entryScript,
            telemetry = telemetry,
            localStorage = storageMap,
            triggerTapEvent = triggerTap
        )

        if (triggerTap) {
            val updatedStorageJson = JSONObject().apply {
                for ((k, v) in storageMap) put(k, v)
            }.toString()
            repo.saveProject(activeProject.copy(localStorageJson = updatedStorageJson))
            repo.logEvent("INFO", "AndroidWidget", "Оброблено натискання на системний віджет '${activeProject.name}'")
        }

        val elementsToRender = execResult.elements.ifEmpty {
            StudioJsonSerializer.elementsFromJson(activeProject.elementsJson)
        }
        val gridSize = WidgetGridSize.fromCode(activeProject.gridSizeCode)

        val bitmap = WidgetBitmapRenderer.renderWidgetToBitmap(
            gridSize = gridSize,
            backgroundColorHex = activeProject.backgroundColor,
            cornerRadius = activeProject.cornerRadius,
            elements = elementsToRender,
            telemetry = telemetry
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_studio_host)
            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            val tapIntent = Intent(context, StudioAppWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TAP
                putExtra(EXTRA_PROJECT_ID, activeProject.id)
            }
            val tapPendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_canvas_image, tapPendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
