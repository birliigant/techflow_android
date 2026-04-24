package com.birliigant.techflow.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "cached_questions")
data class CachedQuestionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val excerpt: String,
    val authorName: String,
    val answerCount: Int,
    val voteCount: Int,
    val viewCount: Int,
    val createdAt: String,
    val tags: List<String>,
    val syncedAt: Long,
)

@Dao
interface QuestionDao {
    @Query("SELECT * FROM cached_questions ORDER BY syncedAt DESC, createdAt DESC")
    suspend fun getAll(): List<CachedQuestionEntity>

    @Query("SELECT * FROM cached_questions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedQuestionEntity>)

    @Query("DELETE FROM cached_questions")
    suspend fun clearAll()
}

class RoomConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(items: List<String>): String = gson.toJson(items)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}

@Database(
    entities = [CachedQuestionEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class TechFlowDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
}
