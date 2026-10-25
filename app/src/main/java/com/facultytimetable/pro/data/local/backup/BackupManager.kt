package com.facultytimetable.pro.data.local.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
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
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Version of the portable JSON snapshot format. */
const val BACKUP_FORMAT_VERSION = 1

data class BackupPayload(
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.1.0",
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

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun history(): Flow<List<BackupHistoryEntity>> = database.backupHistoryDao().getAllBackups()

    suspend fun createBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val payload = readSnapshot()
            val json = gson.toJson(payload)
            val bytes = json.toByteArray(Charsets.UTF_8)
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: return@withContext BackupResult(false, "Could not open the selected file")

            val fileName = suggestedFileName()
            database.backupHistoryDao().insert(
                BackupHistoryEntity(
                    fileName = fileName,
                    filePath = uri.toString(),
                    fileSize = bytes.size.toLong(),
                    checksum = sha256(bytes),
                    notes = "${payload.totalRecords} records"
                )
            )
            BackupResult(
                success = true,
                message = "Backup saved (${payload.totalRecords} records)",
                records = payload.totalRecords,
                fileName = fileName
            )
        } catch (e: Exception) {
            BackupResult(false, e.message ?: "Backup failed")
        }
    }

    suspend fun restoreBackup(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext BackupResult(false, "Could not read the selected file")

            val payload = gson.fromJson(json, BackupPayload::class.java)
                ?: return@withContext BackupResult(false, "The file is not a valid backup")
            if (payload.formatVersion > BACKUP_FORMAT_VERSION) {
                return@withContext BackupResult(
                    false,
                    "This backup was created by a newer app version"
                )
            }
            if (payload.departments.isEmpty() || payload.sections.isEmpty()) {
                return@withContext BackupResult(false, "The backup does not contain timetable data")
            }
            validateReferences(payload)?.let { problem ->
                return@withContext BackupResult(false, problem)
            }

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
                message = "Restored ${payload.totalRecords} records",
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

    private fun validateReferences(payload: BackupPayload): String? {
        val deptIds = payload.departments.map { it.id }.toSet()
        val yearIds = payload.academicYears.map { it.id }.toSet()
        val semesterIds = payload.semesters.map { it.id }.toSet()
        val subjectIds = payload.subjects.map { it.id }.toSet()
        val facultyIds = payload.faculty.map { it.id }.toSet()
        val roomIds = payload.rooms.map { it.id }.toSet()
        val sectionIds = payload.sections.map { it.id }.toSet()
        val slotIds = payload.timeSlots.map { it.id }.toSet()

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

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    companion object {
        fun suggestedFileName(): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            return "faculty-timetable-$stamp.json"
        }

        val mimeType: String = "application/json"
    }
}
