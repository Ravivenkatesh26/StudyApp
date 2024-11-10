package com.example.studyapp.data.repository

import com.example.studyapp.data.local.SessionDao
import com.example.studyapp.domain.model.Session
import com.example.studyapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {
    override suspend fun insertSession(session: Session) {
        sessionDao.insertSession(session)
    }

    override suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(session)
    }

    override fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSession().map { session ->
            session.sortedByDescending { it.date }
        }
    }

    override fun getRecentFiveSessions(): Flow<List<Session>> {
        return sessionDao.getAllSession().map { session ->
            session.sortedByDescending { it.date }
        }.take(5)
    }

    override fun getRecentTenSession(subjectId: Int): Flow<List<Session>> {
      return  sessionDao.getRecentSessionForSubject(subjectId).map { session ->
          session.sortedByDescending { it.date }
      }.take(10)
    }

    override fun getTotalSessionDuration(): Flow<Long> {
        return sessionDao.getTotalSessionDuration()
    }

    override fun getTotalSessionDurationBySubjectId(subjectId: Int): Flow<Long> {
        return sessionDao.getTotalSessionDurationBySubjectId(subjectId)
    }
}