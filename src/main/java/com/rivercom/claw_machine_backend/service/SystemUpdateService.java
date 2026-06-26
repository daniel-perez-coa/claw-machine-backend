package com.rivercom.claw_machine_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@Slf4j
public class SystemUpdateService {

    private final Path repositoryPath;
    private final String repositoryUrl;
    private final String branch;

    public SystemUpdateService(
            @Value("${app.system-update.repository-url:https://github.com/daniel-perez-coa/claw-machine-backend.git}") String repositoryUrl,
            @Value("${app.system-update.repository-path:${user.home}/.claw-machine-admin/updater/claw-machine-backend}") String repositoryPath,
            @Value("${app.system-update.branch:develop}") String branch) {
        this.repositoryPath = Path.of(repositoryPath).toAbsolutePath().normalize();
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
    }

    public UpdateResult updateFromDevelop() {
        if (!isLinux()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La actualizacion automatica solo esta disponible en Linux.");
        }

        StringBuilder logOutput = new StringBuilder();
        verifyRepositoryReachable(logOutput);
        boolean repositoryWasCloned = ensureRepository(logOutput);
        Path electronPath = repositoryPath.resolve("desktop-electron");

        if (!Files.isDirectory(electronPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El repositorio no contiene la app de escritorio.");
        }

        runStep(
                "Buscando nuevas versiones",
                repositoryPath,
                List.of("git", "fetch", "origin", branch + ":refs/remotes/origin/" + branch),
                logOutput
        );

        if (!repositoryWasCloned && !hasRemoteChanges()) {
            return new UpdateResult("No hay nuevas versiones que descargar.", tail(logOutput.toString()));
        }

        if (!repositoryWasCloned) {
            runStep("Descargando cambios", repositoryPath, List.of("git", "pull", "origin", branch), logOutput);
        }

        runStep("Instalando dependencias", electronPath, List.of("npm", "install"), logOutput);
        runStep("Compilando paquete Linux", electronPath, List.of("npm", "run", "dist:linux"), logOutput);

        Path debFile = findNewestDeb(electronPath.resolve("dist"));
        runStep("Instalando paquete", repositoryPath, List.of("pkexec", "apt", "install", "-y", debFile.toString()), logOutput);

        return new UpdateResult("Actualizacion instalada correctamente.", tail(logOutput.toString()));
    }

    private void verifyRepositoryReachable(StringBuilder logOutput) {
        Path workingDirectory = Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
        runStep(
                "Verificando conexion con repositorio",
                workingDirectory,
                List.of("git", "ls-remote", "--heads", repositoryUrl, branch),
                logOutput
        );
    }

    private boolean ensureRepository(StringBuilder logOutput) {
        if (Files.isDirectory(repositoryPath.resolve(".git"))) {
            return false;
        }

        if (Files.exists(repositoryPath) && !isEmptyDirectory(repositoryPath)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La carpeta de actualizacion existe, pero no es un repositorio Git valido: " + repositoryPath
            );
        }

        Path parentPath = repositoryPath.getParent();
        if (parentPath == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ruta del repositorio de actualizacion no es valida.");
        }

        try {
            Files.createDirectories(parentPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible preparar la carpeta de actualizacion.");
        }

        runStep(
                "Preparando repositorio de actualizacion",
                parentPath,
                List.of("git", "clone", "--branch", branch, repositoryUrl, repositoryPath.toString()),
                logOutput
        );
        return true;
    }

    private void runStep(String label, Path workingDirectory, List<String> command, StringBuilder logOutput) {
        logOutput.append("\n== ").append(label).append(" ==\n");
        log.info("Ejecutando actualizacion: {} command={}", label, command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            StringBuilder processOutput = new StringBuilder();
            Thread outputReader = new Thread(() -> readProcessOutput(process, processOutput));
            outputReader.start();

            boolean finished = process.waitFor(Duration.ofMinutes(20).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, label + " tardo demasiado tiempo.");
            }

            outputReader.join(Duration.ofSeconds(5).toMillis());
            logOutput.append(processOutput).append('\n');

            if (process.exitValue() != 0) {
                String detail = summarizeProcessOutput(processOutput.toString());
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        label + " fallo. " + detail
                );
            }
        } catch (IOException exception) {
            log.error("No fue posible ejecutar {}", label, exception);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible ejecutar " + label + ". Verifique que Git, Node/npm y las herramientas del sistema esten instaladas."
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La actualizacion fue interrumpida.");
        }
    }

    private void readProcessOutput(Process process, StringBuilder processOutput) {
        try {
            byte[] bytes = process.getInputStream().readAllBytes();
            processOutput.append(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            processOutput.append("No fue posible leer la salida del proceso.");
        }
    }

    private String summarizeProcessOutput(String output) {
        String cleanOutput = output == null ? "" : output.trim();
        if (cleanOutput.isBlank()) {
            return "Revise permisos, conexion o el repositorio local.";
        }

        String singleLineOutput = cleanOutput.replaceAll("\\s+", " ");
        int maxLength = 220;
        if (singleLineOutput.length() > maxLength) {
            return "Detalle: " + singleLineOutput.substring(0, maxLength) + "...";
        }

        return "Detalle: " + singleLineOutput;
    }

    private Path findNewestDeb(Path distPath) {
        if (!Files.isDirectory(distPath)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se encontro la carpeta dist del paquete Linux.");
        }

        try (Stream<Path> files = Files.list(distPath)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".deb"))
                    .max(Comparator.comparing(path -> path.toFile().lastModified()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "No se encontro el paquete .deb generado."
                    ));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible localizar el paquete .deb.");
        }
    }

    private boolean isEmptyDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }

        try (Stream<Path> files = Files.list(path)) {
            return files.findAny().isEmpty();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible revisar la carpeta de actualizacion.");
        }
    }

    private boolean hasRemoteChanges() {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "rev-list", "--count", "HEAD..origin/" + branch);
        processBuilder.directory(repositoryPath.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "No fue posible verificar nuevas versiones a tiempo.");
            }

            if (process.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible verificar nuevas versiones.");
            }

            return Integer.parseInt(output) > 0;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible verificar nuevas versiones.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La verificacion de nuevas versiones fue interrumpida.");
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La verificacion de nuevas versiones devolvio una respuesta invalida.");
        }
    }

    private boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    private String tail(String value) {
        int maxLength = 3000;
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(value.length() - maxLength);
    }

    public record UpdateResult(String message, String log) {
    }
}
