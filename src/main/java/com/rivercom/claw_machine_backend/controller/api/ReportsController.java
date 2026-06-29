package com.rivercom.claw_machine_backend.controller.api;

import com.rivercom.claw_machine_backend.dto.CampaignAddPointsTransactionDTO;
import com.rivercom.claw_machine_backend.dto.CampaignPrizeRedemptionDTO;
import com.rivercom.claw_machine_backend.dto.CampaignQuickRedemptionDTO;
import com.rivercom.claw_machine_backend.service.ReportsService;
import com.rivercom.claw_machine_backend.service.SystemUpdateService;
import com.rivercom.claw_machine_backend.service.ThermalTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportsController {

    private final ReportsService reportsService;
    private final ThermalTicketService thermalTicketService;
    private final SystemUpdateService systemUpdateService;

    @GetMapping("/database-backup")
    public ResponseEntity<byte[]> exportDatabaseBackup() {
        return buildSqlResponse(reportsService.generateDatabaseBackup());
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

    @PostMapping("/reset-user-points")
    public ResponseEntity<Map<String, String>> resetUserPoints() {
        reportsService.resetAllUserPoints();
        return ResponseEntity.ok(Map.of("message", "Los puntos de los usuarios se reiniciaron correctamente."));
    }

    @PostMapping("/system-update")
    public ResponseEntity<SystemUpdateService.UpdateResult> updateSystem() {
        return ResponseEntity.ok(systemUpdateService.updateFromDevelop());
    }

    @GetMapping("/tickets/add-points/{transactionId}")
    public ResponseEntity<byte[]> printAddPointsTicket(@PathVariable Long transactionId) {
        return buildPdfResponse(reportsService.generateAddPointsTicket(transactionId));
    }

    @PostMapping("/tickets/add-points/{transactionId}/thermal-print")
    public ResponseEntity<Map<String, String>> printAddPointsThermalTicket(@PathVariable Long transactionId) {
        thermalTicketService.printAddPointsTicket(transactionId);
        return ResponseEntity.ok(Map.of("message", "Ticket enviado a la impresora termica."));
    }

    @GetMapping("/tickets/quick-redemption")
    public ResponseEntity<byte[]> printQuickRedemptionTicket(@RequestParam List<Long> expenseIds) {
        return buildPdfResponse(reportsService.generateQuickRedemptionTicket(expenseIds));
    }

    @PostMapping("/tickets/quick-redemption/thermal-print")
    public ResponseEntity<Map<String, String>> printQuickRedemptionThermalTicket(@RequestParam List<Long> expenseIds) {
        thermalTicketService.printQuickRedemptionTicket(expenseIds);
        return ResponseEntity.ok(Map.of("message", "Ticket enviado a la impresora termica."));
    }

    @GetMapping("/tickets/user-redemption/{redemptionId}")
    public ResponseEntity<byte[]> printUserRedemptionTicket(@PathVariable Long redemptionId) {
        return buildPdfResponse(reportsService.generateUserRedemptionTicket(redemptionId));
    }

    @PostMapping("/tickets/user-redemption/{redemptionId}/thermal-print")
    public ResponseEntity<Map<String, String>> printUserRedemptionThermalTicket(@PathVariable Long redemptionId) {
        thermalTicketService.printUserRedemptionTicket(redemptionId);
        return ResponseEntity.ok(Map.of("message", "Ticket enviado a la impresora termica."));
    }

    @GetMapping("/weekly-summary/current")
    public ResponseEntity<byte[]> printCurrentWeeklySummary() {
        return buildPdfDownloadResponse(reportsService.generateCurrentWeeklySummaryReport());
    }

    @GetMapping("/add-points-transactions")
    public ResponseEntity<List<CampaignAddPointsTransactionDTO>> listAddPointsTransactions(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(defaultValue = "false") boolean unassignedOnly) {
        return ResponseEntity.ok(reportsService.listAddPointsTransactions(campaignId, unassignedOnly));
    }

    @GetMapping("/campaigns/{campaignId}/prize-redemptions")
    public ResponseEntity<List<CampaignPrizeRedemptionDTO>> listCampaignPrizeRedemptions(
            @PathVariable Long campaignId) {
        return ResponseEntity.ok(reportsService.listCampaignPrizeRedemptions(campaignId));
    }

    @GetMapping("/campaigns/{campaignId}/quick-redemptions")
    public ResponseEntity<List<CampaignQuickRedemptionDTO>> listCampaignQuickRedemptions(
            @PathVariable Long campaignId) {
        return ResponseEntity.ok(reportsService.listCampaignQuickRedemptions(campaignId));
    }

    private ResponseEntity<byte[]> buildSqlResponse(ReportsService.DatabaseBackupExport backupExport) {
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

    private ResponseEntity<byte[]> buildPdfResponse(ReportsService.PdfReportExport reportExport) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(reportExport.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(reportExport.content().length)
                .body(reportExport.content());
    }

    private ResponseEntity<byte[]> buildPdfDownloadResponse(ReportsService.PdfReportExport reportExport) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(reportExport.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(reportExport.content().length)
                .body(reportExport.content());
    }
}
