package ir.jomyekta.jyforward.data.repository

import ir.jomyekta.jyforward.data.local.FilterRuleDao
import ir.jomyekta.jyforward.data.local.FilterRuleEntity
import ir.jomyekta.jyforward.data.local.HistoryLogDao
import ir.jomyekta.jyforward.data.local.HistoryLogEntity
import ir.jomyekta.jyforward.data.network.WebhookDispatcher
import ir.jomyekta.jyforward.data.network.WebhookResult
import kotlinx.coroutines.flow.Flow

class ForwardRepository(
    private val filterRuleDao: FilterRuleDao,
    private val historyLogDao: HistoryLogDao,
    private val webhookDispatcher: WebhookDispatcher = WebhookDispatcher()
) {
    val allRulesFlow: Flow<List<FilterRuleEntity>> = filterRuleDao.observeAllRules()
    val allHistoryLogsFlow: Flow<List<HistoryLogEntity>> = historyLogDao.observeAllLogs()

    suspend fun getAllRulesSnapshot(): List<FilterRuleEntity> =
        filterRuleDao.getAllRulesSnapshot()

    suspend fun saveRule(rule: FilterRuleEntity): Long {
        return if (rule.id == 0L) {
            filterRuleDao.insertRule(rule)
        } else {
            filterRuleDao.updateRule(rule)
            rule.id
        }
    }

    suspend fun cloneRule(rule: FilterRuleEntity): Long {
        val cloned = rule.copy(
            id = 0L,
            title = "${rule.title} (Copy)",
            createdAt = System.currentTimeMillis()
        )
        return filterRuleDao.insertRule(cloned)
    }

    suspend fun deleteRule(rule: FilterRuleEntity) {
        filterRuleDao.deleteRule(rule)
    }

    suspend fun toggleRule(ruleId: Long, enabled: Boolean) {
        filterRuleDao.setRuleEnabled(ruleId, enabled)
    }

    suspend fun replaceOrMergeRules(rules: List<FilterRuleEntity>, clearExisting: Boolean = false) {
        if (clearExisting) {
            filterRuleDao.deleteAllRules()
        }
        val sanitized = rules.map { it.copy(id = 0L, createdAt = System.currentTimeMillis()) }
        filterRuleDao.insertRules(sanitized)
    }

    suspend fun clearAllHistory() {
        historyLogDao.clearAllLogs()
    }

    /**
     * Evaluates an incoming SMS against all enabled rules in Room DB.
     * ONLY logs messages to [history_logs] if they trigger at least one active filter rule.
     */
    suspend fun processIncomingSms(
        sender: String,
        messageBody: String,
        simSlotIndex: Int,
        subscriptionId: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Int {
        val activeRules = filterRuleDao.getEnabledRules()
        var triggeredCount = 0

        for (rule in activeRules) {
            if (rule.matches(sender, messageBody, simSlotIndex)) {
                triggeredCount++
                val result: WebhookResult = webhookDispatcher.dispatchSmsWebhook(
                    templateUrl = rule.destinationUrl,
                    httpMethod = rule.httpMethod,
                    sender = sender,
                    messageBody = messageBody,
                    simSlotIndex = simSlotIndex,
                    subscriptionId = subscriptionId,
                    timestamp = timestamp
                )

                val historyEntry = HistoryLogEntity(
                    ruleId = rule.id,
                    ruleTitle = rule.title,
                    sender = sender,
                    messageBody = messageBody,
                    matchedKeyword = rule.keyword,
                    simSlotIndex = simSlotIndex,
                    subscriptionId = subscriptionId,
                    destinationUrl = result.resolvedUrl,
                    httpMethod = rule.httpMethod,
                    isSuccess = result.isSuccess,
                    httpStatusCode = result.httpStatusCode,
                    responseSummary = result.responseBodySummary,
                    timestamp = timestamp
                )
                historyLogDao.insertLog(historyEntry)
            }
        }
        return triggeredCount
    }

    /**
     * Allows manual testing of a specific rule from the UI and logs the test result.
     */
    suspend fun testRuleWebhook(rule: FilterRuleEntity, sampleMessage: String): WebhookResult {
        val testSender = if (rule.senderFilter.isBlank() || rule.senderFilter == "*") {
            "9820000"
        } else {
            rule.senderFilter
        }
        val simSlot = if (rule.simSlotFilter == 1) 1 else 0
        val result = webhookDispatcher.dispatchSmsWebhook(
            templateUrl = rule.destinationUrl,
            httpMethod = rule.httpMethod,
            sender = testSender,
            messageBody = sampleMessage,
            simSlotIndex = simSlot,
            subscriptionId = simSlot + 1
        )
        historyLogDao.insertLog(
            HistoryLogEntity(
                ruleId = rule.id,
                ruleTitle = "${rule.title} [TEST]",
                sender = testSender,
                messageBody = sampleMessage,
                matchedKeyword = rule.keyword,
                simSlotIndex = simSlot,
                subscriptionId = simSlot + 1,
                destinationUrl = result.resolvedUrl,
                httpMethod = rule.httpMethod,
                isSuccess = result.isSuccess,
                httpStatusCode = result.httpStatusCode,
                responseSummary = result.responseBodySummary
            )
        )
        return result
    }
}
