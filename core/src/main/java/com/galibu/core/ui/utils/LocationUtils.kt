package com.galibu.core.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Resultado detallado de una resolución GPS: texto + división política detectada.
 *
 * Vive en core para que autenticación, dashboard cliente y perfil profesional
 * usen exactamente la misma detección, sin copias por módulo.
 */
data class GpsFix(
    val address: String,
    val adminArea: String? = null,
    val locality: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * Busca [wanted] (p. ej. del geocoder) dentro del catálogo y devuelve el nombre
 * canónico. Tolera mayúsculas y diferencias menores ("Bogotá D.C." vs "Bogotá").
 */
fun matchCatalogName(candidates: List<String>, wanted: String?): String? {
    if (wanted.isNullOrBlank()) return null
    val w = wanted.trim()
    candidates.firstOrNull { it.equals(w, ignoreCase = true) }?.let { return it }
    val normW = w.lowercase().replace(".", "").replace("d c", "").trim()
    return candidates.firstOrNull {
        val normC = it.lowercase().replace(".", "").replace("d c", "").trim()
        normC == normW || normC.contains(normW) || normW.contains(normC)
    }
}

fun isDeviceLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return try {
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) {
        false
    }
}

@Suppress("DEPRECATION")
fun getCurrentGpsLocation(
    context: Context,
    department: String,
    municipality: String,
    onLocationFetched: (String) -> Unit,
    onGpsUnavailable: (() -> Unit)? = null,
    onDetailedFix: ((GpsFix) -> Unit)? = null
) {
    val fallback = "Calle 100 # 15-22, $municipality, $department"
    fun notifyUnavailable() {
        if (onGpsUnavailable != null) onGpsUnavailable() else onLocationFetched(fallback)
    }
    // Geocodifica un fix a texto + departamento/municipio detectados.
    fun resolveFix(latitude: Double, longitude: Double): GpsFix {
        val addr = try {
            Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)?.firstOrNull()
        } catch (e: Exception) {
            null
        }
        val line = addr?.getAddressLine(0)
        return GpsFix(
            address = if (!line.isNullOrBlank()) line else fallback,
            adminArea = addr?.adminArea?.takeIf { it.isNotBlank() },
            locality = addr?.locality?.takeIf { it.isNotBlank() }
                ?: addr?.subAdminArea?.takeIf { it.isNotBlank() },
            latitude = latitude,
            longitude = longitude
        )
    }
    fun deliver(fix: GpsFix) {
        onDetailedFix?.invoke(fix)
        onLocationFetched(fix.address)
    }
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        notifyUnavailable()
        return
    }

    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFine && !hasCoarse) {
        notifyUnavailable()
        return
    }

    // Sin ningún proveedor activo (GPS y red apagados) nunca llegará un fix:
    // avisar de una vez en lugar de quedarse esperando.
    if (!isDeviceLocationEnabled(context)) {
        notifyUnavailable()
        return
    }

    var delivered = false
    val handler = android.os.Handler(android.os.Looper.getMainLooper())

    try {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = try {
                locationManager.getLastKnownLocation(provider)
            } catch (e: Exception) {
                null
            } ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        if (bestLocation != null) {
            val fix = resolveFix(bestLocation.latitude, bestLocation.longitude)
            delivered = true
            deliver(fix)
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (delivered) return
                delivered = true
                val fix = resolveFix(location.latitude, location.longitude)
                try {
                    locationManager.removeUpdates(this)
                } catch (e: Exception) {}
                deliver(fix)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }
        try {
            locationManager.requestSingleUpdate(provider, listener, null)
        } catch (e: Exception) {
            if (!delivered) {
                delivered = true
                notifyUnavailable()
            }
            return
        }

        // Timeout: si no llega ningún fix, liberar y avisar (antes se quedaba cargando).
        handler.postDelayed({
            if (!delivered) {
                delivered = true
                try {
                    locationManager.removeUpdates(listener)
                } catch (e: Exception) {}
                notifyUnavailable()
            }
        }, 10000)
    } catch (e: Exception) {
        if (!delivered) {
            delivered = true
            notifyUnavailable()
        }
    }
}
