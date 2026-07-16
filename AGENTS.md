# Project Rules

- Before changing behavior, read the relevant local code.
- Keep changes scoped; avoid unrelated refactors and formatting-only noise.
- Unless the user explicitly requests it, do not revert or overwrite existing user changes.
- Do not create Git commits unless the user explicitly asks for a commit.
- Prefer existing project patterns over new abstractions.
- When editing files containing Chinese or other non-ASCII text, use an explicit UTF-8 read/write path. After writing, read the affected text back as UTF-8 and verify it contains no `?` substitutions or mojibake.
- Keep tests in the package/directory structure of the code they cover.
- Share code by proximity first: put a new component or helper with its feature initially; when it is reused by two or more features, promote it to the appropriate shared package. Do not make feature packages depend on each other.

## Compose UI and Navigation

- Navigation uses Navigation 3. Register routes and compose their destinations centrally in `CookApp`.
- Organize UI by responsibility:
  - `XxxPage` is a direct Navigation 3 destination and has its own file.
  - `XxxScreen` is a substantial, independently logical region within a Page (for example, a tab, pager content, bottom-sheet content, or loading state) and has its own file.
  - Widgets are reusable UI components. Cross-Page widgets go in a separate file and should normally be `internal`; widgets used only by one Page or Screen stay in that file and are `private`.
- Group each feature under `shared/src/page/<feature>/`: place its destination in the feature root, its ViewModels and UI state in `biz/`, substantial child views in `screen/`, and feature-local reusable controls in `widget/`. Add `util/` only for feature-specific helpers; promote code to `shared/src/widget/` or another shared package only after it serves multiple features.
- Follow the existing View → ViewModel → Repository separation: composables render state and forward user events, ViewModels own UI state and business logic, and repositories encapsulate data access and platform integrations.
- Callers depend on repository interfaces rather than implementation classes. Bind or construct implementations only at composition/application boundaries.
- Keep UI state focused and cohesive. Do not put unrelated state into one large `UiState` class. When a state object grows too large or different fields change for different reasons, split it by responsibility, feature area, or update flow to avoid unnecessary full-state copies and broad recomposition.

## Verification

- After changing code, run the platform-appropriate check command:
  - Windows: `.\kotlin.bat check`
  - Linux/macOS/other Unix-like systems: `./kotlin check`
- If verification fails because of code compilation or source errors, fix the code and rerun the command.
- If verification fails because of the command/runtime/tooling itself, record the failure and skip further command-based verification for that task.
- For documentation-only changes, review the diff for correctness and formatting; a project check is not required.
- For UI changes with meaningful interaction risk, validate the affected flow in the desktop application or Compose Preview when available.
