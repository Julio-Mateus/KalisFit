package com.jcmateus.kalisfit.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class CartItem(
    var productId: String = "",
    var nombre: String = "",
    var priceDisplay: String = "",
    var priceValue: Long = 0L, // Mantén consistencia con tu modelo Product
    var currencyCode: String = "COP",
    var imagenUrl: String? = null,
    var cantidad: Int = 0,
    var tallaSeleccionada: String? = null,
    var colorSeleccionado: String? = null,
    @ServerTimestamp // Para que Firestore ponga la fecha del servidor al crear/actualizar
    val addedAt: Timestamp? = null,
    // No necesitas un ID separado aquí si el ID del documento en Firestore será el productId
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(
        "", "", "", 0L, "COP", null, 0, null, null, null
    )
}
