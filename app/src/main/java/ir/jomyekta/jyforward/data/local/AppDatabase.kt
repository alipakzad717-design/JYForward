package ir.jomyekta.jyforward.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [FilterRuleEntity::class, HistoryLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun filterRuleDao(): FilterRuleDao
    abstract fun historyLogDao(): HistoryLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        const val SAMPLE_JOMYEKTA_WEBHOOK =
            "https://www.jomyekta.ir/index.php?option=com_osmembership&task=payment_confirm&payment_method=os_cardtocard&cardtocard_action=webhook&token=%24143356%40Aliapalipz&msg={msg}"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jy_forward_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default JomYekta Card-to-Card rule on first install
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).filterRuleDao().insertRule(
                                    FilterRuleEntity(
                                        title = "JomYekta Card-to-Card (واریز)",
                                        senderFilter = "*",
                                        keyword = "واریز",
                                        destinationUrl = SAMPLE_JOMYEKTA_WEBHOOK,
                                        httpMethod = "GET",
                                        simSlotFilter = -1,
                                        isEnabled = true
                                    )
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
