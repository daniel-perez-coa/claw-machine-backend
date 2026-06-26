package com.rivercom.claw_machine_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ThermalPrinterService {

    private final boolean enabled;
    private final String mode;
    private final String queue;
    private final String devicePath;

    public ThermalPrinterService(
            @Value("${app.thermal-printer.enabled:true}") boolean enabled,
            @Value("${app.thermal-printer.mode:cups}") String mode,
            @Value("${app.thermal-printer.queue:claw-printer}") String queue,
            @Value("${app.thermal-printer.device-path:/dev/usb/lp2}") String devicePath) {
        this.enabled = enabled;
        this.mode = mode;
        this.queue = queue;
        this.devicePath = devicePath;
    }

    public void print(byte[] escPosBytes) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La impresora termica esta deshabilitada.");
        }

        if (escPosBytes == null || escPosBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ticket termico esta vacio.");
        }

        if ("device".equalsIgnoreCase(mode)) {
            printToDevice(escPosBytes);
            return;
        }

        printToCups(escPosBytes);
    }

    private void printToCups(byte[] escPosBytes) {
        ProcessBuilder processBuilder = new ProcessBuilder("lp", "-d", queue, "-o", "raw");

        try {
            Process process = processBuilder.start();
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write(escPosBytes);
            }

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "La impresora termica no respondio a tiempo.");
            }

            if (process.exitValue() != 0) {
                String error = new String(process.getErrorStream().readAllBytes());
                log.error("CUPS rechazo el ticket termico. exitCode={} error={}", process.exitValue(), error);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible imprimir el ticket termico.");
            }
        } catch (IOException exception) {
            log.error("No fue posible enviar el ticket termico a CUPS", exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible enviar el ticket termico a CUPS.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "La impresion termica fue interrumpida.");
        }
    }

    private void printToDevice(byte[] escPosBytes) {
        try (OutputStream outputStream = Files.newOutputStream(Path.of(devicePath), StandardOpenOption.WRITE)) {
            outputStream.write(escPosBytes);
        } catch (IOException exception) {
            log.error("No fue posible escribir el ticket termico en {}", devicePath, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible escribir en la impresora termica.");
        }
    }
}
