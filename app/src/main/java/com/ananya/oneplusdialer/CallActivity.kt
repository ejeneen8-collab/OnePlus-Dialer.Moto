package com.ananya.oneplusdialer

import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.telecom.Call
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class CallActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false

    private lateinit var tvStatus: TextView
    private lateinit var tvNumber: TextView
    private lateinit var btnAnswer: Button
    private lateinit var btnHangup: Button
    private lateinit var btnRecord: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        tvStatus = findViewById(R.id.tvStatus)
        tvNumber = findViewById(R.id.tvNumber)
        btnAnswer = findViewById(R.id.btnAnswer)
        btnHangup = findViewById(R.id.btnHangup)
        btnRecord = findViewById(R.id.btnRecord)

        updateCallState(CallService.currentCall?.state ?: Call.STATE_DISCONNECTED)

        CallService.currentCall?.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call?, state: Int) {
                super.onStateChanged(call, state)
                runOnUiThread { updateCallState(state) }
            }
        })

        btnAnswer.setOnClickListener {
            CallService.currentCall?.answer(0)
        }

        btnHangup.setOnClickListener {
            stopRecording()
            CallService.currentCall?.disconnect()
            finish()
        }

        btnRecord.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }
    }

    private fun updateCallState(state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                tvStatus.text = "Incoming Call"
                btnAnswer.visibility = View.VISIBLE
                btnRecord.visibility = View.GONE
            }
            Call.STATE_ACTIVE -> {
                tvStatus.text = "Call Active"
                btnAnswer.visibility = View.GONE
                btnRecord.visibility = View.VISIBLE
            }
            Call.STATE_DIALING -> {
                tvStatus.text = "Dialing..."
                btnAnswer.visibility = View.GONE
                btnRecord.visibility = View.GONE
            }
            Call.STATE_DISCONNECTED -> {
                stopRecording()
                finish()
            }
        }
    }

    private fun startRecording() {
        try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), "OnePlusRecordings")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "Call_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            btnRecord.text = "Stop Recording"
            Toast.makeText(this, "Call recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
                btnRecord.text = "Record"
                Toast.makeText(this, "Recording saved in Recordings/OnePlusRecordings", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
}
