package ir.jomyekta.jyforward

import android.app.Application
import ir.jomyekta.jyforward.data.local.AppDatabase
import ir.jomyekta.jyforward.data.repository.ForwardRepository

class JYForwardApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: ForwardRepository by lazy {
        ForwardRepository(
            filterRuleDao = database.filterRuleDao(),
            historyLogDao = database.historyLogDao()
        )
    }
}
