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
    mainNavController: NavHostController, // No se usa directamente aquí, pero se mantiene por si acaso
    viewModel: StoicismViewModel = viewModel()
) {
    val screenState by viewModel.screenState.collectAsState()

    when {
        screenState.isLoadingModules || screenState.isLoadingProgress -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        screenState.errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                Text("Error: ${screenState.errorMessage}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        screenState.activeModule != null -> {
            val activeModule = screenState.activeModule!! // Sabemos que no es null aquí
            ActiveStoicModuleView(
                module = activeModule,
                userProgress = screenState.userProgress,
                // CAMBIO 1: Pasar el mapa de UserExerciseResponse
                currentExerciseResponses = screenState.currentExerciseResponses,
                onUpdateExerciseResponse = { exerciseId, responseValue, exerciseType ->
                    // CAMBIO 2: Llamar a la función del ViewModel con todos los parámetros
                    viewModel.updateExerciseResponse(
                        exerciseId = exerciseId,
                        responseValue = responseValue,
                        exerciseType = exerciseType,
                        moduleId = activeModule.id // Pasar el ID del módulo activo
                    )
                },
                onCompleteModule = { moduleId ->
                    viewModel.completeModule(moduleId)
                },
                onReviewModule = { moduleId -> // CAMBIO 3: Añadir llamada para repasar
                    viewModel.reviewModule(moduleId)
                }
            )
        }

        else -> {
            // Todos los módulos completados o no hay módulos disponibles
            AllModulesCompletedView(
                modules = screenState.modules,
                userProgress = screenState.userProgress,
                onReviewModule = { moduleId -> // CAMBIO 4: Añadir llamada para repasar
                    viewModel.reviewModule(moduleId)
                }
            )
        }
    }
}


@Composable
fun ActiveStoicModuleView(
    module: StoicModule,
    userProgress: UserStoicProgress?,
    // CAMBIO 5: Tipo de currentExerciseResponses
    currentExerciseResponses: Map<String, UserExerciseResponse>,
    // CAMBIO 6: Firma de onUpdateExerciseResponse
    onUpdateExerciseResponse: (exerciseId: String, responseValue: String, exerciseType: StoicExerciseType) -> Unit,
    onCompleteModule: (moduleId: String) -> Unit,
    onReviewModule: (moduleId: String) -> Unit // Para la lista de módulos al final si la pones también aquí
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
        module.exercises.forEach { exerciseDef -> // exerciseDef es StoicExerciseDefinition
            StoicExerciseItem(
                exercise = exerciseDef,
                // CAMBIO 7: Pasar el objeto UserExerciseResponse completo para este ejercicio
                currentUserResponseObject = currentExerciseResponses[exerciseDef.id],
                onResponseChanged = { responseValue -> // responseValue es el String crudo de la UI
                    // Llamar a la función lambda (que a su vez llama al ViewModel)
                    // con el exerciseDef.id, el responseValue, y el exerciseDef.type
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
            // enabled = podrías añadir lógica aquí, por ejemplo,
            // si todos los ejercicios 'obligatorios' del módulo actual tienen una respuesta no vacía
            // enabled = module.exercises.all { exDef ->
            //    val response = currentExerciseResponses[exDef.id]
            //    response != null && (!exDef.requiresResponse || response.isConsideredComplete())
            // }
            // Donde isConsideredComplete sería una extensión en UserExerciseResponse
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
        // screenState.modules.forEach { m -> // Necesitarías pasar screenState.modules o solo la lista
        //     ModuleListItem(
        //         module = m,
        //         isCompleted = userProgress?.completedModuleIds?.contains(m.id) == true,
        //         onClick = { onReviewModule(m.id) }
        //     )
        // }
        // Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StoicExerciseItem(
    exercise: StoicExerciseDefinition,
    // CAMBIO 8: Recibir el UserExerciseResponse completo
    currentUserResponseObject: UserExerciseResponse?,
    onResponseChanged: (responseValue: String) -> Unit // Sigue siendo String aquí desde la UI específica
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when (exercise.type) {
                StoicExerciseType.REFLECTION_TEXT -> {
                    OutlinedTextField(
                        // CAMBIO 9: Usar responseText del objeto
                        value = currentUserResponseObject?.responseText ?: "",
                        onValueChange = onResponseChanged, // Esto envía el nuevo texto directamente
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
                                .clickable { onResponseChanged(index.toString()) }, // Hacer clickeable toda la fila
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                // CAMBIO 10: Comparar con selectedChoiceIndex del objeto
                                selected = currentUserResponseObject?.selectedChoiceIndex == index,
                                onClick = { onResponseChanged(index.toString()) } // Envía el índice como String
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
                    // CAMBIO 11: UI más interactiva para ACTION_PROMPT, usando 'completed'
                    val isCompleted = currentUserResponseObject?.completed == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onResponseChanged((!isCompleted).toString()) }, // Envía "true" o "false"
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { newCheckedState ->
                                onResponseChanged(newCheckedState.toString()) // Envía "true" o "false"
                            }
                        )
                        Text(
                            text = exercise.questionPrompt ?: "Intenta aplicar esto.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                            fontStyle = if (isCompleted) FontStyle.Normal else FontStyle.Italic
                        )
                    }
                    if (isCompleted) {
                        Text(
                            "¡Bien hecho!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.End)
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
    onReviewModule: (moduleId: String) -> Unit // CAMBIO 12: Añadir lambda para repasar
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
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
        Text(
            "Puedes repasar los módulos:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        modules.forEach { module ->
            ModuleListItem(
                module = module,
                isCompleted = userProgress?.completedModuleIds?.contains(module.id) == true,
                onClick = { onReviewModule(module.id) } // CAMBIO 13: Llamar a onReviewModule
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleListItem(
    module: StoicModule,
    isCompleted: Boolean,
    onClick: () -> Unit // onClick ahora es genérico, la lógica está en el llamador
) {
    Card(
        onClick = onClick, // Permitir repasar o la acción definida por el llamador
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(module.title, style = MaterialTheme.typography.titleSmall)
            if (isCompleted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Completado",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                // Podrías diferenciar visualmente un módulo bloqueado (aún no accesible)
                // de uno simplemente no completado pero accesible.
                // Aquí asumimos que si no está completo, podría estar "bloqueado" o simplemente "pendiente".
                Icon(
                    Icons.Filled.Lock, // O algún otro icono como RadioButtonUnchecked
                    contentDescription = "Pendiente", // O "Bloqueado"
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
