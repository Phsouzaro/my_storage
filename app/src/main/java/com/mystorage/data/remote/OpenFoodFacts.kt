package com.mystorage.data.remote

import com.mystorage.domain.model.ProductInfo
import com.mystorage.domain.model.ProductSource
import com.mystorage.domain.repository.ProductInfoSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject

/** https://openfoodfacts.github.io/openfoodfacts-server/api/ — produto inexistente responde 404 com status 0. */
interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}")
    suspend fun product(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "code,product_name,product_name_pt,brands,image_front_url,quantity",
    ): Response<OffResponse>

    companion object {
        const val BASE_URL = "https://world.openfoodfacts.org/"
    }
}

@Serializable
data class OffResponse(
    val status: Int = 0,
    val product: OffProduct? = null,
)

@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_pt") val productNamePt: String? = null,
    val brands: String? = null,
    @SerialName("image_front_url") val imageFrontUrl: String? = null,
    val quantity: String? = null,
)

/** Converte a resposta do OFF; null quando não há um nome utilizável. */
fun OffProduct.toProductInfo(barcode: String): ProductInfo? {
    val baseName = productNamePt?.takeIf { it.isNotBlank() } ?: productName?.takeIf { it.isNotBlank() } ?: return null
    // "Arroz Tipo 1" + "5 kg" → "Arroz Tipo 1 5 kg" (só se a quantidade ainda não estiver no nome)
    val qty = quantity?.trim()?.takeIf { it.isNotEmpty() && !baseName.contains(it, ignoreCase = true) }
    return ProductInfo(
        barcode = barcode,
        name = listOfNotNull(baseName.trim(), qty).joinToString(" "),
        brand = brands?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
        imageUrl = imageFrontUrl?.takeIf { it.isNotBlank() },
        source = ProductSource.OPEN_FOOD_FACTS,
    )
}

class OpenFoodFactsSource @Inject constructor(
    private val api: OpenFoodFactsApi,
) : ProductInfoSource {
    override val name = "Open Food Facts"

    override suspend fun lookup(barcode: String): ProductInfo? {
        val response = api.product(barcode)
        if (response.code() == 404) return null
        if (!response.isSuccessful) throw HttpException(response)
        val body = response.body() ?: return null
        if (body.status != 1) return null
        return body.product?.toProductInfo(barcode)
    }
}
