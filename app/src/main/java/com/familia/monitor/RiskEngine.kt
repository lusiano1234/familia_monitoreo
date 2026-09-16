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
        "extorsion_dinero" to (RiskLevel.HIGH to listOf(
            Regex("(?i)transfer[i]\\s?(me|dinero|plata)"),
            Regex("(?i)envi[a]\\s?(me\\s)?(dinero|plata|efectivo)"),
            Regex("(?i)pag[a]\\s?(me\\s)?(ahora|urgente|ya)"),
            Regex("(?i)si no pagas"),
            Regex("(?i)quiero (dinero|plata|efectivo)")
        )),
        "amenaza_difusion" to (RiskLevel.HIGH to listOf(
            Regex("(?i)(ense[n]ar|mandar|subir|compartir) (tus|esas|las) (fotos|videos)"),
            Regex("(?i)lo (subo|pongo) a (tiktok|instagram|facebook|internet)"),
            Regex("(?i)tus amigos lo (veran|van a ver)"),
            Regex("(?i)en la escuela lo (saber|veran)"),
            Regex("(?i)te vas a arrepentir"),
            Regex("(?i)tengo tu (cara|video)")
        )),
        "aislamiento_manipulacion" to (RiskLevel.HIGH to listOf(
            Regex("(?i)borra (el chat|los mensajes|la conversacion)"),
            Regex("(?i)no le (digas|cuentes) a nadie"),
            Regex("(?i)es (nuestro|un) secreto"),
            Regex("(?i)(mientele|no le digas) a tus (padres|papas|papas)"),
            Regex("(?i)que no se enteren"),
            Regex("(?i)limpia el historial")
        )),
        "grooming_contacto" to (RiskLevel.HIGH to listOf(
            Regex("(?i)donde (vives|estas)"),
            Regex("(?i)estas (solo|sola)"),
            Regex("(?i)pasame tu (direccion|ubicacion|casa)"),
            Regex("(?i)sacate (la ropa|la playera|la camisa|la remera)"),
            Regex("(?i)quitate (la ropa|la playera|la camisa|la remera)"),
            Regex("(?i)mostrame algo"),
            Regex("(?i)ensename un poquito")
        )),
        "citas_sospechosas" to (RiskLevel.HIGH to listOf(
            Regex("(?i)(encontremonos|veamonos)"),
            Regex("(?i)te (paso a buscar|recojo)"),
            Regex("(?i)veni a mi casa"),
            Regex("(?i)vamos a un lugar")
        )),
        "extorsion_digital" to (RiskLevel.HIGH to listOf(
            Regex("(?i)(robux|diamantes|free fire|fortnite)"),
            Regex("(?i)(codigos|tarjetas) de regalo"),
            Regex("(?i)gift\\s?card"),
            Regex("(?i)comprame (diamantes|monedas)")
        )),
        "robo_de_cuenta" to (RiskLevel.HIGH to listOf(
            Regex("(?i)pasame el codigo"),
            Regex("(?i)llego un (SMS|mensaje)"),
            Regex("(?i)validar (cuenta|perfil)")
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
     */
    private fun normalize(s: String): String {
        val temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
    }

    private fun truncate(s: String, max: Int) =
        if (s.length <= max) s else s.substring(0, max) + "…"
}
