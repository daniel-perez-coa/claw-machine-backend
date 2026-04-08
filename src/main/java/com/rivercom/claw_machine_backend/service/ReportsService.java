package com.rivercom.claw_machine_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsService {

    private static final DateTimeFormatter FILE_NAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;
    private final Flyway flyway;

    public DatabaseBackupExport generateDatabaseBackup() {
        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("claw-machine-backup-", ".sql");

            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SCRIPT TO '" + escapePath(tempFile) + "'");
            }

            byte[] backupBytes = Files.readAllBytes(tempFile);
            String fileName = "claw-machine-backup-" + LocalDateTime.now().format(FILE_NAME_FORMATTER) + ".sql";

            return new DatabaseBackupExport(fileName, backupBytes);
        } catch (SQLException | IOException exception) {
            log.error("No fue posible exportar la base de datos", exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible exportar la base de datos."
            );
        } finally {
            deleteTempFile(tempFile);
        }
    }

    public void importDatabaseBackup(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un archivo de respaldo.");
        }

        Path tempFile = null;

        try {
            tempFile = Files.createTempFile("claw-machine-import-", ".sql");
            file.transferTo(tempFile);

            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP ALL OBJECTS");
                statement.execute("RUNSCRIPT FROM '" + escapePath(tempFile) + "'");
            }
        } catch (SQLException | IOException exception) {
            log.error("No fue posible importar la base de datos", exception);
            recreateEmptyDatabase();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible importar la base de datos."
            );
        } finally {
            deleteTempFile(tempFile);
        }
    }

    public void resetDatabase() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        } catch (SQLException exception) {
            log.error("No fue posible eliminar la base de datos", exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible eliminar la base de datos."
            );
        }

        recreateEmptyDatabase();
    }

    private String escapePath(Path path) {
        return path.toAbsolutePath()
                .toString()
                .replace("\\", "\\\\")
                .replace("'", "''");
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("No fue posible eliminar el archivo temporal de respaldo: {}", path, exception);
        }
    }

    private void recreateEmptyDatabase() {
        try {
            flyway.migrate();
        } catch (Exception exception) {
            log.error("No fue posible recrear la base de datos vacia", exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La base de datos quedo en un estado inconsistente y requiere revision manual."
            );
        }
    }

    public record DatabaseBackupExport(String fileName, byte[] content) {
    }
}
