package com.glip.driver.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.glip.driver.ui.crash.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ErrorReport(
    val errorType: String,
    val errorMessage: String,
    val stackTrace: String,
    val timestamp: String,
    val deviceInfo: String
)

object CrashManager {

    private const val PREFS_NAME = "glip_error_reports"
    private const val KEY_ERROR_TYPE = "last_error_type"
    private const val KEY_ERROR_MSG = "last_error_msg"
    private const val KEY_STACK_TRACE = "last_stack_trace"
    private const val KEY_TIMESTAMP = "last_error_time"
    private const val KEY_DEVICE_INFO = "last_device_info"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleException(context, throwable)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun handleException(context: Context, throwable: Throwable) {
        val report = createReport(throwable)
        saveReport(context, report)

        val intent = Intent(context, CrashActivity::class.java).apply {
            putExtra(CrashActivity.EXTRA_ERROR_TYPE, report.errorType)
            putExtra(CrashActivity.EXTRA_ERROR_MESSAGE, report.errorMessage)
            putExtra(CrashActivity.EXTRA_STACK_TRACE, report.stackTrace)
            putExtra(CrashActivity.EXTRA_TIMESTAMP, report.timestamp)
            putExtra(CrashActivity.EXTRA_DEVICE_INFO, report.deviceInfo)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }

    fun createReport(throwable: Throwable): ErrorReport {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        val timestamp = timeFormat.format(Date())

        val deviceInfo = buildString {
            append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
            append("Android OS: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("Brand: ").append(Build.BRAND).append("\n")
            append("Product: ").append(Build.PRODUCT)
        }

        val type = throwable.javaClass.simpleName.ifBlank { "Exception" }
        val msg = throwable.localizedMessage ?: throwable.message ?: "No error message provided"

        return ErrorReport(
            errorType = type,
            errorMessage = msg,
            stackTrace = stackTrace,
            timestamp = timestamp,
            deviceInfo = deviceInfo
        )
    }

    fun saveReport(context: Context, report: ErrorReport) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ERROR_TYPE, report.errorType)
            .putString(KEY_ERROR_MSG, report.errorMessage)
            .putString(KEY_STACK_TRACE, report.stackTrace)
            .putString(KEY_TIMESTAMP, report.timestamp)
            .putString(KEY_DEVICE_INFO, report.deviceInfo)
            .commit()
    }

    fun getLastError(context: Context): ErrorReport? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val type = prefs.getString(KEY_ERROR_TYPE, null) ?: return null
        val msg = prefs.getString(KEY_ERROR_MSG, "") ?: ""
        val stack = prefs.getString(KEY_STACK_TRACE, "") ?: ""
        val time = prefs.getString(KEY_TIMESTAMP, "") ?: ""
        val device = prefs.getString(KEY_DEVICE_INFO, "") ?: ""

        return ErrorReport(
            errorType = type,
            errorMessage = msg,
            stackTrace = stack,
            timestamp = time,
            deviceInfo = device
        )
    }

    fun clearError(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun simulateTestCrash(context: Context) {
        val testException = RuntimeException(
            "Test Error Triggered: In-App Error Diagnostics verification successful!"
        )
        handleException(context, testException)
    }
}
