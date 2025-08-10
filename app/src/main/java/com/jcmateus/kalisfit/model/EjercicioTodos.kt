package com.jcmateus.kalisfit.model


enum class Equipamiento(val displayName: String) {
    NINGUNO("Ninguno"), // Si la lista está vacía, se puede asumir NINGUNO
    MANCUERNAS("Mancuernas"),
    BARRA_DOMINADAS("Barra de Dominadas"),
    PARALELAS("Paralelas"),
    BANCO("Banco"),
    BANDA("Banda de Resistencia"),
    RUEDA_ABDOMINAL("Rueda Abdominal"),
    OTRO("Otro");

    companion object {
        fun fromString(value: String): Equipamiento? =
            values().find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
    }
}
data class EjercicioTodos(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var imagenUrl: String? = null,
    var grupoMuscular: List<String> = emptyList(),
    // Para equipamientoNecesario, si la lista está vacía en JSON, se mapeará a emptyList()
    var equipamientoNecesario: List<String> = emptyList(),
    var lugarEntrenamiento: List<String> = emptyList(),
    var esUnilateral: Boolean = false
) {
    // Constructor sin argumentos requerido por Firestore/Gson
    constructor() : this("", "", "", null, emptyList(), emptyList(), emptyList(), false)

    // --- Funciones Helper para trabajar con Enums (Opcional pero recomendado) ---
    fun getEquipamientoEnum(): List<Equipamiento> {
        return if (equipamientoNecesario.isEmpty()) {
            listOf(Equipamiento.NINGUNO)
        } else {
            equipamientoNecesario.mapNotNull { Equipamiento.fromString(it) }
        }
    }

    fun getLugarEntrenamientoEnum(): List<LugarEntrenamiento> { // Asumiendo que tienes LugarEntrenamiento
        return lugarEntrenamiento.mapNotNull { nombreLugar ->
            try {
                LugarEntrenamiento.valueOf(nombreLugar.uppercase()) // Ajusta si tu enum tiene otros nombres
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}