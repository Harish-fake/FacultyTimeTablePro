package com.facultytimetable.pro.data.local.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.facultytimetable.pro.BuildConfig
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import com.facultytimetable.pro.data.local.db.AppDatabase
import com.facultytimetable.pro.data.local.db.entity.AcademicYearEntity
import com.facultytimetable.pro.data.local.db.entity.BackupHistoryEntity
import com.facultytimetable.pro.data.local.db.entity.DepartmentEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyEntity
import com.facultytimetable.pro.data.local.db.entity.FacultyLeaveEntity
import com.facultytimetable.pro.data.local.db.entity.HolidayEntity
import com.facultytimetable.pro.data.local.db.entity.RoomEntity
import com.facultytimetable.pro.data.local.db.entity.SectionEntity
import com.facultytimetable.pro.data.local.db.entity.SemesterEntity
import com.facultytimetable.pro.data.local.db.entity.SubjectEntity
import com.facultytimetable.pro.data.local.db.entity.TimeSlotEntity
import com.facultytimetable.pro.data.local.db.entity.TimetableEntryEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Version of the portable JSON snapshot format. */
const val BACKUP_FORMAT_VERSION = 2

data class BackupPayload(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = BuildConfig.VERSION_NAME,
    /**
     * SHA-256 of the payload with this field cleared. Older (format 1) backups do not
     * contain it; their integrity is reported as "legacy" instead of silently trusted.
     */
    val checksum: String? = null,
    val departments: List<DepartmentEntity> = emptyList(),
    val faculty: List<FacultyEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val rooms: List<RoomEntity> = emptyList(),
    val academicYears: List<AcademicYearEntity> = emptyList(),
    val semesters: List<SemesterEntity> = emptyList(),
    val sections: List<SectionEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val timetableEntries: List<TimetableEntryEntity> = emptyList(),
    val holidays: List<HolidayEntity> = emptyList(),
    val facultyLeaves: List<FacultyLeaveEntity> = emptyList()
) {
    val totalRecords: Int
        get() = departments.size + faculty.size + subjects.size + rooms.size +
            academicYears.size + semesters.size + sections.size + timeSlots.size +
            timetableEntries.size + holidays.size + facultyLeaves.size
}

data class BackupResult(
    val success: Boolean,
    val message: String,
    val records: Int = 0,
    val fileName: String? = null
)

/** Read-only summary shown before a restore is confirmed. */
data class BackupPreview(
    val valid: Boolean,
    val message: String,
    val appVersion: String = "",
    val createdAt: Long = 0L,
    val formatVersion: Int = 0,
    val departments: Int = 0,
    val faculty: Int = 0,
    val subjects: Int = 0,
    val rooms: Int = 0,
    val sections: Int = 0,
    val timeSlots: Int = 0,
    val timetableEntries: Int = 0,
    val totalRecords: Int = 0,
    val checksumVerified: Boolean = false,
    val legacyBackup: Boolean = false
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val appPreferences: AppPreferences
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun history(): Flow<List<BackupHistoryEntity>> = database.backupHistoryDao().getAllBackups()

    suspend fun createBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val snapshot = readSnapshot()
            val checksum = checksumOf(snapshot)
            val payload = snapshot.copy(checksum = checksum)
            val json = gson.toJson(payload)
            val bytes = json.toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: return@withContext BackupResult(false, "Could not open the selected file")

            val now = System.currentTimeMillis()
            val fileName = suggestedFileName()
            database.backupHistoryDao().insert(
                BackupHistoryEntity(
                    fileName = fileName,
                    filePath = uri.toString(),
                    fileSize = bytes.size.toLong(),
                    checksum = checksum,
                    notes = "${payload.totalRecords} records · verified SHA-256",
                    createdAt = now
                )
            )
            appPreferences.setLastBackupAt(now)
            BackupResult(
                success = true,
                message = "Backup saved and checksum verified (${payload.totalRecords} records)",
                records = payload.totalRecords,
                fileName = fileName
            )
        } catch (e: Exception) {
            BackupResult(false, e.message ?: "Backup failed")
        }
    }

    /**
     * Parses and verifies a backup without touching the database, so the confirmation
     * dialog can show what will be restored (and whether the file is intact).
     */
    suspend fun inspectBackup(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext BackupPreview(false, "Could not read the selected file")

            val payload = gson.fromJson(json, BackupPayload::class.java)
                ?: return@withContext BackupPreview(false, "The file is not a valid backup")
            val problem = validatePayload(payload)
            if (problem != null) return@withContext BackupPreview(false, problem)

            val checksumVerified = payload.checksum != null
            BackupPreview(
                valid = true,
                message = when {
                    checksumVerified -> "File integrity verified"
                    else -> "Legacy backup without a checksum; its contents cannot be verified"
                },
                appVersion = payload.appVersion,
                createdAt = payload.createdAt,
                formatVersion = payload.formatVersion,
                departments = payload.departments.size,
                faculty = payload.faculty.size,
                subjects = payload.subjects.size,
                rooms = payload.rooms.size,
                sections = payload.sections.size,
                timeSlots = payload.timeSlots.size,
                timetableEntries = payload.timetableEntries.size,
                totalRecords = payload.totalRecords,
                checksumVerified = checksumVerified,
                legacyBackup = !checksumVerified
            )
        } catch (e: Exception) {
            BackupPreview(false, e.message ?: "Could not inspect the selected file")
        }
    }

    suspend fun restoreBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext BackupResult(false, "Could not read the selected file")

            val payload = gson.fromJson(json, BackupPayload::class.java)
                ?: return@withContext BackupResult(false, "The file is not a valid backup")
            validatePayload(payload)?.let { problem ->
                return@withContext BackupResult(false, problem)
            }

            val legacy = payload.checksum == null
            database.withTransaction {
                clearAll()
                payload.departments.forEach { database.departmentDao().insert(it) }
                payload.academicYears.forEach { database.academicYearDao().insert(it) }
                payload.rooms.forEach { database.roomDao().insert(it) }
                payload.subjects.forEach { database.subjectDao().insert(it) }
                payload.faculty.forEach { database.facultyDao().insert(it) }
                payload.semesters.forEach { database.semesterDao().insert(it) }
                payload.sections.forEach { database.sectionDao().insert(it) }
                payload.timeSlots.forEach { database.timeSlotDao().insert(it) }
                payload.holidays.forEach { database.holidayDao().insert(it) }
                payload.facultyLeaves.forEach { database.facultyLeaveDao().insert(it) }
                payload.timetableEntries.forEach { database.timetableEntryDao().insert(it) }
            }

            BackupResult(
                success = true,
                message = if (legacy) {
                    "Restored ${payload.totalRecords} records from a legacy backup (no checksum to verify)"
                } else {
                    "Restored ${payload.totalRecords} records with an intact checksum"
                },
                records = payload.totalRecords
            )
        } catch (e: Exception) {
            BackupResult(false, e.message ?: "Restore failed")
        }
    }

    suspend fun deleteHistoryItem(id: Long) = withContext(Dispatchers.IO) {
        database.backupHistoryDao().deleteById(id)
    }

    private suspend fun readSnapshot(): BackupPayload = BackupPayload(
        departments = database.departmentDao().getAllDepartments().first(),
        faculty = database.facultyDao().getAllFaculty().first(),
        subjects = database.subjectDao().getAllSubjects().first(),
        rooms = database.roomDao().getAllRooms().first(),
        academicYears = database.academicYearDao().getAllAcademicYears().first(),
        semesters = database.semesterDao().getAllSemesters().first(),
        sections = database.sectionDao().getAllSections().first(),
        timeSlots = database.timeSlotDao().getAllTimeSlots().first(),
        timetableEntries = database.timetableEntryDao().getAllEntriesList(),
        holidays = database.holidayDao().getAllHolidays().first(),
        facultyLeaves = database.facultyLeaveDao().getAllLeaves().first()
    )

    private suspend fun clearAll() {
        database.timetableEntryDao().deleteAll()
        database.facultyLeaveDao().deleteAll()
        database.substituteFacultyDao().deleteAll()
        database.sectionDao().deleteAll()
        database.semesterDao().deleteAll()
        database.academicYearDao().deleteAll()
        database.holidayDao().deleteAll()
        database.timeSlotDao().deleteAll()
        database.facultyDao().deleteAll()
        database.subjectDao().deleteAll()
        database.roomDao().deleteAll()
        database.departmentDao().deleteAll()
    }

    /**
     * Shared validation used by both preview and restore. Returns a user-facing problem or
     * null when the snapshot is structurally sound and its checksum matches.
     */
    private fun validatePayload(payload: BackupPayload): String? {
        if (payload.formatVersion > BACKUP_FORMAT_VERSION) {
            return "This backup was created by a newer app version"
        }
        if (payload.departments.isEmpty() || payload.sections.isEmpty()) {
            return "The backup does not contain timetable data"
        }
        if (payload.checksum == null) {
            if (payload.formatVersion >= 2) {
                return "The backup is missing its integrity checksum"
            }
        } else if (!payload.checksum.equals(checksumOf(payload), ignoreCase = true)) {
            return "The backup file appears to be corrupted (checksum mismatch)"
        }
        return validateReferences(payload)
    }

    private fun checksumOf(payload: BackupPayload): String =
        BackupIntegrity.checksumOf(gson.toJson(payload.copy(checksum = null)))

    private fun validateReferences(payload: BackupPayload): String? {
        val deptIds = payload.departments.map { it.id }.toSet()
        val yearIds = payload.academicYears.map { it.id }.toSet()
        val semesterIds = payload.semesters.map { it.id }.toSet()
        val subjectIds = payload.subjects.map { it.id }.toSet()
        val facultyIds = payload.faculty.map { it.id }.toSet()
        val roomIds = payload.rooms.map { it.id }.toSet()
        val sectionIds = payload.sections.map { it.id }.toSet()
        val slotIds = payload.timeSlots.map { it.id }.toSet()

        val duplicateIds = listOf(
            "academic years" to payload.academicYears.map { it.id },
            "semesters" to payload.semesters.map { it.id },
            "departments" to payload.departments.map { it.id },
            "faculty" to payload.faculty.map { it.id },
            "subjects" to payload.subjects.map { it.id },
            "rooms" to payload.rooms.map { it.id },
            "sections" to payload.sections.map { it.id },
            "time slots" to payload.timeSlots.map { it.id },
            "timetable entries" to payload.timetableEntries.map { it.id },
            "holidays" to payload.holidays.map { it.id },
            "leave records" to payload.facultyLeaves.map { it.id }
        ).firstOrNull { (_, ids) -> ids.size != ids.distinct().size }
        if (duplicateIds != null) return "The backup contains duplicate ${duplicateIds.first} ids"

        if (payload.faculty.any { it.departmentId !in deptIds }) return "Some faculty refer to a missing department"
        if (payload.subjects.any { it.departmentId !in deptIds }) return "Some subjects refer to a missing department"
        if (payload.semesters.any { it.academicYearId !in yearIds }) return "Some semesters refer to a missing academic year"
        if (payload.sections.any { it.semesterId !in semesterIds }) return "Some sections refer to a missing semester"
        if (payload.sections.any { it.departmentId != 0L && it.departmentId !in deptIds }) {
            return "Some sections refer to a missing department"
        }
        val invalidEntry = payload.timetableEntries.firstOrNull {
            it.sectionId !in sectionIds || it.subjectId !in subjectIds ||
                it.facultyId !in facultyIds || it.roomId !in roomIds || it.timeSlotId !in slotIds
        }
        if (invalidEntry != null) return "A timetable entry refers to missing data"
        if (payload.facultyLeaves.any { it.facultyId !in facultyIds }) {
            return "Some leave records refer to a missing faculty member"
        }
        return null
    }

    companion object {
        fun suggestedFileName(): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            return "faculty-timetable-$stamp.json"
        }

        val mimeType: String = "application/json"
    }
}
