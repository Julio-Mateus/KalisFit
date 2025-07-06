package com.jcmateus.kalisfit.model

import com.google.firebase.Timestamp


data class Product(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String? = null,

    var priceValue: Long = 0L,
    var priceDisplay: String = "",
    var currencyCode: String = "COP",

    var stock: Int? = null,
    var disponible: Boolean = true,
    var imagenUrl: String? = null,

    var categorias: List<String> = emptyList(),
    var tallasDisponibles: List<String> = emptyList(),
    var coloresDisponibles: List<String> = emptyList(),

    var material: String? = null,
    var marca: String? = null,
    var etiquetas: List<String> = emptyList(),
    var destacado: Boolean = false,
    var sku: String? = null,
    var envioGratis: Boolean = false,
    var tipoProducto: String? = null,
    var fechaCreacion: Timestamp? = null // Usar Timestamp de Firestore
) {
    // Constructor sin argumentos requerido por Firestore para la deserialización
    constructor() : this(
        id = "",
        nombre = "",
        descripcion = null,
        priceValue = 0L,
        priceDisplay = "",
        currencyCode = "COP",
        stock = null,
        disponible = true,
        imagenUrl = null,
        categorias = emptyList(),
        tallasDisponibles = emptyList(),
        coloresDisponibles = emptyList(),
        material = null,
        marca = null,
        etiquetas = emptyList(),
        destacado = false,
        sku = null,
        envioGratis = false,
        tipoProducto = null,
        fechaCreacion = null // Inicializar Timestamp como null
    )
}
