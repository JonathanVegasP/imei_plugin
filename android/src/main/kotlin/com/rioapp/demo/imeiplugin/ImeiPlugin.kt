package com.rioapp.demo.imeiplugin

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import androidx.core.app.ActivityCompat

import android.os.Build

import android.telephony.TelephonyManager

import android.content.pm.PackageManager

import androidx.core.content.ContextCompat
import android.content.SharedPreferences
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import java.util.*


/** ImeiPlugin */
class ImeiPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
    private lateinit var channel: MethodChannel
    private lateinit var activity: Activity
    private lateinit var result: Result
    private var ssrpr = false

    companion object {
        const val MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1995

        const val MY_PERMISSIONS_REQUEST_READ_PHONE_STATE_IMEI_MULTI = 1997

        const val PREF_UNIQUE_ID = "PREF_UNIQUE_ID_99599"

        const val ERCODE_PERMISSIONS_DENIED = "2000"
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "imei_plugin")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        this.result = result

        ssrpr = call.argument<Boolean>("ssrpr")!!

        when (call.method) {
            "getImei" -> getImei()

            "getImeiMulti" -> getImeiMulti()

            "getId" -> getID()

            else -> result.notImplemented()
        }
    }

    fun getUUID(): String {

        val sharedPrefs: SharedPreferences = activity.getSharedPreferences(
                PREF_UNIQUE_ID, Context.MODE_PRIVATE)

        var uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null)

        if (uniqueID == null) {
            uniqueID = UUID.randomUUID().toString()
            val editor = sharedPrefs.edit()
            editor.putString(PREF_UNIQUE_ID, uniqueID)
            editor.apply()
        }

        return uniqueID
    }

    private fun getImei() {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                result.success(getUUID())
            } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val telephonyManager = activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) result.success(telephonyManager.imei) else result.success(telephonyManager.deviceId)
            } else {
                if (ssrpr && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE)) result.error(ERCODE_PERMISSIONS_DENIED, "Permission Denied", null) else ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_PHONE_STATE), MY_PERMISSIONS_REQUEST_READ_PHONE_STATE)
            }
        } catch (ex: Exception) {
            result.error("get-imei", ex.message, ex)
        }
    }

    private fun getImeiMulti() {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                result.success(arrayListOf(getUUID()))
            } else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val telephonyManager = activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val phoneCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        telephonyManager.activeModemCount
                    } else {
                        telephonyManager.phoneCount
                    }
                    val imeis: ArrayList<String> = arrayListOf()
                    for (i in 0 until phoneCount) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) imeis.add(telephonyManager.getImei(i)) else imeis.add(telephonyManager.getDeviceId(i))
                    }
                    result.success(imeis)
                } else {
                    result.success(arrayListOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) telephonyManager.imei else telephonyManager.deviceId))

                }
            } else {
                if (ssrpr && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE)) result.error(ERCODE_PERMISSIONS_DENIED, "Permission Denied", null) else ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_PHONE_STATE), MY_PERMISSIONS_REQUEST_READ_PHONE_STATE_IMEI_MULTI)
            }
        } catch (ex: java.lang.Exception) {
            result.error("get-imei-multi", ex.message, ex)
        }
    }

    private fun getID() = result.success(getUUID())

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE || requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE_IMEI_MULTI) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE) {
                    getImei()
                } else {
                    getImeiMulti()
                }
            } else {
                result.error(ERCODE_PERMISSIONS_DENIED, "Permission Denied", null)
            }
            return true
        }

        return false
    }
}
