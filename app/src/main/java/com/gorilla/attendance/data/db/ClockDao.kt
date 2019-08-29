package com.gorilla.attendance.data.db

import androidx.room.*
import com.gorilla.attendance.data.model.ClockData
import io.reactivex.Single

@Dao
abstract class ClockDao{
    @Insert(onConflict = OnConflictStrategy.ROLLBACK)
    abstract fun insertClockData(clockData: ClockData?): Long

    @Query("SELECT * FROM ClockData")
    abstract fun getClockData(): Single<Array<ClockData>>

    @Delete
    abstract fun deleteClockData(listClockData: Array<ClockData>): Int
}