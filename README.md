# Library Manager

## Requirements

- JDK 17
- Maven 3.9 or newer

## Running with SQLite

The application expects an existing SQLite database at `sql/library.sqlite`. It validates the schema and does not create or update tables automatically. SQLite is available through the Maven `sqlite-test` profile.

### PowerShell script

```powershell
.\runme-sqlite.ps1
```

### Direct Java command

Generate the runtime classpath with Maven, set the JDBC URL, then start the application with `java`:

```powershell
mvn -Psqlite-test compile dependency:build-classpath "-Dmdep.outputFile=target/sqlite-classpath.txt"
$dbPath = (Resolve-Path .\sql\library.sqlite).Path
$env:HIBERNATE_DB_URL = "jdbc:sqlite:$dbPath"
java -cp "target\classes;$(Get-Content target\sqlite-classpath.txt)" org.bdd.ui.LibraryApp
```

## Running with PostgreSQL

Create or restore the database and schema before starting the application. For a new local database, run `psql -d postgres -f sql/library_db.sql` with local PostgreSQL authentication configured. The application validates the existing schema and does not create or update tables automatically.

### PowerShell script

```powershell
.\runme-postgresql.ps1
```

The script prompts for the JDBC URL and credentials when they are not set in the process environment. Use a local, isolated test database; do not point it at production.

### Direct Java command

Set the PostgreSQL connection values in the current PowerShell process, generate the runtime classpath, then launch the application:

```powershell
$env:HIBERNATE_DB_URL = 'jdbc:postgresql://localhost:5432/library_db'
$env:HIBERNATE_DB_USERNAME = Read-Host 'PostgreSQL username'
$securePassword = Read-Host 'PostgreSQL password' -AsSecureString
$env:HIBERNATE_DB_PASSWORD = ([System.Net.NetworkCredential]::new('', $securePassword)).Password
mvn compile dependency:build-classpath "-Dmdep.outputFile=target/postgresql-classpath.txt"
java -cp "target\classes;$(Get-Content target\postgresql-classpath.txt)" org.bdd.ui.LibraryApp
```

Do not put database credentials in tracked files. For another runtime setup, provide `HIBERNATE_DB_URL`, `HIBERNATE_DB_USERNAME`, and `HIBERNATE_DB_PASSWORD` as process environment variables. The schema and ID mapping must be validated against the target PostgreSQL database before deployment; the repository contains no production database connection details.
