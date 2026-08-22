package com.zleeper.sleepapp.platform.sleep

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class PlayServicesSleepSignalSource @Inject constructor(@ApplicationContext private val context: Context) : SleepSignalSource {
    private val client by lazy { ActivityRecognition.getClient(context) }
    private val pendingIntent by lazy {
        PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, SleepEventReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    override suspend fun subscribe(): Result<Unit> {
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(SecurityException("Activity Recognition permission is required for automatic sleep estimation"))
        }
        return suspendCancellableCoroutine { continuation ->
            client.requestSleepSegmentUpdates(pendingIntent, SleepSegmentRequest.getDefaultSleepSegmentRequest())
                .addOnSuccessListener { continuation.resume(Result.success(Unit)) }
                .addOnFailureListener { continuation.resume(Result.failure(it)) }
        }
    }

    override suspend fun unsubscribe(): Result<Unit> {
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(SecurityException("Activity Recognition permission is not granted"))
        }
        return suspendCancellableCoroutine { continuation ->
            client.removeSleepSegmentUpdates(pendingIntent)
                .addOnSuccessListener { continuation.resume(Result.success(Unit)) }
                .addOnFailureListener { continuation.resume(Result.failure(it)) }
        }
    }

    override fun isAvailable(): Boolean = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    private companion object { const val REQUEST_CODE = 4107 }
}
