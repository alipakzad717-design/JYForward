package ir.jomyekta.jyforward.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterRuleDao {

    @Query("SELECT * FROM filter_rules ORDER BY createdAt DESC")
    fun observeAllRules(): Flow<List<FilterRuleEntity>>

    @Query("SELECT * FROM filter_rules ORDER BY createdAt DESC")
    suspend fun getAllRulesSnapshot(): List<FilterRuleEntity>

    @Query("SELECT * FROM filter_rules WHERE isEnabled = 1 ORDER BY createdAt DESC")
    suspend fun getEnabledRules(): List<FilterRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FilterRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<FilterRuleEntity>)

    @Update
    suspend fun updateRule(rule: FilterRuleEntity)

    @Delete
    suspend fun deleteRule(rule: FilterRuleEntity)

    @Query("UPDATE filter_rules SET isEnabled = :enabled WHERE id = :ruleId")
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)

    @Query("DELETE FROM filter_rules")
    suspend fun deleteAllRules()
}
