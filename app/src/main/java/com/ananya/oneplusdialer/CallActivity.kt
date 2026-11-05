package com.oneplus.dialer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telecom.Call
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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
            ivSpeakerIcon.backgroundTintList = ColorStateList.valueOf(if (isSpeakerOn) Color.WHITE else Color.parseColor("#455A64"))
        }

        // Mute Toggle
        btnMute.setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            ivMuteIcon.backgroundTintList = ColorStateList.valueOf(if (isMuted) Color.WHITE else Color.parseColor("#455A64"))
        }

        // Hold Toggle
        btnHold.setOnClickListener {
            if (call != null) {
                if (isHold) {
                    call.unhold()
                    isHold = false
                    ivHoldIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#455A64"))
                } else {
                    call.hold()
                    isHold = true
                    ivHoldIcon.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                }
            }
        }

        // Call Recording Button Toggle
        btnRecord.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                ivRecordIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EB0029"))
                tvRecordText.text = "Recording"
                Toast.makeText(this, "Recording Call...", Toast.LENGTH_SHORT).show()
            } else {
                ivRecordIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#455A64"))
                tvRecordText.text = "Record"
                Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show()
            }
        }

        // End Call
        btnEndCall.setOnClickListener {
            handler.removeCallbacks(timerRunnable)
            call?.disconnect()
            finish()
        }
    }

    private fun getContactName(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
