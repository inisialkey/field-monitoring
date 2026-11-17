package com.inisialkey.fieldmonitoring.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.inisialkey.fieldmonitoring.report.data.AttachmentEntity
import com.inisialkey.fieldmonitoring.report.data.ReportDao
import com.inisialkey.fieldmonitoring.report.data.ReportEntity

@Database(entities = [ReportEntity::class, AttachmentEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE reports SET category = 'OTHER' WHERE category IS NOT NULL AND category NOT IN (" +
                        "'FOREST_REHABILITATION', 'MANGROVE', 'PEATLAND_RESTORATION', 'SOCIAL_FORESTRY', " +
                        "'RENEWABLE_ENERGY', 'WASTE_MANAGEMENT', 'BIODIVERSITY', 'OTHER')",
                )
            }
        }
    }
}
