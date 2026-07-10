package com.facultytimetable.pro.domain.usecase.audit

import com.facultytimetable.pro.data.local.db.dao.AuditLogDao
import com.facultytimetable.pro.data.local.db.entity.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun log(
        action: String,
        entityType: String,
        entityId: Long,
        details: String = "",
        performedBy: String = "user"
    ) {
        scope.launch {
            auditLogDao.insert(
                AuditLogEntity(
                    action = action,
                    entityType = entityType,
                    entityId = entityId,
                    details = details,
                    performedBy = performedBy
                )
            )
        }
    }

    fun logCreation(entityType: String, entityId: Long, name: String = "") {
        log("Created $entityType: $name", entityType, entityId)
    }

    fun logUpdate(entityType: String, entityId: Long, name: String = "") {
        log("Updated $entityType: $name", entityType, entityId)
    }

    fun logDeletion(entityType: String, entityId: Long, name: String = "") {
        log("Deleted $entityType: $name", entityType, entityId)
    }

    fun logGeneration(entityType: String, count: Int) {
        log("Auto-generated $count timetable entries", entityType, 0)
    }
}
