@file:Suppress("DEPRECATION")

package com.r2devpros.audioplayer.presentation.home

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.r2devpros.audioplayer.R
import com.r2devpros.audioplayer.databinding.ActivityMainBinding
import com.r2devpros.audioplayer.utils.FilesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var layout: ActivityMainBinding
    private lateinit var audiosAdapter: AudiosRVAdapter
    private val requestPermissionCode = 100
    private var audios = arrayListOf<DocumentFile>()
    private val exoPlayer by lazy { ExoPlayer.Builder(this).build() }

    //region NOTIFICATION
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private var notificationId = 1000
    private var channelId = "com.r2devpros.audioPlayer.CHANNEL_01"
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("MainActivity_TAG: onCreate: ")
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main)
        layout.lifecycleOwner = this
        initRecyclerView()
        validatePermissions()
    }

    private fun initRecyclerView() {
        Timber.d("MainActivity_TAG: initRecyclerView: ")
        audiosAdapter = AudiosRVAdapter { item ->
            Timber.d("MainActivity_TAG: onAudioClicked: ${item.name}")
            val mediaItem = item.file?.uri?.let { MediaItem.fromUri(it) }
            if (mediaItem != null) {
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()

                convertAudios()
            }
        }

        layout.rvAudioFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            adapter = audiosAdapter
        }
    }

    //region NOTIFICATION
    private fun convertAudios() {
        Timber.d("MainActivity_TAG: convertAudios: ")
        val aud = audios.sortedBy { it.name }
        val mediaItems = aud.map { file ->
            MediaItem.fromUri(file.uri)
        }

        exoPlayer.addMediaItems(mediaItems)
        exoPlayer.playWhenReady = true

//        createMediaSession()

        exoPlayer.prepare()
        exoPlayer.play()
    }

//    private fun createMediaSession() {
//        Timber.d("MainActivity_TAG: createMediaSession: ")
//       val mediaSession = MediaSession.Builder(this, exoPlayer).build()
//        mediaSession.player = exoPlayer
//    }

    @OptIn(UnstableApi::class)
    private fun initNotification() {
        Timber.d("MainActivity_TAG: initNotification: ")
        validateNotificationPermission()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Audio Player Channel",
                    NotificationManager.IMPORTANCE_LOW
                )

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }

            val mediaDescriptionAdapter =
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        return null
                    }

                    override fun getCurrentContentText(player: Player): String {
                        return "SONG NAME"
                    }

                    override fun getCurrentContentTitle(player: Player): String {
                        return getString(R.string.app_name)
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ): Bitmap? {
                        // Customize the large icon for the notification
                        // Example: load an icon from resources
//                return BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                        return null
                    }
                }

            playerNotificationManager = PlayerNotificationManager.Builder(
                this,
                notificationId,
                channelId,
                mediaDescriptionAdapter
            ).build()

            playerNotificationManager.setPlayer(exoPlayer)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }
    //endregion

    //region PERMISSIONS VALIDATION
    private fun validateNotificationPermission() {
        Timber.d("MainActivity_TAG: validateNotificationPermission: ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(this, "YOU CAN SHOW NOTIFICATION", Toast.LENGTH_LONG).show()
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Snackbar.make(
                        layout.containerMain,
                        "Notification blocked",
                        Snackbar.LENGTH_LONG
                    ).setAction("Settings") {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val uri: Uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }.show()
                }

                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

            }

        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT)
                .show()
        } else {
//            Toast.makeText(
//                this, "${getString(R.string.app_name)} can't post notifications without Notification permission",
//                Toast.LENGTH_LONG
//            ).show()

            Snackbar.make(
                layout.containerMain,
                String.format(
                    String.format(
                        getString(R.string.txt_error_post_notification),
                        getString(R.string.app_name)
                    )
                ),
                Snackbar.LENGTH_INDEFINITE
            ).setAction(getString(R.string.goto_settings)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val settingsIntent: Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    startActivity(settingsIntent)
                }
            }.show()
        }
    }

    private fun validatePermissions() {
        Timber.d("MainActivity_TAG: validatePermissions: ")
        val permission =
            if (Build.VERSION.SDK_INT > 32) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

        val permissionGranted = ContextCompat.checkSelfPermission(
            applicationContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            if (Build.VERSION.SDK_INT > 32)
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            else
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestPermissionCode)
        } else
            selectFolder()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted)
                selectFolder()
            else
                Toast.makeText(this, "PERMISSION DENIED", Toast.LENGTH_LONG).show()

        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Timber.d("MainActivity_TAG: onRequestPermissionsResult: ")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionCode) {
            if (grantResults.isNotEmpty()) {
                val permissionGranted = grantResults.all { result ->
                    result == PackageManager.PERMISSION_GRANTED
                }
                if (permissionGranted)
                    selectFolder()
                else {
                    Toast.makeText(this, "PERMISSIONS ARE REQUIRED", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    //endregion

    //region RETRIEVE AUDIOS
    private fun selectFolder() {
        Timber.d("MainActivity_TAG: selectFolder: ")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        selectFolderLauncher.launch(intent)
    }

    private val selectFolderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    layout.pbLoading.visibility = View.VISIBLE
                    result.data?.data?.let { uri ->
                        val folder = DocumentFile.fromTreeUri(this, uri)
                        CoroutineScope(Dispatchers.IO).launch {
                            importAudioFiles(folder)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d("SelectFolderLauncher: ERROR ${e.message}")
                layout.pbLoading.visibility = View.GONE
            }
        }

    private fun importAudioFiles(folder: DocumentFile?) {
        Timber.d("MainActivity_TAG: importAudioFiles: ")
        if (folder != null && folder.isDirectory)
            recursiveImport(folder)
        else {
            Timber.d("MainActivity_TAG: importAudioFiles: ERROR: Invalid folder selection")
            layout.pbLoading.visibility = View.GONE
        }
    }

    private fun recursiveImport(folder: DocumentFile) {
        Timber.d("MainActivity_TAG: recursiveImport: ")
        try {
            addAudioToList(folder)

            val files = folder.listFiles()

            if (files.any { it.isDirectory })
                files.filter { it.isDirectory }.forEach { directory ->
                    addAudioToList(directory)
                }
        } catch (e: Exception) {
            Timber.d("MainActivity_TAG: recursiveImport: ERROR ${e.message}")
            layout.pbLoading.visibility = View.GONE
        }

        runOnUiThread {
            val items = audios.map {
                AudioItemViewModel().apply {
                    file = it
                    duration = FilesHelper.getAudioDuration(this@MainActivity, it)
                    author = FilesHelper.getAudioAuthor(this@MainActivity, it)
                    imageCover = FilesHelper.getAudioArt(this@MainActivity, it)
                }
            }.sortedBy { it.name }
            audiosAdapter.itemList = items
            layout.pbLoading.visibility = View.GONE
//            createMediaSession()
            initNotification()
        }
    }

    private fun addAudioToList(folder: DocumentFile) {
        Timber.d("MainActivity_TAG: addAudioToList: ")
        try {
            val files = folder.listFiles()
            files.filter { it.type?.startsWith("audio/") == true }
                .forEach { audioFile ->
                    audios.add(audioFile)
                }
        } catch (e: Exception) {
            Timber.d("MainActivity_TAG: addAudioToList: ERROR ${e.message}")
            layout.pbLoading.visibility = View.GONE
        }
    }
    //endregion RETRIEVE AUDIOS
}