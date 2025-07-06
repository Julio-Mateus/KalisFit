package com.jcmateus.kalisfit.util

import com.google.firebase.firestore.Query
object FirestoreCollections {
    const val PRODUCTS = "products"
    // Si tienes otras colecciones principales, puedes añadirlas aquí.
    // const val USERS = "users"
    // const val ORDERS = "orders"
}

object FirestoreFields {
    // Campos comunes de la colección 'products' que usarás en consultas
    const val DISPONIBLE = "disponible"
    const val FECHA_CREACION = "fechaCreacion"
    const val SLUG = "slug"
    const val CATEGORIAS = "categorias"
    const val DESTACADO = "destacado"
    const val NOMBRE = "nombre" // Para ordenamiento por nombre, si lo necesitas
    // Añade otros campos por los que vayas a filtrar u ordenar frecuentemente
}

// Opcional: Para direcciones de ordenamiento comunes
object FirestoreQueryDirections {
    val ASCENDING = Query.Direction.ASCENDING
    val DESCENDING = Query.Direction.DESCENDING
}