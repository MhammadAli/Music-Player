package com.example.spotifyclone.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.RequestManager
import com.example.spotifyclone.R
import com.example.spotifyclone.adapters.SwipeSongAdapter
import com.example.spotifyclone.data.entities.Song
import com.example.spotifyclone.exoplayer.isPlaying
import com.example.spotifyclone.exoplayer.toSong
import com.example.spotifyclone.ui.viewmodels.MainViewModel
import com.example.spotifyclone.utils.Status
import com.example.spotifyclone.utils.Status.*
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val mainViewModel: MainViewModel by viewModels() // here we bind the mainViewModel with the activity lifecycle

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private var curPlayingSong: Song? = null
    private var playbackState: PlaybackStateCompat? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        subscribeToObservers()
        vpSong.adapter = swipeSongAdapter
        vpSong.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (playbackState?.isPlaying == true) {
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                } else {
                    curPlayingSong = swipeSongAdapter.songs[position]
                }
            }
        })

        ivPlayPause.setOnClickListener {
            curPlayingSong?.let {
                mainViewModel.playOrToggleSong(it, true)
            }
        }
        swipeSongAdapter.setItemClickListener {

            navHostFragment.findNavController().navigate(
                R.id.globalActionToSongFragment
            )
//            Log.d("gg", "onCreate1: ${navHostFragment.findNavController().currentDestination}")
//            hideBottomBar()

        }
        //// we should put and addOnChangedListener here
        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (navHostFragment.findNavController().currentDestination?.id) {
                R.id.songFragment -> hideBottomBar()
                R.id.homeFragment -> showBottomBar()
                else -> showBottomBar()
            }
            Log.d("gg", "onCreate2: ${navHostFragment.findNavController().currentDestination}")
        }

    }

    private fun hideBottomBar() {
        ivCurSongImage.isVisible = false
        vpSong.isVisible = false
        ivPlayPause.isVisible = false
    }

    private fun showBottomBar() {
        ivCurSongImage.isVisible = true
        vpSong.isVisible = true
        ivPlayPause.isVisible = true
    }

    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != -1) {
            vpSong.currentItem = newItemIndex
            curPlayingSong = song
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItem.observe(this) { result ->
            when (result.status) {
                SUCCESS -> {
                    result.data?.let { songs ->
                        swipeSongAdapter.songs = songs
                        if (songs.isNotEmpty()) {
                            glide.load(curPlayingSong ?: songs[0].imageUrl).into(ivCurSongImage)
                        }
                        switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
                    }
                }
                ERROR -> Unit
                LOADING -> Unit
            }
        }
        mainViewModel.curPlayingSong.observe(this) {
            if (it == null) return@observe
            curPlayingSong = it.toSong()
            glide.load(curPlayingSong?.imageUrl).into(ivCurSongImage)
            switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
        }
        mainViewModel.playbackState.observe(this) {
            playbackState = it
            ivPlayPause.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
        mainViewModel.isConnected.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    ERROR -> Snackbar.make(
                        rootLayout,
                        result.message ?: "An unknown error occurred",
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }

        mainViewModel.networkError.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    ERROR -> Snackbar.make(
                        rootLayout,
                        result.message ?: "An unknown error occurred",
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
    }
}