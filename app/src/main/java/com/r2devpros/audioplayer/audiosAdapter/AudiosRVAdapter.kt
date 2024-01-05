package com.r2devpros.audioplayer.audiosAdapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.r2devpros.audioplayer.R
import com.r2devpros.audioplayer.databinding.AudioItemLayoutBinding
import timber.log.Timber

class AudiosRVAdapter(private val audioClicked: (AudioItemViewModel) -> Unit) :
    RecyclerView.Adapter<AudiosRVAdapter.ViewHolder>() {

    var itemList: List<AudioItemViewModel> = arrayListOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ViewHolder(val layout: AudioItemLayoutBinding) :
        RecyclerView.ViewHolder(layout.root) {
        fun bind(audioFile: AudioItemViewModel) {
            layout.root.setOnClickListener {
                audioClicked(audioFile)
            }
            Timber.d("ViewHolder_TAG: bind: ${audioFile.name}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.d("AudiosRVAdapter_TAG: onCreateViewHolder: ")
        val binding: AudioItemLayoutBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.audio_item_layout,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Timber.d("AudiosRVAdapter_TAG: onBindViewHolder: ")
        val audioFile = itemList[position]
        holder.layout.setVariable(BR.viewModel, audioFile)
        holder.bind(audioFile)
        holder.layout.executePendingBindings()
    }
}