package com.example.studyapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.studyapp.domain.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Upsert
    suspend fun upsertSubject(subject: Subject)

    @Query("SELECT COUNT(*) FROM SUBJECT")
    fun getTotalSubjectCount(): Flow<Int>

    @Query("SELECT SUM(goalHours) FROM Subject")
    fun getTotalGoalHours(): Flow<Float>

    @Query("SELECT *FROM Subject WHERE subjectId = :subjectId")
    suspend fun getSubjectId(subjectId: Int): Subject?

    @Query("DELETE FROM Subject WHERE subjectId = :subjectId")
    suspend fun deleteSubject(subjectId: Int)

    @Query("SELECT *FROM Subject")
    fun getAllSubjects(): Flow<List<Subject>>

}