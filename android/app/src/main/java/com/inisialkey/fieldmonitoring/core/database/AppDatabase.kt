package com.inisialkey.fieldmonitoring.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.inisialkey.fieldmonitoring.report.data.AttachmentEntity
import com.inisialkey.fieldmonitoring.report.data.ReportDao
import com.inisialkey.fieldmonitoring.report.data.ReportEntity

@Database(entities = [ReportEntity::class, AttachmentEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
