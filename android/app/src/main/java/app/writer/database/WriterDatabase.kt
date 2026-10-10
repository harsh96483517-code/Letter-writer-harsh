package app.writer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.writer.model.ExportRecord
import app.writer.model.TemplateState
import app.writer.model.WriterDocument

@Database(
    entities = [WriterDocument::class, ExportRecord::class, TemplateState::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class WriterDatabase : RoomDatabase() {
    abstract fun documents(): DocumentDao
    abstract fun exports(): ExportDao
    abstract fun templateStates(): TemplateStateDao

    companion object {
        fun create(context: Context): WriterDatabase =
            Room.databaseBuilder(context.applicationContext, WriterDatabase::class.java, "writer.db")
                // No destructive fallback on purpose: a schema change must ship a real migration,
                // otherwise people's letters would be wiped.
                .build()
    }
}
