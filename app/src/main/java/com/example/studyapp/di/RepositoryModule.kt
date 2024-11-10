package com.example.studyapp.di

import com.example.studyapp.data.repository.SessionRepositoryImpl
import com.example.studyapp.data.repository.SubjectRepositoryImpl
import com.example.studyapp.data.repository.TaskRepositoryImpl
import com.example.studyapp.domain.repository.SessionRepository
import com.example.studyapp.domain.repository.SubjectRepository
import com.example.studyapp.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.SocketImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
 abstract class RepositoryModule {

     @Singleton
     @Binds
     abstract fun bindSubjectRepository(
         impl: SubjectRepositoryImpl
     ): SubjectRepository

    @Singleton
    @Binds
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Singleton
    @Binds
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository
}