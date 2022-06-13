package com.ducksoup.snaplist

import android.content.Context
import androidx.lifecycle.*
import androidx.room.*
import com.ducksoup.snaplist.model.SChoice
import com.ducksoup.snaplist.model.SItem
import com.ducksoup.snaplist.model.SList
import com.ducksoup.snaplist.model.SListWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Database(entities = [SList::class, SItem::class, SChoice::class], version = 8, exportSchema = false)
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

    @Insert
    suspend fun insertItem(item: SItem)

    @Query("UPDATE items SET checked = :checked WHERE id = :itemId")
    suspend fun setItemChecked(checked: Boolean, itemId: Int)

    @Query("DELETE FROM items WHERE listId = :listId")
    suspend fun deleteItems(listId: Int)

    @Query("DELETE FROM items WHERE listId = :listId AND checked = 1")
    suspend fun deleteCheckedItems(listId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: SList): Long

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteListOnly(listId: Int)

    @Transaction
    suspend fun deleteList(listId: Int) {
        deleteListOnly(listId)
        deleteItems(listId)
    }

    // These control which list (tab) is selected

    @Query("UPDATE choices SET selectedList = :listId")
    suspend fun setSelectedList(listId: Int)

    @Query("SELECT selectedList FROM choices LIMIT 1")
    suspend fun getSelectedList(): Int

    // Initiator method, should be replaced with something sensible


    // Get list of lists user has
    @Query("SELECT * FROM lists")
    suspend fun getLists(): List<SList>

    // Get items from given list
    @Query("SELECT * FROM items WHERE listId = :listId")
    suspend fun getItems(listId: Int): List<SItem>


    // For initialization debug
    @Query("DELETE FROM choices")
    suspend fun deleteAllChoices()

    @Query("DELETE FROM lists")
    suspend fun deleteAllLists()

    @Query("INSERT INTO choices (selectedList) VALUES (:selectedList)")
    suspend fun initChoices(selectedList: Int)

}

//class State(private val dao: SnapListDao): ViewModel() {
//    val lists: LiveData<List<SList>> = dao.getLists().asLiveData()
//    fun selectList(listId: Int) = viewModelScope.launch { dao.selectList(listId) }
//    fun getSelectedList() = viewModelScope.launch { dao.getSelectedList() }
//    fun insertList(list: SList) = viewModelScope.launch { dao.insertList(list) }
//}
//
//class Factory(private val dao: SnapListDao): ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(State::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return State(dao) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
