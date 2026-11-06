package com.mystorage.di

import com.mystorage.BuildConfig
import com.mystorage.data.remote.CosmosApi
import com.mystorage.data.remote.CosmosSource
import com.mystorage.data.remote.OpenFoodFactsApi
import com.mystorage.data.remote.OpenFoodFactsSource
import com.mystorage.domain.repository.ProductInfoSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** OFF pede um User-Agent identificando o app. */
    private const val USER_AGENT = "MyStorage/${BuildConfig.VERSION_NAME} (Android; personal project)"

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("X-Cosmos-Token")
                })
            }
        }
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(client: OkHttpClient, json: Json): OpenFoodFactsApi =
        retrofit(OpenFoodFactsApi.BASE_URL, client, json).create(OpenFoodFactsApi::class.java)

    @Provides
    @Singleton
    fun provideCosmosApi(client: OkHttpClient, json: Json): CosmosApi =
        retrofit(CosmosApi.BASE_URL, client, json).create(CosmosApi::class.java)

    @Provides
    @Named(CosmosApi.TOKEN)
    fun provideCosmosToken(): String = BuildConfig.COSMOS_TOKEN

    /** Ordem de consulta: Open Food Facts → Cosmos. */
    @Provides
    fun provideProductInfoSources(
        off: OpenFoodFactsSource,
        cosmos: CosmosSource,
    ): List<@JvmSuppressWildcards ProductInfoSource> = listOf(off, cosmos)
}
