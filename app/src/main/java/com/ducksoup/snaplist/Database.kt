package com.ducksoup.snaplist

import android.content.Context
import androidx.lifecycle.*
import androidx.room.*
import com.ducksoup.snaplist.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Database(entities = [SList::class, SItem::class], version = 11, exportSchema = false)
abstract class SnapListDatabase : RoomDatabase() {

    abstract fun dao(): SnapListDao

    companion object {
        @Volatile
        private var INSTANCE: SnapListDatabase? = null

        fun getDatabase(context: Context): SnapListDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SnapListDatabase::class.java,
                    "SnapListDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

@Dao
interface SnapListDao {

    // Items

    @Insert
    suspend fun insertItem(item: SItem)

    @Update
    suspend fun updateItem(item: SItem): Int

    @Query("DELETE FROM items WHERE listId = :listId")
    suspend fun deleteItems(listId: Int):Int

    @Query("DELETE FROM items WHERE listId = :listId AND checked = 1")
    suspend fun deleteCheckedItems(listId: Int):Int

    // Get items from given list
    @Query("SELECT * FROM items WHERE listId = :listId")
    suspend fun getItems(listId: Int): List<SItem>


    // Lists

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: SList): Long

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: Int)

    @Transaction
    suspend fun delList(listId: Int) {
        deleteList(listId)
        deleteItems(listId)
    }

    @Query("DELETE FROM lists")
    suspend fun deleteAllLists()

    @Query("SELECT * FROM lists")
    suspend fun getLists(): List<SList>

}