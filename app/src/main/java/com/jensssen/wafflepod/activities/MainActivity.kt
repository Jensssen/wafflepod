package com.jensssen.wafflepod.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import com.jensssen.wafflepod.Adapter.MessageAdapter
import com.jensssen.wafflepod.R
import com.jensssen.wafflepod.classes.Message
import com.jensssen.wafflepod.classes.TrackProgressBar
import com.jensssen.wafflepod.databinding.ActivityMainBinding
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.error.SpotifyDisconnectedException
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.Image
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerState


class MainActivity : AppCompatActivity() {

    // Spotify
    private val clientId = "d87686a9fa4144799f5751c5e90598e3"
    private val redirectUri = "wafflepod-login://callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    // Firebase
    private lateinit var auth: FirebaseAuth

    // Message Box
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageList: ArrayList<Message>
    private lateinit var adapter: MessageAdapter

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val TAG = "MainActivityyyy"
    private lateinit var trackProgressBar: TrackProgressBar
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private lateinit var currentPlayerState: PlayerState
    private val errorCallback = { throwable: Throwable -> logError(throwable) }
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val db = Firebase.firestore
    private lateinit var name: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Initialize Firebase Auth
        auth = Firebase.auth

        getUserNameByUID("users", auth.uid.toString())

        trackProgressBar =
            TrackProgressBar(binding.progressbar) { seekToPosition: Long -> seekTo(seekToPosition) }

        messageList = ArrayList()
        adapter = MessageAdapter(this, messageList)
        binding.messageBox.layoutManager = LinearLayoutManager(this)
        binding.messageBox.adapter = adapter
}

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) to Firebase and update UI accordingly.
        if (auth.currentUser == null) {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }

        // Check if user is signed in (non-null) to Spotify and update UI accordingly.
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("MainActivity", "Connected! Yay!")
                // Now you can start interacting with App Remote
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", throwable.message, throwable)
                // Something went wrong when attempting to connect! Handle errors here
            }
        })

        binding.btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString()
        if (!TextUtils.isEmpty(message)) {
            val newMessage = Message(
                date = "1.2.22",
                message = binding.etMessage.text.toString(),
                author = name,
                uri = auth.uid.toString(),
                position = currentPlayerState.playbackPosition.toInt()/1000
            )
            uploadUserToDb(newMessage)
            binding.etMessage.clearFocus()
            binding.etMessage.setText("")
            closeKeyBoard(binding.etMessage)
            messageList.add(0, newMessage)
            adapter.notifyDataSetChanged()

        } else {
            Toast.makeText(
                baseContext,
                "Message is empty!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun closeKeyBoard(view: View) {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }


    private fun uploadUserToDb(message: Message) {
        // Add a new user to DB
        db.collection("messages/${currentPlayerState.track.uri}/messages").document()
            .set(message)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: $documentReference")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding user to db", e)
                Toast.makeText(
                    baseContext,
                    e.message.toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            finish()
            startActivity(intent)
        }
        return true
    }

    private fun connected() {
//        spotifyAppRemote?.let {
//            // Play a playlist
//            val playlistURI = "spotify:playlist:37i9dQZF1DX2sUQwD7tbmL"
//            it.playerApi.play(playlistURI)
//            // Subscribe to PlayerState
//            it.playerApi.subscribeToPlayerState().setEventCallback {
//                val track: Track = it.track
//                Log.d("MainActivity", track.name + " by " + track.artist.name)
//            }
//        }

        playerStateSubscription = cancelAndResetSubscription(playerStateSubscription)
        playerStateSubscription = assertAppRemoteConnected()
            .playerApi
            .subscribeToPlayerState()
            .setEventCallback(playerStateEventCallback)
            .setLifecycleCallback(
                object : Subscription.LifecycleCallback {
                    override fun onStart() {
                        Log.d("MainActivity", "Event: start")
                    }

                    override fun onStop() {
                        Log.d("MainActivity", "Event: end")
                    }
                })
            .setErrorCallback {} as Subscription<PlayerState>
    }

    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    private fun <T : Any?> cancelAndResetSubscription(subscription: Subscription<T>?): Subscription<T>? {
        return subscription?.let {
            if (!it.isCanceled) {
                it.cancel()
            }
            null
        }
    }

    private fun assertAppRemoteConnected(): SpotifyAppRemote {
        spotifyAppRemote?.let {
            if (it.isConnected) {
                return it
            }
        }
        Log.e(TAG, getString(R.string.err_spotify_disconnected))
        throw SpotifyDisconnectedException()
    }


    private val playerStateEventCallback = Subscription.EventCallback<PlayerState> { playerState ->
        Log.v(TAG, String.format("Player State: %s", gson.toJson(playerState)))
        currentPlayerState = playerState
        updateSeekbar(playerState)

        val imageUri = ImageUri(playerState.track.imageUri.raw)

        spotifyAppRemote
            ?.imagesApi
            ?.getImage(imageUri, Image.Dimension.THUMBNAIL)
            ?.setResultCallback { bitmap: Bitmap? -> binding.imgTrack.setImageBitmap(bitmap) }

        getMultipleDocumentsByQuery("messages/${playerState.track.uri}/messages")
    }

    private fun getMultipleDocumentsByQuery(path: String) {

        db.collection(path)
            .whereGreaterThan("position", 10)
            .get()
            .addOnSuccessListener { documents ->
                messageList.clear()
                for (document in documents) {
                    messageList.add(
                        Message(
                            date = "1.2.2022",
                            message = document.data["message"]?.toString(),
                            author = document.data["author"]?.toString(),
                            uri = document.data["uri"]?.toString(),
                            position = document.data["position"].toString().toInt()
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

    }

    private fun getUserNameByUID(path: String, uid: String) {
        val docRef: DocumentReference = db.collection(path).document(uid)

        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: " + task.result.data)
                    name = document.data?.get("name").toString()

                } else {
                    Log.d(TAG, "No such document")
                }
            } else {
                Log.d(TAG, "get failed with ", task.exception)
            }
        }
    }


    private fun logError(throwable: Throwable) {
        Toast.makeText(this, R.string.err_generic_toast, Toast.LENGTH_SHORT).show()
        Log.e(TAG, "", throwable)
    }

    private fun updateSeekbar(playerState: PlayerState) {
        trackProgressBar.apply {
            if (playerState.playbackSpeed > 0) {
                unpause()
            } else {
                pause()
            }
            // Invalidate seekbar length and position
            binding.progressbar.max = playerState.track.duration.toInt()
            binding.progressbar.isEnabled = true
            setDuration(playerState.track.duration)
            update(playerState.playbackPosition)
        }
    }

    private fun seekTo(seekToPosition: Long) {
        assertAppRemoteConnected()
            .playerApi
            .seekTo(seekToPosition)
            .setErrorCallback(errorCallback)
    }
}