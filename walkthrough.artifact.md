# Walkthrough - Blindaje Crítico en Español Neutro

Se ha reforzado masivamente el motor de reglas de riesgo para proteger a menores contra extorsión, grooming y manipulación, utilizando un lenguaje neutro para maximizar la efectividad en diferentes países.

## Categorías de Protección Añadidas

### Aislamiento y Manipulación (CRÍTICO)
*   Detecta órdenes para ocultar información: `"borra los mensajes"`, `"no le digas a nadie"`, `"que no se enteren"`, `"mientele a tus padres"`.

### Amenaza de Difusión (CRÍTICO)
*   Detecta extorsión basada en miedo social: `"lo subo a tiktok"`, `"tus amigos lo verán"`, `"lo verán en la escuela"`, `"tengo tu video"`.

### Grooming y Control Físico (CRÍTICO)
*   Detecta intrusiones a la privacidad y pedidos de contenido sensible: `"donde vives"`, `"estas sola"`, `"quítate la ropa"`, `"enséñame un poquito"`.

### Extorsión Digital (CRÍTICO)
*   Detecta el pedido de monedas de juegos populares: `"robux"`, `"diamantes"`, `"tarjetas de regalo"`, `"free fire"`.

## Mejoras Técnicas
*   **Neutralización de Idioma**: Se cambiaron regionalismos (como "mentile" o "tenes") por términos neutros ("mientele", "tienes") y se añadieron variantes para cubrir todas las posibilidades.
*   **Reglas Sin Acentos**: El motor ahora normaliza el texto (ej: "Pásame" -> "Pasame") antes de comparar, haciendo imposible que un atacante evada el filtro usando tildes.

## Verificación

1.  **Estado**: La aplicación actualizada ya se encuentra activa en el dispositivo.
2.  **Prueba Recomendada**: Envía un mensaje con la frase `"borra los mensajes es un secreto"`. El sistema lo clasificará como `aislamiento_manipulacion` de nivel **HIGH** y activará el aviso inmediato.

> [!IMPORTANT]
> **Acción Inmediata**: Ante cualquier alerta nivel HIGH recibida por estas nuevas categorías, se recomienda intervenir físicamente y asegurar el dispositivo del menor.
