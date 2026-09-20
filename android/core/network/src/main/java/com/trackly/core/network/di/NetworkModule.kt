package com.trackly.core.network.di

import com.trackly.core.network.AuthApi
import com.trackly.core.network.OrderApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun checkIsEmulator(): Boolean {
        val brand = android.os.Build.BRAND
        val device = android.os.Build.DEVICE
        val fingerprint = android.os.Build.FINGERPRINT
        val hardware = android.os.Build.HARDWARE
        val model = android.os.Build.MODEL
        val manufacturer = android.os.Build.MANUFACTURER
        val product = android.os.Build.PRODUCT

        return (brand.startsWith("generic") && device.startsWith("generic")) ||
                fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                model.contains("google_sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK built for x86") ||
                manufacturer.contains("Genymotion") ||
                product.contains("sdk_gphone") ||
                product.contains("google_sdk") ||
                product.contains("sdk") ||
                product.contains("sdk_x86") ||
                product.contains("vbox86p") ||
                product.contains("emulator") ||
                product.contains("simulator")
    }

    private val BASE_URL: String
        get() = if (checkIsEmulator()) "http://10.0.2.2:8080/" else "http://192.168.0.111:8080/"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderApi(retrofit: Retrofit): OrderApi {
        return retrofit.create(OrderApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDriverApi(retrofit: Retrofit): com.trackly.core.network.DriverApi {
        return retrofit.create(com.trackly.core.network.DriverApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeocodingApi(okHttpClient: OkHttpClient): com.trackly.core.network.GeocodingApi {
        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.trackly.core.network.GeocodingApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAddressSearchRepository(
        geocodingApi: com.trackly.core.network.GeocodingApi
    ): com.trackly.core.network.AddressSearchRepository {
        return com.trackly.core.network.AddressSearchRepositoryImpl(geocodingApi)
    }
}

