package com.example.spotifyclone.data.remote

import android.util.Log
import com.example.spotifyclone.utils.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import com.example.spotifyclone.data.entities.Song
import kotlinx.coroutines.tasks.await

class MusicDatabase {
    val TAG = "MusicDatabase"
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    suspend fun getAllSongs(): List<Song> {
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (ex: Exception) {
            Log.e(TAG, "getAllSongs: ", ex)
            emptyList()
        }
    }
}