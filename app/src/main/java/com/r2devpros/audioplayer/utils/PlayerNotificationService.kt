@file:Suppress("DEPRECATION")

package com.r2devpros.audioplayer.utils

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.r2devpros.audioplayer.R

@Suppress("DEPRECATION")
class PlayerNotificationService : Service() {

    private lateinit var mPlayer: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var trackSelector: DefaultTrackSelector
    private var notificationId = 1000
    private var channelId = "com.r2devpros.audioPlayer.CHANNEL_01"
    private var uriFile: String? = ""

    private val binder = VideoServiceBinder()

    override fun onBind(intent: Intent?): IBinder {
        uriFile = intent?.getStringExtra("Url")
        playMedia()
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        trackSelector = DefaultTrackSelector(this)
        mPlayer = SimpleExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
        trackSelector.setParameters(
            trackSelector
                .buildUponParameters()
                .setMaxVideoBitrate(0)
        )
    }

    /**
     * This class will be what is returned when an activity binds to this service.
     * The activity will also use this to know what it can get from our service to know
     * about the video playback.
     */
    inner class VideoServiceBinder : Binder() {

        /**
         * This method should be used only for setting the exoplayer instance.
         * If exoplayer's internal are altered or accessed we can not guarantee
         * things will work correctly.
         */
        fun getExoPlayerInstance() = mPlayer
        fun getService(): PlayerNotificationService = this@PlayerNotificationService
    }


    /**
     * When called will load into exo player our sample playback video.
     */
    private fun playMedia() {
        val context = this

        uriFile?.let {
            val mediaItem: MediaItem = MediaItem.fromUri(it)
            mPlayer.setMediaItem(mediaItem)
        }
        mPlayer.playWhenReady = true
        mPlayer.prepare()

        mPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) {
                    stopForeground( /* removeNotification= */false)
                }
            }
        })

        val mediaDescriptor = object : PlayerNotificationManager.MediaDescriptionAdapter {

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                // return pending intent
                return null
            }

            //pass description here
            override fun getCurrentContentText(player: Player): String {
                return "ExoPlayer PIP example"
            }

            //pass title (mostly playing audio name)
            override fun getCurrentContentTitle(player: Player): String {
                return "ExoPlayer PIP example Video"
            }

            // pass image as bitmap
            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? {
                return BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background)
            }
        }

        val notificationListener = object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationPosted(
                notificationId: Int,
                notification: Notification,
                onGoing: Boolean
            ) {
                startForeground(notificationId, notification)
            }

            override fun onNotificationCancelled(
                notificationId: Int,
                dismissedByUser: Boolean
            ) {
                super.onNotificationCancelled(notificationId, dismissedByUser)
                stopSelf()
                context.sendBroadcast(Intent("closeActivity"))
            }
        }

        playerNotificationManager =
            PlayerNotificationManager.Builder(context, notificationId, channelId).apply {
                setChannelNameResourceId(R.string.app_name)
                setChannelDescriptionResourceId(R.string.channel_description)
                setMediaDescriptionAdapter(mediaDescriptor)
                setNotificationListener(notificationListener)
            }.build()

        //attach player to playerNotificationManager
        playerNotificationManager.setPlayer(mPlayer) // this line brings the player to the notification bar
        playerNotificationManager.setUseStopAction(true)
        playerNotificationManager.setUseFastForwardAction(true)
        playerNotificationManager.setUseRewindAction(true)
//        playerNotificationManager.setFastForwardIncrementMs(0)
//        playerNotificationManager.setRewindIncrementMs(0)
        playerNotificationManager.setSmallIcon(R.drawable.ic_launcher_background)
    }

    //removing service when user swipe out our app
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
}