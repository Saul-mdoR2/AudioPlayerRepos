package com.r2devpros.audioplayer.presentation.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("MainActivity_TAG: onCreate: ")
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main)
        layout.lifecycleOwner = this
        initRecyclerView()
//        layout.playerView.player = exoPlayer
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
//                layout.playerView.visibility = View.VISIBLE
                exoPlayer.play()
            }
        }

        layout.rvAudioFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
            adapter = audiosAdapter
        }
    }

    //region PERMISSIONS VALIDATION
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