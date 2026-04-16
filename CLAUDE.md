# Schedule — project guidance for Claude

## UI stack

- **Material3 only.** No `androidx.compose.material.*` (M2) imports anywhere in `app/src/main/`. Use `androidx.compose.material3.*`. Material icons stay at `androidx.compose.material.icons.*` — that package still ships from the icons artifact and is not M2 UI.
- **Edge-to-edge is the default.** Activities call `enableEdgeToEdge()` before `setContent`. The theme makes the status and navigation bars transparent. Every `Scaffold` passes `contentWindowInsets = WindowInsets.safeDrawing` (or `systemBars` if the screen should draw behind the top bar).
- `BackdropScaffold`, `ContentAlpha`, `LocalContentAlpha`, M2 `Divider`, M2 `BottomNavigation` are forbidden — they were removed in this migration.

## Code style

- **No comments in code.** Write self-explanatory code with clear names. Do not add explanatory comments, TODOs, or section separators in Kotlin, Gradle (`.gradle.kts`), or `libs.versions.toml`. A comment is acceptable only when it documents a non-obvious invariant, a hidden constraint, or a workaround for a specific bug — and then one short line, no blocks.
- **No section-header comments** in build files or the version catalog. Structure carries the meaning.
- Do not leave `// removed …`, `// old …`, or similar breadcrumbs when deleting code.

## Build / dependencies

- All versions and artifact coordinates live in `gradle/libs.versions.toml`. Add new deps there; reference via `libs.*` in `app/build.gradle.kts`. Do not hardcode versions in module build files.
- Build scripts are Kotlin DSL (`.gradle.kts`). No Groovy files.
- Gradle wrapper: **9.4.1**. AGP: **9.0.1**. Kotlin: **2.3.20**. JDK to run Gradle: **21** (17–26 supported).
- AGP 9.0 applies Kotlin automatically — do **not** add `org.jetbrains.kotlin.android` to plugins.
- `buildConfig = true` is explicitly enabled; AGP 9 defaults it off.
- Firebase BOM 34+ merged the `-ktx` artifacts into the main ones. Import `com.google.firebase.<module>.*`, not `.ktx.*`.

## Koin (4.x) in Compose

- In Composables use `koinViewModel()` / `koinInject()`.
- In Activities/Fragments use `by viewModel()` from `org.koin.androidx.viewmodel.ext.android.viewModel`.
- The old `by viewModel()` / `getViewModel()` / `get()` from `org.koin.androidx.compose` are removed.
