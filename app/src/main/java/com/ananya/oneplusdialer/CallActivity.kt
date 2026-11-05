package com.oneplus.dialer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.Call
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.IOException

class CallActivity : AppCompatActivity() {

    private lateinit var tvCallerName: TextView
    private lateinit var tvCallerNumber: TextView
    private lateinit var tvCallDuration: TextView
    private lateinit var btnMute: View
    private lateinit var btnSpeaker: View
    private lateinit var btnHold: View
    private lateinit var btnRecord: View
    private lateinit var btnEndCall: View

    private lateinit var ivRecordIcon: ImageView
    private lateinit var tvRecordText: TextView
    private lateinit var ivMuteIcon: ImageView
    private lateinit var ivSpeakerIcon: ImageView
    private lateinit var ivHoldIcon: ImageView

    private var isMuted = false
    private var isSpeakerOn = false
    private var isHold = false
    private var isRecording = false

    private var mediaRecorder: MediaRecorder? = null
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            seconds++
            val mins = seconds / 60
            val secs = seconds % 60
            tvCallDuration.text = String.format("%02d:%02d", mins, secs)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        tvCallerName = findViewById(R.id.tvCallerName)
        tvCallerNumber = findViewById(R.id.tvCallerNumber)
        tvCallDuration = findViewById(R.id.tvCallDuration)
        btnMute = findViewById(R.id.btnMute)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnHold = findViewById(R.id.btnHold)
        btnRecord = findViewById(R.id.btnRecord)
        btnEndCall = findViewById(R.id.btnEndCall)

        ivRecordIcon = findViewById(R.id.ivRecordIcon)
        tvRecordText = findViewById(R.id.tvRecordText)
        ivMuteIcon = findViewById(R.id.ivMuteIcon)
        ivSpeakerIcon = findViewById(R.id.ivSpeakerIcon)
        ivHoldIcon = findViewById(R.id.ivHoldIcon)

        val call = CallService.currentCall
        val number = call?.details?.handle?.schemeSpecificPart ?: "Unknown"
        tvCallerNumber.text = "Mobile, $number"
        tvCallerName.text = getContactName(number) ?: number

        handler.post(timerRunnable)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Speaker Toggle
        btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            audioManager.isSpeakerphoneOn = isSpeakerOn
            ivSpeakerIcon.backgroundTintList = ColorStateList.valueOf(if (isSpeakerOn) Color.WHITE else Color.parseColor("#556265"))
            ivSpeakerIcon.setColorFilter(if (isSpeakerOn) Color.BLACK else Color.WHITE)
        }

        // Mute Toggle
        btnMute.setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            ivMuteIcon.backgroundTintList = ColorStateList.valueOf(if (isMuted) Color.WHITE else Color.parseColor("#556265"))
            ivMuteIcon.setColorFilter(if (isMuted) Color.BLACK else Color.WHITE)
        }

        // Hold Toggle
        btnHold.setOnClickListener {
            if (call != null) {
                if (isHold) {
                    call.unhold()
                    isHold = false
                    ivHoldIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#556265"))
                } else {
                    call.hold()
                    isHold = true
                    ivHoldIcon.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                }
            }
        }

        // Call Recording Toggle
        btnRecord.setOnClickListener {
            if (!isRecording) {
                startRecording(number)
            } else {
                stopRecording()
            }
        }

        // End Call
        btnEndCall.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
            handler.removeCallbacks(timerRunnable)
            call?.disconnect()
            finish()
        }
    }

    private fun startRecording(number: String) {
        try {
            val recordDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings")
            if (!recordDir.exists()) recordDir.mkdirs()
            val file = File(recordDir, "Call_${number}_${System.currentTimeMillis()}.m4a")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            ivRecordIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EB0029"))
            tvRecordText.text = "Recording"
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            ivRecordIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#556265"))
            tvRecordText.text = "Record"
            Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            stopRecording()
        }
        handler.removeCallbacks(timerRunnable)
    }
}
