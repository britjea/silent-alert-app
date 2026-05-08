package com.seuusuario.silentalert.core.di

import com.seuusuario.silentalert.BuildConfig
import com.seuusuario.silentalert.data.remote.api.NotificationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Replace the sha256/ values with your actual API server leaf-certificate
     * and issuer hashes (use `openssl s_client` or OkHttp's HandshakeCertificates
     * helper to obtain them). Two pins are recommended so a backup pin survives
     * cert rotation.
     */
    private val certificatePinner = CertificatePinner.Builder()
        .add(BuildConfig.API_HOST, "sha256/${BuildConfig.SSL_PIN_PRIMARY}")
        .add(BuildConfig.API_HOST, "sha256/${BuildConfig.SSL_PIN_BACKUP}")
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)
}
