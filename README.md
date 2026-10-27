# Galibu-FE — Apps Android (Cliente + Profesional)

Dos aplicaciones Kotlin/Compose + módulo `core` compartido.

```
Galibu-FE/
├── client-app/         # com.galibu.client — solicita servicios, ofertas, chat, pagos
├── professional-app/   # com.galibu.professional — marketplace, ofertas, trabajos, chat
└── core/               # Room (caché), repositorio, Retrofit contra el backend,
                        # Firebase (solo chat/typing, Storage, push), UI compartida
```

## Requisitos

- JDK 17+ (compilación APK debug verificada con el SDK en `D:\DevData\Android\SDK`;
  `local.properties` apunta al SDK de usuario por defecto)
- Backend corriendo (ver `../Galibu-BE/README.md`) — **sin backend no hay login**
- Mismo WiFi entre el PC y el teléfono (la app llama a la IP LAN del PC)

## Configuración

`local.properties` (no se commitea):

```properties
sdk.dir=C\:\\Users\\pacho\\AppData\\Local\\Android\\Sdk
apiUrl=http://192.168.1.7:8080/api/
```

- `sdk.dir`: para compilar localmente se usa temporalmente `D:\DevData\Android\SDK`
  (tiene build-tools completos) y luego se restaura.
- `apiUrl`: base del backend con `/api/` al final. Emulador → `http://10.0.2.2:8080/api/`,
  teléfono físico → IP LAN del PC (`ipconfig`).

## Dónde se actualiza la IP del backend

1. Averigua la IP LAN del PC donde corre el backend: `ipconfig` → IPv4
   (ej. `192.168.1.7`). El teléfono debe estar en el mismo WiFi.
2. Edita `Galibu-FE/local.properties` (crealo si no existe) y pon:
   `apiUrl=http://TU_IP:8080/api/` (con `/api/` al final).
3. Recompila e instala (cada app lee su propio `BuildConfig.API_URL`):
   `gradlew.bat :client-app:assembleDebug :professional-app:assembleDebug`
   + `adb install -r ...apk`. Sin recompilar, el APK sigue apuntando a la IP vieja.
4. Si no conecta: abre el puerto en el PC del backend
   (Firewall de Windows → entrada TCP 8080) y verifica en el navegador del
   teléfono `http://TU_IP:8080/api/geo/categories` (debe dar 200/login).

Detalle técnico: `client-app/build.gradle.kts` y
`professional-app/build.gradle.kts` leen `apiUrl` de `local.properties`
(default `http://192.168.1.7:8080/api/`) y lo exponen como
`BuildConfig.API_URL`; cada `MainActivity.onCreate` lo inyecta en
`ApiConfig.baseUrl` (valor inicial solo-emulador `http://10.0.2.2:8080/api/`)
antes de crear el repositorio/Retrofit.

## Comandos (desde `Galibu-FE/`)

```bat
gradlew.bat :core:testDebugUnitTest          # tests unitarios (82)
gradlew.bat :client-app:assembleDebug        # APK cliente
gradlew.bat :professional-app:assembleDebug  # APK profesional
adb install -r client-app\build\outputs\apk\debug\client-app-debug.apk
```

## Notas

- `google-services.json` por app (proyecto Firebase `galibu-app`, solo push/chat/storage).
- Los manifests permiten tráfico cleartext (`usesCleartextTraffic`, solo desarrollo
  local contra `http://IP:8080`); en producción el backend irá con HTTPS y se retira.
- Sesión JWT con auto-refresh (OkHttp Authenticator); matar la app no cierra sesión,
  solo el botón Cerrar sesión. Al reabrir hay refresh silencioso (`refreshAll`).
- Datos en tiempo real: REST + refresh al volver a primer plano y al abrir detalles;
  el chat sí es realtime (listeners Firestore, se conservan).
