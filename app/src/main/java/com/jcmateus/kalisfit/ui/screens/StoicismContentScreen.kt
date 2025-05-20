package com.jcmateus.kalisfit.ui.screens



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.jcmateus.kalisfit.R // Asegúrate de que R se importe correctamente
import com.jcmateus.kalisfit.ui.theme.KalisFitTheme // Asume que tienes tu tema

@Composable
fun StoicismContentScreen(
    mainNavController: NavHostController // Para posible navegación futura desde aquí
) {
    Scaffold( // Usamos Scaffold por si queremos una TopAppBar específica para esta pantalla en el futuro
        topBar = {
            // Podríamos tener una TopAppBar simple aquí si no usamos la del KalisMainScreen
            // o si esta pantalla se abre de una forma que no muestra la TopAppBar principal.
            // Por ahora, si es parte del NavHost principal que ya tiene una TopAppBar,
            // esta podría no ser necesaria o podría causar un doble TopAppBar.
            // Si KalisMainScreen YA provee la TopAppBar con el título correcto,
            // no necesitamos otra aquí.
            // Por simplicidad, si el título ya se maneja en KalisMainScreen, omitimos esta TopAppBar.
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica el padding del Scaffold
                .padding(horizontal = 16.dp) // Padding general para el contenido
                .verticalScroll(rememberScrollState()) // Para que el contenido sea desplazable
        ) {
            // 1. Título Principal de la Sección
            Text(
                text = stringResource(id = R.string.stoicism_title_main),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // 2. Introducción Breve
            SectionTitle(title = stringResource(id = R.string.stoicism_section_introduction))
            SectionContent(text = stringResource(id = R.string.stoicism_introduction_text))

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Sección Temática Ejemplo: "El Dicotomía del Control"
            SectionTitle(title = stringResource(id = R.string.stoicism_section_control_title))
            SectionContent(text = stringResource(id = R.string.stoicism_section_control_text))
            QuoteCard(
                quote = stringResource(id = R.string.stoicism_quote_epictetus_control),
                author = "Epicteto"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Sección Temática Ejemplo: "Vivir conforme a la Naturaleza"
            SectionTitle(title = stringResource(id = R.string.stoicism_section_nature_title))
            SectionContent(text = stringResource(id = R.string.stoicism_section_nature_text))
            // Podrías añadir otra cita o un ejercicio práctico aquí

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Ejercicio Práctico / Reflexión
            SectionTitle(title = stringResource(id = R.string.stoicism_section_exercise_title))
            ExerciseCard(
                title = stringResource(id = R.string.stoicism_exercise_negative_visualization_title),
                description = stringResource(id = R.string.stoicism_exercise_negative_visualization_desc)
            )

            Spacer(modifier = Modifier.height(32.dp)) // Espacio al final
        }
    }
}

// --- Componentes Auxiliares para la Pantalla de Estoicismo ---

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
        textAlign = TextAlign.Justify, // Justificar para un look más formal
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

@Composable
fun ExerciseCard(title: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
fun StoicismContentScreenPreview() {
    KalisFitTheme { // Usa tu tema de la aplicación
        StoicismContentScreen(rememberNavController())
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
fun QuoteCardPreview() {
    KalisFitTheme {
        QuoteCard(
            quote = "La felicidad de tu vida depende de la calidad de tus pensamientos.",
            author = "Marco Aurelio"
        )
    }
}