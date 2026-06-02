package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.NewsArticle
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiNewsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestNews(currentDateStr: String): List<NewsArticle> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiNewsService", "API Key is missing or default. Returning simulated background news.")
            return generateMockNews()
        }

        val prompt = """
            Actúa como un sofisticado agente de búsqueda de noticias globales de alta fidelidad. 
            Hoy es $currentDateStr. Debes buscar o simular de forma realista las últimas 12 noticias de alto impacto más importantes del momento:
            - 4 noticias de la sección 'Mundo' (usando fuentes como Reuters, BBC News, Associated Press, Al Jazeera, o CNN).
            - 4 noticias de la sección 'Alemania' (usando fuentes como Tagesschau, Deutsche Welle (DW), Der Spiegel, o Süddeutsche Zeitung).
            - 4 noticias de la sección 'Colombia' (usando fuentes como El Tiempo, El Espectador, Caracol Radio, o Revista Semana).

            Para cada una, genera un artículo completo con un título llamativo y relevante de la actualidad de 2026, un resumen breve y un contenido detallado (2 párrafos realistas). 
            Asigna un nivel de impacto ('HIGH', 'MEDIUM', o 'LOW') de forma selectiva. Al menos 2 o 3 titulares de las 12 deben ser 'HIGH' de alto impacto para activar las notificaciones push de sistema.

            Debes retornar STRICTLY un arreglo JSON de objetos sin ningún tipo de explicación adicional, formato de código markdown (no incluyas ```json al principio ni al final), ni saludos. El JSON debe ser parseable directamente.

            La estructura exacta de cada objeto del arreglo JSON debe ser:
            {
              "title": "Título llamativo en español",
              "description": "Breve descripción corta (resumen) de la noticia en español",
              "content": "Contenido detallado y profundo de la noticia en español, explicando causas y consecuencias (2 párrafos)",
              "section": "Mundo" o "Alemania" o "Colombia",
              "source": "Nombre del medio informativo original, ej. 'Tagesschau', 'Reuters'",
              "impactLevel": "HIGH" o "MEDIUM" o "LOW",
              "imageUrl": "URL de imagen real de alta resolución de Unsplash temática de la noticia (ej. https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&w=800&q=80)",
              "videoUrl": "Opcional. URL de un archivo de video MP4 público como 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4' o 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' para simular cobertura (dejar nulo o vacío para la mayoría)"
            }
        """.trimIndent()

        // Construct request body according to the direct Gemini API schema
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }
            put("contents", contentsArray)
            
            // Configure model to respond in JSON mode
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(mediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiNewsService", "API request failed with code: ${response.code}. Payload: ${response.body?.string()}")
                    return generateMockNews()
                }

                val responseBodyStr = response.body?.string() ?: return generateMockNews()
                val jsonResponse = JSONObject(responseBodyStr)
                
                val textResponse = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                parseResponseJson(textResponse)
            }
        } catch (e: Exception) {
            Log.e("GeminiNewsService", "Error calling Gemini News API: ${e.message}", e)
            generateMockNews()
        }
    }

    private fun parseResponseJson(jsonStr: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
            // Check if there are any trailing markdown markers
            var cleanJson = jsonStr.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.removePrefix("```json")
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.removePrefix("```")
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.removeSuffix("```")
            }
            cleanJson = cleanJson.trim()

            val jsonArray = if (cleanJson.startsWith("[")) {
                JSONArray(cleanJson)
            } else {
                // If it is wrapped in an object
                val wrapper = JSONObject(cleanJson)
                if (wrapper.has("articles")) {
                    wrapper.getJSONArray("articles")
                } else if (wrapper.has("news")) {
                    wrapper.getJSONArray("news")
                } else {
                    return generateMockNews()
                }
            }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "Titular de Actualidad")
                val description = obj.optString("description", "Resumen de la noticia de último minuto.")
                val content = obj.optString("content", "Detalles completos de la noticia actual.")
                val section = obj.optString("section", "Mundo")
                val source = obj.optString("source", "Fuente de Información")
                val impactLevel = obj.optString("impactLevel", "MEDIUM").uppercase()
                val imageUrl = obj.optString("imageUrl", "").let { if (it.isBlank() || it == "null") null else it }
                val videoUrl = obj.optString("videoUrl", "").let { if (it.isBlank() || it == "null") null else it }

                articles.add(
                    NewsArticle(
                        title = title,
                        description = description,
                        content = content,
                        section = section,
                        source = source,
                        timestamp = System.currentTimeMillis() - i * 120000L, // Slightly offset so they sort cleanly
                        impactLevel = impactLevel,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiNewsService", "Error parsing response JSON: ${e.message}", e)
            return generateMockNews()
        }
        return articles
    }

    // High fidelity fallback mockup when API key is missing or network fails
    fun generateMockNews(): List<NewsArticle> {
        val now = System.currentTimeMillis()
        return listOf(
            // World
            NewsArticle(
                title = "Cumbre Global de Energía Renovable 2026 sella Acuerdo de Cero Emisiones",
                description = "Líderes de más de 120 países firman un acuerdo histórico para triplicar la energía solar y eólica hacia el fin de la década.",
                content = "La cumbre de París concluuyó con una resolución sin precedentes donde las potencias industriales acordaron subsidios masivos para reemplazar combustibles fósiles. El acuerdo incluye el establecimiento de un fondo global de 100 mil millones de dólares para naciones en desarrollo.\n\nExpertos aseguran que esta cumbre redefine el panorama económico mundial para la próxima década, acelerando la transición energética.",
                section = "Mundo",
                source = "Reuters",
                timestamp = now,
                impactLevel = "HIGH",
                imageUrl = "https://images.unsplash.com/photo-1466611653911-95081537e5b7?auto=format&fit=crop&w=800&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            ),
            NewsArticle(
                title = "Alianza Científica anuncia el mayor avance en fusión nuclear comercial",
                description = "Científicos logran sostener una ganancia neta de energía de fusión por más de 12 horas seguidas.",
                content = "La de hoy es una jornada que reescribe el futuro de la física y la ingeniería y revoluciona las expectativas de suministro energético comercial autónomo.\n\nSe espera que los primeros prototipos de reactores termo-nucleares de fusión entren en funcionamiento a principios de los años 30.",
                section = "Mundo",
                source = "BBC News",
                timestamp = now - 3600000L,
                impactLevel = "MEDIUM",
                imageUrl = "https://images.unsplash.com/photo-1507668077129-56e32842fceb?auto=format&fit=crop&w=800&q=80"
            ),
            NewsArticle(
                title = "Mercados mundiales reaccionan con optimismo tras anuncios de estabilidad fiscal",
                description = "Bolsas de Tokio, Londres y Nueva York registran alzas del 2% tras la estabilización de tasas bancarias.",
                content = "Las decisiones coordinadas por los bancos centrales han generado un clima de tranquilidad financiera. Esta medida combate la inflación y asegura fondos de liquidez para pymes.\n\nInversores proyectan que este sea el inicio de un periodo de expansión estable.",
                section = "Mundo",
                source = "Associated Press",
                timestamp = now - 7200000L,
                impactLevel = "LOW",
                imageUrl = "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?auto=format&fit=crop&w=800&q=80"
            ),
            NewsArticle(
                title = "Nuevos telescopios espaciales captan las primeras señales de vapor de agua en exoplanetas cercanos",
                description = "La misión conjunta de detección espacial reporta indicios de atmósfera habitable en el sistema Gliese.",
                content = "Los análisis espectrales confirman la presencia masiva de vapor de agua en la atmósfera superior del planeta. Esto abre una nueva vía de investigación sobre la vida exoplanetaria.\n\nLos científicos ya organizan misiones de monitoreo profundo continuo.",
                section = "Mundo",
                source = "Al Jazeera",
                timestamp = now - 10800000L,
                impactLevel = "LOW",
                imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=800&q=80"
            ),

            // Germany
            NewsArticle(
                title = "Alemania aprueba reforma integral para digitalizar la burocracia federal",
                description = "El Bundestag aprueba un paquete legal masivo que permite realizar el 98% de trámites estatales en línea.",
                content = "La histórica reforma, impulsada por ministerios de innovación y modernización, promete ahorrar más de 15 mil millones de euros anuales al sector público y privado. A partir del próximo trimestre, las firmas digitales serán equivalentes a las físicas en cualquier trámite federal.\n\nOpositores y defensores celebran esto como el mayor salto de modernización de Alemania en el siglo.",
                section = "Alemania",
                source = "Tagesschau",
                timestamp = now - 500000L,
                impactLevel = "HIGH",
                imageUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?auto=format&fit=crop&w=800&q=80"
            ),
            NewsArticle(
                title = "Transición Industrial: Fabricantes automotores alemanes logran récord de ventas eléctricas",
                description = "Las marcas alemanas reportan un crecimiento de ventas del 34% en el mercado de vehículos eléctricos.",
                content = "Los datos agregados del mercado automotor demuestran un éxito de adaptación industrial frente a la competencia asiática y norteamericana. La inversión estatal en electrolineras facilitó este despegue masivo.\n\nInversores de Múnich estiman que Alemania recupera el liderazgo en innovación de movilidad.",
                section = "Alemania",
                source = "Deutsche Welle (DW)",
                timestamp = now - 4100000L,
                impactLevel = "MEDIUM",
                imageUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=800&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"
            ),
            NewsArticle(
                title = "Berlín se consagra como la capital de las startups de Inteligencia Artificial en Europa",
                description = "Más de 500 nuevas empresas de base tecnológica se establecieron en la capital alemana en lo que va del año.",
                content = "Un ecosistema de talento universitario, capital de riesgo y regulaciones claras ha atraído a programadores de todo el mundo. Berlín lidera ahora la inversión privada en tecnología y automatización en la Unión Europea.\n\nLa ciudad planea crear distritos tecnológicos especializados para pymes.",
                section = "Alemania",
                source = "Der Spiegel",
                timestamp = now - 8200000L,
                impactLevel = "MEDIUM",
                imageUrl = "https://images.unsplash.com/photo-1677442136019-21780efad99a?auto=format&fit=crop&w=800&q=80"
            ),
            NewsArticle(
                title = "Múnich acoge la mayor feria cultural de innovación y sostenibilidad de Baviera",
                description = "Se espera la visita de más de medio millón de personas para debates y presentaciones artísticas.",
                content = "El evento reúne arte contemporáneo e iniciativas para rediseñar espacios urbanos verdes. Museos locales presentan exhibiciones inmersivas e interactivas abiertas al público.\n\nLa organización celebra la alta participación juvenil de esta edición.",
                section = "Alemania",
                source = "Süddeutsche Zeitung",
                timestamp = now - 12200000L,
                impactLevel = "LOW",
                imageUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=800&q=80"
            ),

            // Colombia
            NewsArticle(
                title = "Colombia anuncia plan de infraestructura férrea nacional de última generación",
                description = "El gobierno nacional destina fondos históricos para revivir el tren de carga y pasajeros que conectará el interior con los puertos del Caribe.",
                content = "El ambicioso plan ferroviario busca reducir costos logísticos en un 40% y reactivar economías locales de Boyacá, Antioquia, Caldas y la Costa Atlántica. Las locomotoras serán impulsadas con hidrógeno verde.\n\nGremios empresariales aplauden el anuncio como el proyecto de conectividad física más importante del siglo en Colombia.",
                section = "Colombia",
                source = "El Tiempo",
                timestamp = now - 1500000L,
                impactLevel = "HIGH",
                imageUrl = "https://images.unsplash.com/photo-1474487548417-781cb71495f3?auto=format&fit=crop&w=800&q=80",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            NewsArticle(
                title = "Bogotá y Medellín se consolidan como hubs de exportación de software y videojuegos",
                description = "Empresas creadas en Colombia expanden sus ventas mundiales superando los 400 millones de dólares de exportación anual.",
                content = "El talento nacional en desarrollo de software, modelado 3D y diseño interactivo ha captado la atención de grandes marcas globales. Programas de becas e incentivos tributarios impulsan el empleo local de alta tecnología.\n\nLa Cámara de Comercio proyecta duplicar la fuerza laboral técnica en un año.",
                section = "Colombia",
                source = "El Espectador",
                timestamp = now - 5500000L,
                impactLevel = "MEDIUM",
                imageUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=800&q=80"
            ),
            NewsArticle(
                title = "Biodiversidad: Parque Nacional Natural Serranía de Chiribiquete estrena centro de investigación avanzada",
                description = "Con alianzas internacionales, se abre un centro científico dedicado a descifrar especies biológicas inéditas.",
                content = "El parque, catalogado como patrimonio mundial, contará con científicos residentes enfocados en la preservación de la selva virgen de la Amazonía. Los datos recolectados se publicarán de forma abierta.\n\nEste hito posiciona a Colombia como pionera de bio-fármacos sostenibles.",
                section = "Colombia",
                source = "Revista Semana",
                timestamp = now - 9500000L,
                impactLevel = "LOW",
                imageUrl = "https://images.unsplash.com/photo-1516026672322-bc52d61a5555?auto=format&fit=crop&w=800&q=80"
            ),
            NewsArticle(
                title = "El café especial colombiano bate récords en subasta internacional de Londres",
                description = "Lote cultivado por caficultores del Huila alcanza una valoración récord de 150 dólares por libra.",
                content = "El lote, destacado por aromas frutales sutiles y acidez equilibrada, fue adquirido por una tostadora asiática de prestigio. El mercado internacional premia los procesos amigables con el medio ambiente.\n\nCaficultores locales expresaron orgullo y alegría por este reconocimiento de calidad.",
                section = "Colombia",
                source = "Caracol Radio",
                timestamp = now - 13500000L,
                impactLevel = "LOW",
                imageUrl = "https://images.unsplash.com/photo-1447933601403-0c6688de566e?auto=format&fit=crop&w=800&q=80"
            )
        )
    }
}
