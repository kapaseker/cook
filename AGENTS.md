# Project Rules

- After changing code, run the platform-appropriate check command:
  - Windows: `.\kotlin.bat check`
  - Linux/macOS/other Unix-like systems: `./kotlin check`
- If verification fails because of code compilation or source errors, fix the code and rerun the command.
- If verification fails because of the command/runtime/tooling itself, record the failure and skip further command-based verification for that task.
