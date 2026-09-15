package com.familia.monitor

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object ContactHelper {
    private const val TAG = "ContactHelper"

    /**
     * Verifica si un nombre o número de teléfono existe en los contactos del sistema.
     * Requiere el permiso READ_CONTACTS otorgado.
     */
    fun isContactUnknown(context: Context, nameOrNumber: String?): Boolean {
        if (nameOrNumber.isNullOrBlank()) return false

        try {
            val uri = ContactsContract.Contacts.CONTENT_URI
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(nameOrNumber)

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.count > 0) {
                    return false // Encontrado por nombre
                }
            }

            // Si no se encontró por nombre exacto, intentamos buscar por número de teléfono
            val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI
            val filterUri = android.net.Uri.withAppendedPath(phoneUri, android.net.Uri.encode(nameOrNumber))
            val phoneProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

            context.contentResolver.query(filterUri, phoneProjection, null, null, null)?.use { cursor ->
                if (cursor.count > 0) {
                    return false // Encontrado por número
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar contactos: ${e.message}")
        }

        return true // No se encontró en la agenda
    }
}
