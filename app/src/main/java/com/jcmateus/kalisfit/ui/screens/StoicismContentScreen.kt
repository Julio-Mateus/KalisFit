package com.jcmateus.kalisfit.ui.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.jcmateus.kalisfit.model.StoicExerciseDefinition
import com.jcmateus.kalisfit.model.StoicExerciseType
import com.jcmateus.kalisfit.model.StoicModule
import com.jcmateus.kalisfit.model.UserStoicProgress
import com.jcmateus.kalisfit.viewmodel.StoicismViewModel
import androidx.compose.runtime.getValue
import com.jcmateus.kalisfit.model.UserExerciseResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoicismContentScreen(
    mainNavController: NavHostController, // No se usa directamente aquí
    viewModel: StoicismViewModel = viewModel()
) {
    val screenState by viewModel.screenState.collectAsState()

    when {
        screenState.isLoadingModules || screenState.isLoadingProgress -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        screenState.errorMessage != null -> {
            // Considera usar un Snackbar o un diálogo para errores no bloqueantes,
            // o un diseño más específico para errores que impiden la carga.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Error: ${screenState.errorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        screenState.activeModule != null -> {
            val activeModule = screenState.activeModule!! // Sabemos que no es null aquí
            ActiveStoicModuleView(
                module = activeModule,
                userProgress = screenState.userProgress, // Lo pasamos por si lo necesitas en el futuro
                currentExerciseResponses = screenState.currentExerciseResponses,
                onUpdateExerciseResponse = { exerciseId, responseValue, exerciseType ->
                    viewModel.updateExerciseResponse(
                        exerciseId = exerciseId,
                        responseValue = responseValue,
                        exerciseType = exerciseType,
                        moduleId = activeModule.id
                    )
                },
                onCompleteModule = { moduleId ->
                    viewModel.completeModule(moduleId)
                },
                onReviewModule = { moduleId ->
                    viewModel.reviewModule(moduleId)
                }
                // Si quieres la lista de módulos al final de ActiveStoicModuleView, necesitarás pasarla:
                // allModulesForReviewList = screenState.modules
            )
        }

        else -> {
            // Todos los módulos completados o no hay módulos disponibles (y no hay error)
            AllModulesCompletedView(
                modules = screenState.modules,
                userProgress = screenState.userProgress,
                onReviewModule = { moduleId ->
                    viewModel.reviewModule(moduleId)
                }
            )
        }
    }
}


@Composable
fun ActiveStoicModuleView(
    module: StoicModule,
    userProgress: UserStoicProgress?, // Mantenido por si se usa en el futuro
    currentExerciseResponses: Map<String, UserExerciseResponse>,
    onUpdateExerciseResponse: (exerciseId: String, responseValue: String, exerciseType: StoicExerciseType) -> Unit,
    onCompleteModule: (moduleId: String) -> Unit,
    onReviewModule: (moduleId: String) -> Unit
    // allModulesForReviewList: List<StoicModule> = emptyList() // Descomentar si implementas la lista al final
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Título del Módulo
        Text(
            text = module.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(vertical = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // 2. Introducción del Módulo
        if (module.introduction.isNotBlank()) {
            SectionContent(text = module.introduction)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Contenido Teórico
        module.theoryContent.forEach { paragraph ->
            if (paragraph.subtitle != null) {
                SectionTitle(title = paragraph.subtitle)
            }
            SectionContent(text = paragraph.text)
        }
        if (module.theoryContent.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))

        // 4. Citas
        module.quotes.forEach { quote ->
            QuoteCard(quote = quote.text, author = quote.author)
        }
        if (module.quotes.isNotEmpty()) Spacer(modifier = Modifier.height(16.dp))

        // 5. Ejercicios Prácticos
        module.exercises.forEach { exerciseDef ->
            StoicExerciseItem(
                exercise = exerciseDef,
                currentUserResponseObject = currentExerciseResponses[exerciseDef.id],
                onResponseChanged = { responseValue -> // responseValue es el String crudo de la UI
                    onUpdateExerciseResponse(exerciseDef.id, responseValue, exerciseDef.type)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onCompleteModule(module.id) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = module.exercises.all { exDef ->
                // Los ejercicios no obligatorios no impiden completar el módulo.
                // Para los obligatorios, deben tener una respuesta considerada "completa".
                if (!exDef.isRequired) {
                    true
                } else {
                    val response = currentExerciseResponses[exDef.id]
                    // Usar la función del modelo UserExerciseResponse
                    response?.isConsideredRespondedOrCompleted(exDef) == true
                }
            }
        ) {
            Text("Marcar como Completado y Continuar")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Opcional: Lista de todos los módulos para repaso rápido desde el módulo activo
        // Text(
        //     "Repasar otros módulos:",
        //     style = MaterialTheme.typography.titleMedium,
        //     modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
        // )
        // allModulesForReviewList.forEach { m ->
        //     // Asegúrate de tener userProgress no nulo si accedes a completedModuleIds
        //     val isCompleted = userProgress?.completedModuleIds?.contains(m.id) == true
        //     ModuleListItem(
        //         module = m,
        //         isCompleted = isCompleted,
        //         onClick = { onReviewModule(m.id) }
        //     )
        // }
        // Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StoicExerciseItem(
    exercise: StoicExerciseDefinition,
    currentUserResponseObject: UserExerciseResponse?,
    onResponseChanged: (responseValue: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Para poner "Obligatorio" a la derecha
            ) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp) // Dar peso para que el título no empuje "Obligatorio"
                )
                if (exercise.isRequired) {
                    Text(
                        "Obligatorio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary, // O un color que elijas para destacar
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp)) // Espacio entre título y descripción

            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when (exercise.type) {
                StoicExerciseType.REFLECTION_TEXT -> {
                    OutlinedTextField(
                        value = currentUserResponseObject?.responseText ?: "",
                        onValueChange = onResponseChanged,
                        label = { Text(exercise.questionPrompt ?: "Tu reflexión...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 5
                    )
                }

                StoicExerciseType.MULTIPLE_CHOICE -> {
                    exercise.questionPrompt?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    exercise.choices?.forEachIndexed { index, choice ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onResponseChanged(index.toString()) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentUserResponseObject?.selectedChoiceIndex == index,
                                onClick = { onResponseChanged(index.toString()) }
                            )
                            Text(
                                text = choice,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                StoicExerciseType.ACTION_PROMPT -> {
                    val isCompleted = currentUserResponseObject?.completed == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onResponseChanged((!isCompleted).toString()) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { newCheckedState ->
                                onResponseChanged(newCheckedState.toString())
                            }
                        )
                        Text(
                            text = exercise.questionPrompt ?: "Marca si has realizado esta acción.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                            // Opcional: Cambiar estilo de fuente si está completado
                            // fontStyle = if (isCompleted) FontStyle.Normal else FontStyle.Italic
                        )
                    }
                    if (isCompleted) {
                        Text(
                            "Completado", // O "¡Bien hecho!"
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                        )
                    }
                }

                StoicExerciseType.CHECKLIST -> {
                    exercise.questionPrompt?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    exercise.items?.forEachIndexed { itemIndex, itemText ->
                        // Obtener el estado 'checked' del item específico
                        val isChecked = currentUserResponseObject?.checklistResponses?.getOrNull(itemIndex) == true
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // Enviar "itemIndex:newState" al ViewModel
                                    onResponseChanged("$itemIndex:${!isChecked}")
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { newCheckedState ->
                                    onResponseChanged("$itemIndex:$newCheckedState")
                                }
                            )
                            Text(
                                text = itemText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    // Opcional: Mostrar un mensaje si el checklist completo se considera 'completed'
                    // (según la lógica del ViewModel que actualiza UserExerciseResponse.completed)
                    if (currentUserResponseObject?.completed == true) {
                        Text(
                            "Checklist completado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AllModulesCompletedView(
    modules: List<StoicModule>,
    userProgress: UserStoicProgress?,
    onReviewModule: (moduleId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Es buena idea tenerlo por si la lista es larga
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Para centrar el contenido si es poco
    ) {
        Text(
            "¡Felicidades!",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            "Has completado todos los módulos de introducción al Estoicismo. Esperamos que te hayan sido de utilidad.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Puedes repasar los módulos:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        modules.forEach { module ->
            ModuleListItem(
                module = module,
                isCompleted = userProgress?.completedModuleIds?.contains(module.id) == true,
                onClick = { onReviewModule(module.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Necesario para Card onClick
@Composable
fun ModuleListItem(
    module: StoicModule,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) // Un poco más sutil
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 1.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp) // Ajuste de padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                module.title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(end = 8.dp) // Para que el título no se corte si es largo
            )
            if (isCompleted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Completado",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                // Icono para módulos pendientes o bloqueados
                // Puedes diferenciarlo si tienes esa lógica (ej. módulo actual vs. futuros bloqueados)
                Icon(
                    // Icons.Filled.RadioButtonUnchecked, // Alternativa a Lock
                    Icons.Filled.Lock,
                    contentDescription = "Pendiente",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// --- Componentes de UI de Sección (Sin Cambios) ---
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SectionContent(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Justify,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun QuoteCard(quote: String, author: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "\"$quote\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "- $author",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
