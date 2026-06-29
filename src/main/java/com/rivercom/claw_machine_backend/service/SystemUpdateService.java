package com.rivercom.claw_machine_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
public class SystemUpdateService {

    private final Path repositoryPath;
    private final Path releaseDownloadPath;
    private final Path updaterRootPath;
    private final String repositoryUrl;
    private final String branch;
    private final String releaseApiUrl;
    private final String packageName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SystemUpdateService(
            @Value("${app.system-update.repository-url:https://github.com/daniel-perez-coa/claw-machine-backend.git}") String repositoryUrl,
            @Value("${app.system-update.repository-path:${user.home}/.claw-machine-admin/updater/claw-machine-backend}") String repositoryPath,
            @Value("${app.system-update.branch:develop}") String branch,
            @Value("${app.system-update.release-api-url:https://api.github.com/repos/daniel-perez-coa/claw-machine-backend/releases/latest}") String releaseApiUrl,
            @Value("${app.system-update.release-download-path:${user.home}/.claw-machine-admin/updater/releases}") String releaseDownloadPath,
            @Value("${app.system-update.package-name:maquina-de-garra}") String packageName) {
        this.repositoryPath = Path.of(repositoryPath).toAbsolutePath().normalize();
        this.releaseDownloadPath = Path.of(releaseDownloadPath).toAbsolutePath().normalize();
        this.updaterRootPath = this.repositoryPath.getParent();
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.releaseApiUrl = releaseApiUrl;
        this.packageName = packageName;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public UpdateResult updateFromDevelop() {
        if (!isLinux()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La actualizacion automatica solo esta disponible en Linux.");
        }

        StringBuilder logOutput = new StringBuilder();
        ensureReleaseDownloadPath();

        if (installPendingDownloadedPackage(releaseDownloadPath, logOutput)) {
            return new UpdateResult("Actualizacion instalada correctamente.", tail(logOutput.toString()), true);
        }

        GitHubRelease release = fetchLatestRelease(logOutput);
        String releaseVersion = normalizeReleaseVersion(release.tagName());
        String installedVersion = readInstalledPackageVersion(packageName, logOutput);

        if (installedVersion != null && !isDebVersionNewer(releaseVersion, installedVersion, logOutput)) {
            return new UpdateResult("No hay nuevas versiones que descargar.", tail(logOutput.toString()), false);
        }

        Path debFile = downloadReleaseAsset(release, logOutput);
        DebPackageInfo packageInfo = readDebPackageInfo(debFile);
        String currentInstalledVersion = readInstalledPackageVersion(packageInfo.packageName(), logOutput);

        if (currentInstalledVersion != null && !isDebVersionNewer(packageInfo.version(), currentInstalledVersion, logOutput)) {
            return new UpdateResult("No hay nuevas versiones que descargar.", tail(logOutput.toString()), false);
        }

        installDebPackage(debFile, logOutput);
        return new UpdateResult("Actualizacion instalada correctamente.", tail(logOutput.toString()), true);
    }

    private void ensureReleaseDownloadPath() {
        try {
            Files.createDirectories(releaseDownloadPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible preparar la carpeta de descargas de actualizacion.");
        }
    }

    private GitHubRelease fetchLatestRelease(StringBuilder logOutput) {
        logOutput.append("\n== Buscando ultima version publicada ==\n");

        HttpRequest request = HttpRequest.newBuilder(URI.create(releaseApiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "claw-machine-admin-updater")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No fue posible consultar la ultima version publicada. Detalle: HTTP " + response.statusCode()
                );
            }

            JsonNode releaseNode = objectMapper.readTree(response.body());
            String tagName = releaseNode.path("tag_name").asText("");
            JsonNode assetsNode = releaseNode.path("assets");

            if (tagName.isBlank() || !assetsNode.isArray()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La respuesta de releases no contiene una version valida.");
            }

            GitHubReleaseAsset debAsset = findDebAsset(assetsNode)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "La ultima version publicada no contiene un paquete .deb."
                    ));

            logOutput.append("Ultima version publicada: ").append(tagName).append(".\n");
            logOutput.append("Paquete disponible: ").append(debAsset.name()).append(".\n");
            return new GitHubRelease(tagName, debAsset);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible leer la informacion de actualizacion.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La consulta de actualizaciones fue interrumpida.");
        }
    }

    private Optional<GitHubReleaseAsset> findDebAsset(JsonNode assetsNode) {
        GitHubReleaseAsset firstDebAsset = null;

        for (JsonNode assetNode : assetsNode) {
            String name = assetNode.path("name").asText("");
            String downloadUrl = assetNode.path("browser_download_url").asText("");

            if (!name.endsWith(".deb") || downloadUrl.isBlank()) {
                continue;
            }

            GitHubReleaseAsset asset = new GitHubReleaseAsset(name, downloadUrl);
            if (name.contains("amd64")) {
                return Optional.of(asset);
            }

            if (firstDebAsset == null) {
                firstDebAsset = asset;
            }
        }

        return Optional.ofNullable(firstDebAsset);
    }

    private Path downloadReleaseAsset(GitHubRelease release, StringBuilder logOutput) {
        GitHubReleaseAsset asset = release.asset();
        Path targetPath = releaseDownloadPath.resolve(asset.name()).normalize();

        if (!targetPath.startsWith(releaseDownloadPath)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "El nombre del paquete de actualizacion no es seguro.");
        }

        if (Files.exists(targetPath)) {
            logOutput.append("El paquete ya estaba descargado: ").append(targetPath).append(".\n");
            return targetPath;
        }

        logOutput.append("\n== Descargando paquete publicado ==\n");
        logOutput.append(asset.downloadUrl()).append('\n');

        HttpRequest request = HttpRequest.newBuilder(URI.create(asset.downloadUrl()))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "claw-machine-admin-updater")
                .GET()
                .build();

        try {
            HttpResponse<Path> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofFile(
                            targetPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No fue posible descargar el paquete de actualizacion. Detalle: HTTP " + response.statusCode()
                );
            }

            return targetPath;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible guardar el paquete de actualizacion.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La descarga de actualizacion fue interrumpida.");
        }
    }

    private String normalizeReleaseVersion(String tagName) {
        String normalizedVersion = tagName == null ? "" : tagName.trim();
        if (normalizedVersion.startsWith("v") || normalizedVersion.startsWith("V")) {
            normalizedVersion = normalizedVersion.substring(1);
        }

        if (normalizedVersion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La version publicada no es valida.");
        }

        return normalizedVersion;
    }

    private boolean installPendingDownloadedPackage(Path distPath, StringBuilder logOutput) {
        if (!Files.isDirectory(distPath)) {
            logOutput.append("No hay paquete local pendiente para instalar.\n");
            return false;
        }

        Optional<Path> pendingDebFile = findNewestDebOptional(distPath);
        if (pendingDebFile.isEmpty()) {
            logOutput.append("No hay paquete local pendiente para instalar.\n");
            return false;
        }

        Path debFile = pendingDebFile.get();
        DebPackageInfo packageInfo = readDebPackageInfo(debFile);
        String installedVersion = readInstalledPackageVersion(packageInfo.packageName(), logOutput);

        if (installedVersion != null && !isDebVersionNewer(packageInfo.version(), installedVersion, logOutput)) {
            logOutput.append("El paquete local ")
                    .append(packageInfo.packageName())
                    .append(" version ")
                    .append(packageInfo.version())
                    .append(" no es mas nuevo que la version instalada ")
                    .append(installedVersion)
                    .append(".\n");
            return false;
        }

        logOutput.append("Se encontro un paquete local pendiente: ")
                .append(packageInfo.packageName())
                .append(" ")
                .append(packageInfo.version())
                .append(". Se instalara sin descargar nuevamente.\n");
        installDebPackage(debFile, logOutput);
        return true;
    }

    private DebPackageInfo readDebPackageInfo(Path debFile) {
        String packageName = runCommandForOutput(
                "Leyendo nombre del paquete local",
                debFile.getParent(),
                List.of("dpkg-deb", "-f", debFile.toString(), "Package")
        );
        String version = runCommandForOutput(
                "Leyendo version del paquete local",
                debFile.getParent(),
                List.of("dpkg-deb", "-f", debFile.toString(), "Version")
        );

        if (packageName.isBlank() || version.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible leer la version del paquete local.");
        }

        return new DebPackageInfo(packageName, version);
    }

    private String readInstalledPackageVersion(String packageName, StringBuilder logOutput) {
        ProcessResult result = runCommand(
                "Leyendo version instalada",
                releaseDownloadPath,
                List.of("dpkg-query", "-W", "-f=${Version}", packageName),
                Duration.ofSeconds(30)
        );

        if (result.exitCode() == 0) {
            String version = result.output().trim();
            logOutput.append("Version instalada de ")
                    .append(packageName)
                    .append(": ")
                    .append(version)
                    .append(".\n");
            return version;
        }

        logOutput.append("El paquete ")
                .append(packageName)
                .append(" no esta instalado o no se pudo consultar; se intentara instalar el paquete local.\n");
        return null;
    }

    private boolean isDebVersionNewer(String candidateVersion, String installedVersion, StringBuilder logOutput) {
        ProcessResult result = runCommand(
                "Comparando versiones",
                releaseDownloadPath,
                List.of("dpkg", "--compare-versions", candidateVersion, "gt", installedVersion),
                Duration.ofSeconds(30)
        );

        if (result.exitCode() == 0) {
            return true;
        }

        if (result.exitCode() == 1) {
            return false;
        }

        logOutput.append(result.output()).append('\n');
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible comparar la version del paquete local.");
    }

    private void installDebPackage(Path debFile, StringBuilder logOutput) {
        runStep(
                "Instalando paquete",
                debFile.getParent(),
                List.of("/usr/bin/pkexec", "/usr/bin/apt", "install", "--reinstall", "-y", debFile.toString()),
                logOutput
        );
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
            logOutput.append("La carpeta de actualizacion existe, pero no es un repositorio Git valido; se limpiara y descargara de nuevo.\n");
            cleanUpdaterRepository();
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

        return cloneRepository(logOutput);
    }

    private boolean cloneRepository(StringBuilder logOutput) {
        Path parentPath = repositoryPath.getParent();
        if (parentPath == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ruta del repositorio de actualizacion no es valida.");
        }

        runStep(
                "Preparando repositorio de actualizacion",
                parentPath,
                List.of("git", "clone", "--branch", branch, repositoryUrl, repositoryPath.toString()),
                logOutput
        );
        return true;
    }

    private void cleanUpdaterRepository() {
        assertRepositoryPathIsSafeToDelete();

        if (!Files.exists(repositoryPath)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(repositoryPath)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new UpdaterCleanupException(exception);
                        }
                    });
        } catch (UpdaterCleanupException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible limpiar la carpeta de actualizacion.");
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible revisar la carpeta de actualizacion.");
        }
    }

    private void assertRepositoryPathIsSafeToDelete() {
        if (updaterRootPath == null || !repositoryPath.startsWith(updaterRootPath) || repositoryPath.equals(updaterRootPath)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La ruta del repositorio de actualizacion no es segura.");
        }
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

    private String runCommandForOutput(String label, Path workingDirectory, List<String> command) {
        ProcessResult result = runCommand(label, workingDirectory, command, Duration.ofSeconds(30));

        if (result.exitCode() != 0) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    label + " fallo. " + summarizeProcessOutput(result.output())
            );
        }

        return result.output().trim();
    }

    private ProcessResult runCommand(String label, Path workingDirectory, List<String> command, Duration timeout) {
        log.info("Ejecutando actualizacion: {} command={}", label, command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, label + " tardo demasiado tiempo.");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(process.exitValue(), output);
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
        return findNewestDebOptional(distPath)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se encontro el paquete .deb generado."
                ));
    }

    private Optional<Path> findNewestDebOptional(Path distPath) {
        if (!Files.isDirectory(distPath)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se encontro la carpeta dist del paquete Linux.");
        }

        try (Stream<Path> files = Files.list(distPath)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".deb"))
                    .max(Comparator.comparing(path -> path.toFile().lastModified()));
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

    public record UpdateResult(String message, String log, boolean restartRequired) {
    }

    private record DebPackageInfo(String packageName, String version) {
    }

    private record ProcessResult(int exitCode, String output) {
    }

    private record GitHubRelease(String tagName, GitHubReleaseAsset asset) {
    }

    private record GitHubReleaseAsset(String name, String downloadUrl) {
    }

    private static class UpdaterCleanupException extends RuntimeException {
        private UpdaterCleanupException(Throwable cause) {
            super(cause);
        }
    }
}
