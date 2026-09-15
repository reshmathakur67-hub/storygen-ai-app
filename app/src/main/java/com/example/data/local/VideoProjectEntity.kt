package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalScript: String,
    val characterName: String,
    val characterRole: String,
    val aspectRatio: String, // "9:16" or "16:9"
    val durationSec: Int, // 5, 10, or 30
    val styleKey: String,
    val voiceProfileKey: String,
    val scenesJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
