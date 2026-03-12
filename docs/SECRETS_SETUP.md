# Configuración de secretos locales

Este proyecto ya no expone la API Key de Google Maps en el `AndroidManifest.xml`.

## Pasos
1. Copia `local.properties.example` como `local.properties` (en la raíz del proyecto).
2. Define tu clave:

```
MAPS_API_KEY=TU_API_KEY_DE_GOOGLE_MAPS
```

3. Sincroniza Gradle y ejecuta la app.

## Notas
- `local.properties` **no** debe subirse al repositorio.
- Para CI/CD, inyecta `MAPS_API_KEY` como secreto y genera `local.properties` en el pipeline.
