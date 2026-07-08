package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.RoomType
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {

    @Query("SELECT * FROM rooms ORDER BY name ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoomById(id: Long): RoomEntity?

    @Query("SELECT * FROM rooms WHERE id = :id")
    fun getRoomByIdFlow(id: Long): Flow<RoomEntity?>

    @Query("SELECT * FROM rooms WHERE type = :type ORDER BY name ASC")
    fun getRoomsByType(type: RoomType): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE name = :name LIMIT 1")
    suspend fun getRoomByName(name: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(room: RoomEntity): Long

    @Update
    suspend fun update(room: RoomEntity)

    @Delete
    suspend fun delete(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM rooms")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM rooms WHERE type = :type")
    fun getRoomCountByType(type: RoomType): Flow<Int>

    @Query("SELECT * FROM rooms WHERE name LIKE '%' || :query || '%' OR building LIKE '%' || :query || '%'")
    fun searchRooms(query: String): Flow<List<RoomEntity>>
}
