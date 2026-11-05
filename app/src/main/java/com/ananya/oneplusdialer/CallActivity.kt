package com.oneplus.dialer

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CallActivity : AppCompatActivity() {

    private lateinit var tvCallerName: TextView
    private lateinit var tvCallDuration: TextView
    private lateinit var btnMute: View
    private lateinit var btnSpeaker: View
    private lateinit var btnHold: View
    private lateinit var btnEndCall: FloatingActionButton
    
    private lateinit var ivMuteIcon: ImageView
    private lateinit var ivSpeakerIcon: ImageView
    private lateinit var ivHoldIcon: ImageView

    private var isMuted = false
    private var isSpeakerOn = false
    private var isHold = false

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
        tvCallDuration = findViewById(R.id.tvCallDuration)
        btnMute = findViewById(R.id.btnMute)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnHold = findViewById(R.id.btnHold)
        btnEndCall = findViewById(R.id.btnEndCall)

        ivMuteIcon = findViewById(R.id.ivMuteIcon)
        ivSpeakerIcon = findViewById(R.id.ivSpeakerIcon)
        ivHoldIcon = findViewById(R.id.ivHoldIcon)

        // Read Call info from CallService
        val call = CallService.currentCall
        val number = call?.details?.handle?.schemeSpecificPart ?: "Unknown Caller"
        tvCallerName.text = number

        // Start Call Timer
        handler.post(timerRunnable)

        // Speaker Toggle
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            audioManager.isSpeakerphoneOn = isSpeakerOn
            if (isSpeakerOn) {
                ivSpeakerIcon.backgroundTintList = getColorStateList(android.R.color.white)
                ivSpeakerIcon.setColorFilter(getColor(android.R.color.black))
            } else {
                ivSpeakerIcon.backgroundTintList = getColorStateList(android.R.color.darker_gray)
                ivSpeakerIcon.setColorFilter(getColor(android.R.color.white))
            }
        }

        // Mute Toggle
        btnMute.setOnClickListener {
            isMuted = !isMuted
            audioManager.isMicrophoneMute = isMuted
            if (isMuted) {
                ivMuteIcon.backgroundTintList = getColorStateList(android.R.color.white)
                ivMuteIcon.setColorFilter(getColor(android.R.color.black))
            } else {
                ivMuteIcon.backgroundTintList = getColorStateList(android.R.color.darker_gray)
                ivMuteIcon.setColorFilter(getColor(android.R.color.white))
            }
        }

        // Hold Toggle
        btnHold.setOnClickListener {
            if (call != null) {
                if (isHold) {
                    call.unhold()
                    isHold = false
                    ivHoldIcon.backgroundTintList = getColorStateList(android.R.color.darker_gray)
                } else {
                    call.hold()
                    isHold = true
                    ivHoldIcon.backgroundTintList = getColorStateList(android.R.color.white)
                }
            }
        }

        // End Call
        btnEndCall.setOnClickListener {
            handler.removeCallbacks(timerRunnable)
            if (call != null) {
                call.disconnect()
            }
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
    }
}
