# 2FA Notifier

App Android mínima que copia al portapapeles, al instante, los códigos de verificación que llegan por **Gmail** (leyendo su notificación) y por **SMS**.

- **Sin permiso de internet.** No puede enviar nada fuera del teléfono.
- Solo lee notificaciones de Gmail y de apps de mensajes; el resto se ignora.
- Nada se guarda en disco. El diagnóstico vive en memoria y el portapapeles se borra a los 60 s (opcional).
- El código se copia marcado como *sensible* (Android 13+ no muestra la vista previa).

## Instalar

Con [Obtainium](https://github.com/ImranR98/Obtainium): agrega `https://github.com/samuelvelero/android-2fa-notifier`.

## Configurar

1. Abre la app y activa **Acceso a notificaciones**, **Recibir SMS** y **Mostrar avisos**.
   Si Android dice "ajuste restringido": Info de la app → ⋮ → *Permitir ajustes restringidos*.
2. Deja las notificaciones de Gmail **activadas** (pueden ser silenciosas).
3. Si el diagnóstico muestra "contenido sensible oculto" en vez del código, desactiva
   *Ajustes → Notificaciones → Notificaciones mejoradas / Sugerir acciones y respuestas*.

## Compilar

```
./gradlew test assembleRelease
```

La firma usa `keystore.properties` + `release.jks` locales (no están en el repositorio).
