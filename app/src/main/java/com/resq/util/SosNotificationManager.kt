package com.resq.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object SosNotificationManager {
    private const val CHANNEL_ID = "resq_sos_channel"
    private const val CHANNEL_NAME = "ResQ Emergency SOS Alerts"
    private const val NOTIF_ID_SENT = 9001
    private const val NOTIF_ID_RECEIVED = 9002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority emergency SOS alerts with alarm sound"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showSosSentNotification(context: Context, packetId: String, locationText: String) {
        createNotificationChannel(context)
        triggerSoundAndVibration(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 CRITICAL SOS BROADCASTED")
            .setContentText("Emergency packet $packetId transmitting to nearby devices ($locationText)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_SENT, builder.build())
        }
    }

    @SuppressLint("MissingPermission")
    fun showSosReceivedNotification(context: Context, senderId: String, text: String, lat: Double, lng: Double) {
        createNotificationChannel(context)
        triggerSoundAndVibration(context)

        val locationFormatted = "%.4f, %.4f".format(lat, lng)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 EMERGENCY SOS RECEIVED")
            .setContentText("From $senderId at $locationFormatted: $text")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Emergency SOS from device $senderId\nLocation: $locationFormatted\nMessage: $text"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        runCatching {
            NotificationManagerCompat.from(context).notify((NOTIF_ID_RECEIVED + System.currentTimeMillis() % 1000).toInt(), builder.build())
        }
    }

    private fun triggerSoundAndVibration(context: Context) {
        // 1. Check for custom MP3 sound in res/raw/ (e.g. res/raw/siren.mp3 or res/raw/emergency_siren.mp3)
        val resId = context.resources.getIdentifier("siren", "raw", context.packageName)
            .takeIf { it != 0 }
            ?: context.resources.getIdentifier("emergency_siren", "raw", context.packageName).takeIf { it != 0 }

        if (resId != null && resId != 0) {
            runCatching {
                val mediaPlayer = android.media.MediaPlayer.create(context.applicationContext, resId)
                mediaPlayer?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    start()
                    setOnCompletionListener { release() }
                }
            }
        } else {
            // Fallback to system alarm sound if no MP3 is placed in res/raw yet
            runCatching {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
                ringtone?.play()
            }.onFailure {
                runCatching {
                    val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
                }
            }
        }

        // Vibration
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 400), -1)
            }
        }
    }
}
