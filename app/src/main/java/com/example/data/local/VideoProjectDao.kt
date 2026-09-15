package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<VideoProjectEntity>>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): VideoProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity): Long

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("DELETE FROM video_projects")
    suspend fun deleteAll()
}
