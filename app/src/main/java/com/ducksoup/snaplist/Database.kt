package com.ducksoup.snaplist

import android.content.Context
import androidx.lifecycle.*
import androidx.room.*
import com.ducksoup.snaplist.model.SList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Database(entities = [SList::class], version = 3, exportSchema = false)
abstract class SnapListDatabase : RoomDatabase() {

    abstract fun listDao(): ListDao

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
interface ListDao {

    @Query("SELECT * FROM lists")
    fun getLists(): Flow<List<SList>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(list: SList)

    @Query("DELETE FROM lists")
    suspend fun deleteAll()
}

class ListViewModel(private val listDao: ListDao) : ViewModel() {
    val allLists: LiveData<List<SList>> = listDao.getLists().asLiveData()
    fun insert(list: SList) = viewModelScope.launch { listDao.insert(list) }
}

class ListViewModelFactory(private val listDao: ListDao): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListViewModel(listDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}