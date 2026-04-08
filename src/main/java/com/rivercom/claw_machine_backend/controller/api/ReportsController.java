package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/database-backup")
    public ResponseEntity<byte[]> exportDatabaseBackup() {
        ReportsService.DatabaseBackupExport backupExport = reportsService.generateDatabaseBackup();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(backupExport.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentType(MediaType.parseMediaType("application/sql"))
                .contentLength(backupExport.content().length)
                .body(backupExport.content());
    }

    @PostMapping("/database-backup/import")
    public ResponseEntity<Map<String, String>> importDatabaseBackup(@RequestParam("file") MultipartFile file) {
        reportsService.importDatabaseBackup(file);
        return ResponseEntity.ok(Map.of("message", "Base de datos importada correctamente."));
    }

    @DeleteMapping("/database-backup")
    public ResponseEntity<Map<String, String>> deleteDatabaseBackup() {
        reportsService.resetDatabase();
        return ResponseEntity.ok(Map.of("message", "Base de datos eliminada correctamente."));
    }
}
