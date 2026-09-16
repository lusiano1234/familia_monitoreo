# Plan de Implementación: Refuerzo Crítico del Motor de Riesgo (Protección Infantil)

Este plan tiene como objetivo expandir masivamente el motor de reglas local para detectar situaciones de extorsión, grooming y aislamiento, específicamente diseñado para la protección de menores de edad.

## User Review Required

> [!CAUTION]
> **Sensibilidad de los datos**: Estas reglas son más agresivas y podrían generar algunos falsos positivos. Sin embargo, en un contexto de extorsión real, es preferible pecar de precavido. El sistema seguirá operando 100% localmente y solo subirá el fragmento que disparó la alerta.

## Cambios Propuestos

### 1. Expansión del Diccionario de Riesgo

#### [MODIFY] [RiskEngine.kt](file:///C:/Users/LENOVO SERIES PRO/Desktop/android/android/app/src/main/java/com/familia/monitor/RiskEngine.kt)
Se añadirán y expandirán las siguientes categorías con patrones de lenguaje natural:

- **Aislamiento y Secretismo (Nivel HIGH)**:
    - Patrones: "tus papas no estan", "borra el chat", "borra los mensajes", "limpia la charla", "nadie tiene que saber", "mentile a tus papas", "que no se enteren".
- **Amenazas de Difusión/Doxing (Nivel HIGH)**:
    - Patrones: "se lo mando a tus amigos", "lo subo a tiktok", "lo subo a instagram", "toda la escuela lo va a ver", "se a que escuela vas", "tengo tu cara en el video".
- **Insinuaciones de "Juegos" Peligrosos (Nivel HIGH)**:
    - Patrones: "jugamos a un reto", "verdad o consecuencia", "sacate la remera", "mostrame un poquito", "estamos solos vos y yo".
- **Control Físico/Presencia (Nivel HIGH)**:
    - Patrones: "estas en tu cuarto", "que ropa tenes puesta", "estoy afuera", "veni a la esquina", "quedamos en vernos".
- **Extorsión Económica Específica (Nivel HIGH)**:
    - Patrones: "paga o subo", "quiero tarjetas de regalo", "mandame codigos", "comprame diamantes", "si no pagas ya sabes".

### 2. Mejora en la Normalización
- Se ajustarán las expresiones regulares para ser más flexibles ante variaciones comunes en el chat (uso de "k" por "que", omisión de espacios, etc.).

## Plan de Verificación

### Pruebas Manuales (Simulacros)
1.  **Aislamiento**: Enviar "borra el chat que es secreto" -> Verificar alerta `aislamiento_secretismo`.
2.  **Difusión**: Enviar "lo voy a subir a tiktok" -> Verificar alerta `amenaza_difusion`.
3.  **Extorsión**: Enviar "mandame codigos de roblox o publico" -> Verificar alerta `extorsion_infantil`.
4.  **Presencia**: Enviar "estas sola en tu pieza" -> Verificar alerta `grooming_avanzado`.

---

**¿Deseas que aplique este refuerzo masivo de seguridad ahora mismo?**
