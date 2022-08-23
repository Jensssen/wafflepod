package com.jensssen.wafflepod.classes

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.jensssen.wafflepod.Adapter.MessageAdapter

object MessageHandler {

    private var finalMessageList: ArrayList<Message> = ArrayList()
    private var fullMessageList: ArrayList<Message> = ArrayList()
    private var currentTrackUri: String = ""
    private val TAG = "MessageHandler"
    private var currentPlaybackPosition: Int = 0
    private var currentTrackLength: Long = 1000000
    private var currentlyVisualizedSection = -1


    fun populateFullMessageList(path: String, trackId: String, db:FirebaseFirestore, adapter: MessageAdapter) {
        if (currentTrackUri != trackId) {
            queryAllDocuments(path, db, adapter)
        }
        currentTrackUri = trackId
    }

    private fun queryAllDocuments(path: String, db: FirebaseFirestore, adapter: MessageAdapter) {
        db.collection(path)
            .get()
            .addOnSuccessListener { documents ->
                fullMessageList.clear()
                for (document in documents) {
                    fullMessageList.add(
                        Message(
                            date = "1.2.2022",
                            message = document.data["message"].toString(),
                            author = document.data["author"].toString(),
                            uri = document.data["uri"]?.toString(),
                            position = document.data["position"].toString().toInt()
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                currentlyVisualizedSection = -1
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    fun addMessageToList(index: Int, message: Message, adapter: MessageAdapter) {
        fullMessageList.add(index, message)
        finalMessageList.add(index, message)
        adapter.notifyDataSetChanged()
    }

    fun getFullMessageList(): ArrayList<Message> {
        return fullMessageList
    }

    fun getFinalMessageList(): ArrayList<Message> {
        return finalMessageList
    }

    fun setCurrentPlaybackPosition(currentPlaybackPosition: Int){
        this.currentPlaybackPosition = currentPlaybackPosition
    }
    fun getCurrentPlaybackPosition(): Int{
        return this.currentPlaybackPosition
    }

    fun setCurrentTrackLength(currentTrackLength: Long){
        this.currentTrackLength = currentTrackLength
    }

    fun getCurrentTrackLength(): Long{
        return this.currentTrackLength
    }

    fun updateFinalMessageList(sectionToVisualize: Int, leftBound: Int, rightBound: Int, adapter: MessageAdapter) {
        if (currentlyVisualizedSection != sectionToVisualize){
            Log.d(TAG, "sectionToVisualize ${currentlyVisualizedSection} - ${sectionToVisualize}")
            finalMessageList.clear()
            for (item in fullMessageList) {
                Log.d(TAG, "FULL_MESSAGE_LIST position: ${item.position}")
                val messagePosition = item.position
                if (messagePosition in leftBound until rightBound){
                    finalMessageList.add(item)
                    Log.d(TAG, "Added ${item.message} to Recyclerview")
                }
            }
            adapter.notifyDataSetChanged()
        }
        currentlyVisualizedSection = sectionToVisualize
    }
}