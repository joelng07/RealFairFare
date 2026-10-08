# FairFare

FairFare is a JavaFX expense-sharing application with self-contained SQLite persistence.

## Launch directly

Double-click `distribution/FairFare/FairFare.exe`. No MySQL server, account, password, environment variable, or internet connection is required.

On the first launch, FairFare automatically creates its SQLite database at:

```text
%LOCALAPPDATA%\FairFare\fairfare.db
```

The file stays on the device and preserves users, groups, members, expenses, and balances after the application closes. Copy that `.db` file only if you intentionally want to migrate local data to another device.

## Run during development

```powershell
.\.tools\apache-maven-3.9.9\bin\mvn.cmd javafx:run
```

The SQLite JDBC driver is declared in `pom.xml`; Maven downloads it on the first build.
