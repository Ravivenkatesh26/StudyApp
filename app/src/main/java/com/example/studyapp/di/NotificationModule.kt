package com.example.studyapp.di

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.studyapp.R
import com.example.studyapp.presentation.session.ServiceHelper
import com.example.studyapp.presentation.session.ServiceHelper.clickPendingIntent
import com.example.studyapp.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.studyapp.util.Constants.NOTIFICATION_ID
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object NotificationModule {
    @ServiceScoped
    @Provides
    fun provideNotificationBuilder(
        @ApplicationContext context: Context
    ):NotificationCompat.Builder{
        return NotificationCompat.Builder(
            context,
            NOTIFICATION_CHANNEL_ID
        ).setContentTitle("Study Session Timer")
            .setContentText("00::00::00")
            .setSmallIcon(R.drawable.study)
            .setOngoing(true)
            .setContentIntent(ServiceHelper.clickPendingIntent(context))
    }

    @ServiceScoped
    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ):NotificationManager{
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}