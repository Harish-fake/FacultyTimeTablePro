package com.facultytimetable.pro.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.facultytimetable.pro.data.local.db.entity.FacultyAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FacultyAssignmentDao {
    @Query("SELECT * FROM faculty_assignments ORDER BY departmentId ASC, semesterId ASC")
    fun getAllAssignments(): Flow<List<FacultyAssignmentEntity>>

    @Query("SELECT * FROM faculty_assignments WHERE id = :id")
    suspend fun getAssignmentById(id: Long): FacultyAssignmentEntity?

    @Query("SELECT * FROM faculty_assignments WHERE departmentId = :departmentId")
    fun getAssignmentsByDepartment(departmentId: Long): Flow<List<FacultyAssignmentEntity>>

    @Query("SELECT * FROM faculty_assignments WHERE semesterId = :semesterId")
    fun getAssignmentsBySemester(semesterId: Long): Flow<List<FacultyAssignmentEntity>>

    @Query("SELECT * FROM faculty_assignments WHERE facultyId = :facultyId")
    fun getAssignmentsByFaculty(facultyId: Long): Flow<List<FacultyAssignmentEntity>>

    @Query("SELECT * FROM faculty_assignments WHERE subjectId = :subjectId")
    fun getAssignmentsBySubject(subjectId: Long): Flow<List<FacultyAssignmentEntity>>

    @Query("SELECT * FROM faculty_assignments WHERE departmentId = :deptId AND semesterId = :semId")
    fun getAssignmentsByDeptAndSemester(deptId: Long, semId: Long): Flow<List<FacultyAssignmentEntity>>

    @Query("SELECT * FROM faculty_assignments WHERE facultyId = :facultyId AND semesterId = :semesterId")
    suspend fun getAssignmentByFacultyAndSemester(facultyId: Long, semesterId: Long): List<FacultyAssignmentEntity>

    @Query("SELECT SUM(hoursPerWeek) FROM faculty_assignments WHERE facultyId = :facultyId AND semesterId = :semesterId")
    suspend fun getTotalHoursByFacultyAndSemester(facultyId: Long, semesterId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(assignment: FacultyAssignmentEntity): Long

    @Update
    suspend fun update(assignment: FacultyAssignmentEntity)

    @Delete
    suspend fun delete(assignment: FacultyAssignmentEntity)

    @Query("DELETE FROM faculty_assignments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM faculty_assignments")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM faculty_assignments")
    fun getCountFlow(): Flow<Int>
}
