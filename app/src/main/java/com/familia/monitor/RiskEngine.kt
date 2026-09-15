package com.familia.monitor

/**
 * Motor de reglas 100% local. El texto completo de la notificación NUNCA
 * sale del dispositivo salvo que coincida con un patrón de riesgo; en ese
 * caso se sube solo el fragmento relevante, no la conversación completa.
 */
object RiskEngine {

    enum class RiskLevel { NONE, LOW, MEDIUM, HIGH }

    data class RiskMatch(
        val level: RiskLevel,
        val category: String,
        val matchedFragment: String
    )

    // Patrones agrupados por categoría. Ajustables sin tocar el resto del código.
    // IMPORTANTE: Definir los patrones sin acentos ya que el texto se normaliza.
    private val patterns: Map<String, Pair<RiskLevel, List<Regex>>> = mapOf(
        "dinero_urgente" to (RiskLevel.HIGH to listOf(
            Regex("(?i)transfer[i]\\s?(me|dinero|plata)"),
            Regex("(?i)mand[a]\\s?(me\\s)?(dinero|plata|efectivo)"),
            Regex("(?i)pag[a]\\s?(me\\s)?(ahora|urgente|ya)")
        )),
        "amenaza" to (RiskLevel.HIGH to listOf(
            Regex("(?i)si no .* (le cuento|aviso|mando|publico)"),
            Regex("(?i)voy a (publicar|mandar|compartir) (tus|esas|las) fotos"),
            Regex("(?i)te vas a arrepentir")
        )),
        "secreto_forzado" to (RiskLevel.MEDIUM to listOf(
            Regex("(?i)no le digas a nadie"),
            Regex("(?i)es (nuestro|nuestro) secreto"),
            Regex("(?i)no se lo cuentes a tus padres")
        )),
        "pedido_de_foto" to (RiskLevel.MEDIUM to listOf(
            Regex("(?i)mandame\\s?(una)?\\s?foto"),
            Regex("(?i)mostrame\\s?(una)?\\s?foto"),
            Regex("(?i)prend[e]\\s?(la)?\\s?c[a]mara")
        )),
        "contacto_desconocido_persistente" to (RiskLevel.LOW to listOf(
            Regex("(?i)agregame\\s?(a)?\\s?otra\\s?red"),
            Regex("(?i)habl(emos|amos)\\s?por\\s?otro\\s?lado")
        )),
        "grooming" to (RiskLevel.HIGH to listOf(
            Regex("(?i)donde vivis"),
            Regex("(?i)estas solo"),
            Regex("(?i)pasame tu direccion"),
            Regex("(?i)no le cuentes a nadie")
        )),
        "sextorsion" to (RiskLevel.HIGH to listOf(
            Regex("(?i)tengo tus fotos"),
            Regex("(?i)voy a publicar el video"),
            Regex("(?i)borra el chat"),
            Regex("(?i)si no haces lo que digo")
        )),
        "citas_sospechosas" to (RiskLevel.MEDIUM to listOf(
            Regex("(?i)encontremonos"),
            Regex("(?i)te paso a buscar"),
            Regex("(?i)veni a mi casa")
        )),
        "robo_de_cuenta" to (RiskLevel.HIGH to listOf(
            Regex("(?i)pasame el codigo"),
            Regex("(?i)llego un SMS"),
            Regex("(?i)validar cuenta")
        ))
    )

    fun evaluate(text: String): RiskMatch? {
        val normalizedText = normalize(text)
        for ((category, rule) in patterns) {
            val (level, regexList) = rule
            for (regex in regexList) {
                val match = regex.find(normalizedText)
                if (match != null) {
                    return RiskMatch(
                        level = level,
                        category = category,
                        matchedFragment = truncate(match.value, 200)
                    )
                }
            }
        }
        return null
    }

    /**
     * Remueve acentos y diacríticos del texto para facilitar la detección.
     * Ejemplo: "Pásame tu dirección" -> "Pasame tu direccion"
     */
    private fun normalize(s: String): String {
        val temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
    }

    private fun truncate(s: String, max: Int) =
        if (s.length <= max) s else s.substring(0, max) + "…"
}
