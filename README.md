# Work.Co - Mis proyectos

Aplicación Android académica en Java para organizar proyectos estudiantiles. Adaptación del Work.Co web entregado, que usaba Express, EJS y MySQL. Cada cuenta administra exclusivamente sus propios proyectos.

## Funciones

Registro, inicio y cierre de sesión con correo/contraseña; sesión persistente; creación, lista en tiempo real, edición y eliminación confirmada de proyectos; validación local y reglas de seguridad por UID. Interfaz XML con Material Components y textos en español.

## Tecnologías y versiones fijadas

Android Studio Panda 1 | 2025.3.1; Java 17; AGP 8.13.2; Gradle 8.13; SDK mínimo 23; compileSdk/targetSdk 36; Material 1.13.0; AppCompat 1.7.1; Core 1.16.0; RecyclerView 1.4.0; Firebase BoM 34.7.0; google-services 4.4.4; Firebase Authentication y Cloud Firestore.

Estas versiones son deliberadamente fijas y no son todas las últimas. La documentación consultada el 22-11-2026 muestra BoM 35.2.0 y google-services 4.6.0. Antes de actualizar, verifica compatibilidad, SDK mínimo y cambios en las notas oficiales. No hace falta actualizar para seguir esta configuración académica.

## Configuración

1. Extraer el ZIP y abrir la carpeta WorkCoAndroid con File > Open en Android Studio Panda 1.
2. Instalar Android SDK Platform 36 y Build Tools 35.0.0 desde SDK Manager. Seleccionar JDK 17 para Gradle desde Settings > Build, Execution, Deployment > Build Tools > Gradle. Los nombres de menús pueden variar.
3. Crear un proyecto en https://console.firebase.google.com/ y registrar una app Android con applicationId EXACTO `cl.workco.app`.
4. Descargar el archivo real `google-services.json` y agregarlo localmente a `app/google-services.json`. No se incluye una configuración ficticia. `.gitignore` evita subirlo al repositorio.
5. Habilitar Authentication > Sign-in method > Email/Password. Mantener la política mínima de seis caracteres para esta demostración o informar al usuario de requisitos más estrictos.
6. Crear Cloud Firestore Standard, base `(default)`, modo producción. Elegir región disponible y publicar el contenido completo de `firestore.rules` en la pestaña Rules. No dejar reglas públicas.
7. Sincronizar Gradle y ejecutar `app` en un dispositivo o emulador API 23 o superior con internet. Para la presentación se recomienda API 35 o 36 y una imagen Google Play.
8. Crear una cuenta desde la app e ingresar cinco proyectos usando los datos sugeridos en `docs/GUIA.md`.

No se necesita MySQL, Node.js, un servidor propio, Storage ni claves de servicio. SHA-1 no es requisito de este flujo básico de correo y contraseña. Permisos INTERNET y ACCESS_NETWORK_STATE son normales y no solicitan autorización en tiempo de ejecución.

## Compilar

Windows: `gradlew.bat assembleDebug`

macOS/Linux: `chmod +x gradlew` y después `./gradlew assembleDebug`.

APK esperado: `app/build/outputs/apk/debug/app-debug.apk`. Android Studio genera `local.properties` con la ruta local del SDK; no se debe compartir entre computadores.

## Datos y seguridad

Colección `proyectos`. ID automático de documento; campos `userId`, `titulo`, `descripcion`, `categoria`, `estado`. El ID no se duplica como campo porque `getId()` usa `@Exclude`. Solo Authentication maneja credenciales; no hay documentos de contraseñas.

La consulta siempre usa `whereEqualTo("userId", uid)` y las reglas validan dueño, tipos, campos, largos y estados permitidos. Crear usa `set`; actualizar usa `update` y conserva UID e ID. No hay límite artificial de cinco registros.

## Conectividad y ciclo de vida

La lista distingue datos de caché, cambios pendientes y sincronización confirmada. Firestore mantiene caché local de forma predeterminada en Android. Las escrituras necesitan confirmación del servidor para mostrar éxito. Se rechaza iniciar una escritura sin conexión detectada; si internet cae después, puede quedar pendiente hasta reconectar. Atrás o cerrar la app no cancela una escritura que ya fue enviada. El ID de una creación se conserva ante recreación de pantalla para reducir duplicados.

El listener se registra en onStart y se elimina en onStop; al salir de la cuenta se desconecta y se vacía el adaptador. La caché no se borra al cerrar sesión, pero toda consulta está filtrada por el UID actual. Las reglas protegen el backend; no deben reemplazarse por una condición solo visual.

## Validación de esta entrega

Se revisaron la estructura del ZIP, la sintaxis Java, todos los XML y las referencias locales de recursos. No se pudo ejecutar assembleDebug: este entorno no tiene Android SDK y Java no pudo resolver services.gradle.org al descargar Gradle. No se ha ejecutado contra tu proyecto Firebase. Por tanto, no se afirma que exista un APK compilado o una prueba real de nube. Completar las comprobaciones de `docs/GUIA.md` después de configurar Firebase.

## Documentación

- `docs/GUIA.md`: configuración detallada, rúbrica, capturas, defensa y errores.
- `docs/CODIGO_EXPLICADO.md`: contenido íntegro de archivos de texto y explicaciones por archivo.
- `docs/INFORME.md` y `docs/Informe_Firebase_WorkCo.pdf`: investigación de tres páginas con documentación oficial.
- `docs/VERIFICACION.txt`: comprobaciones realmente realizadas y pruebas manuales pendientes.

## Fuentes oficiales

- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/docs/firestore/quickstart
- https://firebase.google.com/docs/firestore/security/rules-conditions
- https://firebase.google.com/docs/firestore/security/rules-query
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
