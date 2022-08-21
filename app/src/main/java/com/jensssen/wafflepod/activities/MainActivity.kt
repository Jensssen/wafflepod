package com.jensssen.wafflepod.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
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
import com.spotify.protocol.types.PlayerState
import kotlin.collections.ArrayList

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
    private val errorCallback = { throwable: Throwable -> logError(throwable) }
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Initialize Firebase Auth
        auth = Firebase.auth

        trackProgressBar =
            TrackProgressBar(binding.progressbar) { seekToPosition: Long -> seekTo(seekToPosition) }

        messageList = ArrayList()
        adapter = MessageAdapter(this, messageList)
        binding.messageBox.layoutManager = LinearLayoutManager(this)
        binding.messageBox.adapter = adapter
    }

    @SuppressLint("SimpleDateFormat")
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.logout){
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
        updateSeekbar(playerState)

        db.collection("messages/${playerState.track.uri}/messages")
            .whereGreaterThan("position", 10)
            .get()
            .addOnSuccessListener { documents ->
                messageList.clear()
                for (document in documents) {
//                    Log.d(TAG, "${document.id} => ${document.data}")
                    messageList.add(Message("1.2.2022", document.data["message"]?.toString(), document.data["username"]?.toString()))
                    Log.d(TAG, document.data["username"].toString())
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
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