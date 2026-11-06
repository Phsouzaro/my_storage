package com.mystorage.data.remote

import com.mystorage.domain.model.ProductInfo
import com.mystorage.domain.model.ProductSource
import com.mystorage.domain.repository.ProductInfoSource
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import javax.inject.Inject
import javax.inject.Named

/** Bluesoft Cosmos — base brasileira de GTIN. Requer token (cosmos.token em local.properties). */
interface CosmosApi {
    @GET("gtins/{barcode}.json")
    suspend fun gtin(
        @Path("barcode") barcode: String,
        @Header("X-Cosmos-Token") token: String,
    ): Response<CosmosProduct>

    companion object {
        const val BASE_URL = "https://api.cosmos.bluesoft.com.br/"
        const val TOKEN = "cosmosToken"
    }
}

@Serializable
data class CosmosProduct(
    val description: String? = null,
    val brand: CosmosBrand? = null,
    val thumbnail: String? = null,
)

@Serializable
data class CosmosBrand(val name: String? = null)

class CosmosSource @Inject constructor(
    private val api: CosmosApi,
    @param:Named(CosmosApi.TOKEN) private val token: String,
) : ProductInfoSource {
    override val name = "Cosmos"

    override suspend fun lookup(barcode: String): ProductInfo? {
        if (token.isBlank()) return null // sem token configurado: fonte desativada
        val response = api.gtin(barcode, token)
        if (response.code() == 404) return null
        if (!response.isSuccessful) throw HttpException(response)
        val body = response.body() ?: return null
        val name = body.description?.takeIf { it.isNotBlank() } ?: return null
        return ProductInfo(
            barcode = barcode,
            name = name.trim(),
            brand = body.brand?.name?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = body.thumbnail?.takeIf { it.isNotBlank() },
            source = ProductSource.COSMOS,
        )
    }
}
