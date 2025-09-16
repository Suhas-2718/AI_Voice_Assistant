package com.example.ai_voice_assistant

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var sendButton: ImageButton
    private val messages = mutableListOf<Message>()

    private lateinit var fabMic: FloatingActionButton
    private var isListening = false
    private var micAnim: AnimationDrawable? = null

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var messageInput: EditText
    private val client = OkHttpClient()
    private val flaskUrl = "http://192.168.0.172:5000/chat" // replace with your Flask IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        fabMic = findViewById(R.id.micButton)

        // mic button click
        fabMic.setOnClickListener {
            if (!isListening) {
                startListening()
                startMicAnimation()
            } else {
                stopListening()

                stopMicAnimation()
            }
        }

        adapter = MessageAdapter(messages)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // TTS setup
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // SpeechRecognizer setup
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // send button click
        sendButton.setOnClickListener {
            val text = messageInput.text.toString()
            if (text.isNotEmpty()) {
                addMessage("User", text)
                sendMessageToFlask(text)
                messageInput.text.clear()
            }
        }
    }



    // THE BELOW IS FUNCTION TO ADD MESSAGE TO RECYCLER VIEW



    private fun addMessage(sender: String, text: String) {
        messages.add(Message(sender, text))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }




    // THE BELOW IS FUNCTION TO SEND MESSAGE TO SERVER(FLASK)

    private fun sendMessageToFlask(userMessage: String) {
        val json = JSONObject()
        json.put("message", userMessage)

        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(flaskUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Backend not connected", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                println("Server raw response: $bodyString")  // 🔍 Debug print

                try {
                    val json = JSONObject(bodyString ?: "{}")
                    val botReply = json.optString("reply", "Sorry, I didn’t get that.")

                    runOnUiThread {
                        addMessage("Bot", botReply)
                        tts.speak(botReply, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Invalid server response", Toast.LENGTH_SHORT).show()
                    }
                    e.printStackTrace()
                }
            }
        })
    }


    // THE BELOW IS FUNCTION TO START LISTENING ON FABed

    private fun startListening() {
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                startMicAnimation()
            }

            override fun onBeginningOfSpeech() {
                isListening = true
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                stopMicAnimation()

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    messageInput.setText(spokenText)
                    addMessage("User", spokenText)
                    sendMessageToFlask(spokenText)
                }
            }

            override fun onEndOfSpeech() {
                isListening = false
                stopMicAnimation()
            }

            override fun onError(error: Int) {
                isListening = false
                stopMicAnimation()
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }




    // THE BELOW IS FUNCTION TO STOP LISTENING

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        stopMicAnimation()
    }



    // THE BELOW IS FUNCTION TO STOP SPEAKING





    // THE BELOW IS FUNCTION TO START ANIMATING

    private fun startMicAnimation() {
        fabMic.setImageResource(R.drawable.mic_animation) // animation-list drawable
        micAnim = fabMic.drawable as AnimationDrawable
        micAnim?.start()

        fabMic.animate().scaleX(1.9f).scaleY(1.9f).setDuration(350).start()
    }




    // THE BELOW IS FUNCTION TO STOP LISTENING

    private fun stopMicAnimation() {
        micAnim?.stop()
        fabMic.setImageResource(R.drawable.mic_1)
        fabMic.animate().scaleX(1f).scaleY(1f).setDuration(250).start()
    }


    // THE BELOW IS FUNCTION TO STOP LISTENING

    override fun onDestroy() {
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}