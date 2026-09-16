package com.familia.monitor

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

object ContactHelper {
    private const val TAG = "ContactHelper"

    /**
     * Verifica si un nombre o número de teléfono existe en los contactos del sistema.
     */
    fun isContactUnknown(context: Context, nameOrNumber: String?): Boolean {
        if (nameOrNumber.isNullOrBlank()) return false

        // Verificar si tenemos el permiso antes de consultar la base de datos
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permiso de lectura de contactos no otorgado. Asumiendo desconocido.")
            return true
        }

        try {
            val uri = ContactsContract.Contacts.CONTENT_URI
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(nameOrNumber)

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.count > 0) return false 
            }

            val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI
            val filterUri = android.net.Uri.withAppendedPath(phoneUri, android.net.Uri.encode(nameOrNumber))
            val phoneProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

            context.contentResolver.query(filterUri, phoneProjection, null, null, null)?.use { cursor ->
                if (cursor.count > 0) return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando contactos: ${e.message}")
        }

        return true 
    }
}
