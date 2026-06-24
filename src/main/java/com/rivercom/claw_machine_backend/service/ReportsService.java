package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.MachineCampaign;
import com.rivercom.claw_machine_backend.domain.entity.MachineExpenseRecords;
import com.rivercom.claw_machine_backend.domain.entity.PointTransaction;
import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.entity.PrizeRedemption;
import com.rivercom.claw_machine_backend.domain.entity.IncomeRecords;
import com.rivercom.claw_machine_backend.domain.entity.User;
import com.rivercom.claw_machine_backend.domain.enums.TransactionType;
import com.rivercom.claw_machine_backend.dto.CampaignAddPointsTransactionDTO;
import com.rivercom.claw_machine_backend.dto.CampaignPrizeRedemptionDTO;
import com.rivercom.claw_machine_backend.dto.CampaignQuickRedemptionDTO;
import com.rivercom.claw_machine_backend.dto.CampaignQuickRedemptionItemDTO;
import com.rivercom.claw_machine_backend.repository.IncomeRecordsRepository;
import com.rivercom.claw_machine_backend.repository.MachineCampaignRepository;
import com.rivercom.claw_machine_backend.repository.MachineExpenseRecordsRepository;
import com.rivercom.claw_machine_backend.repository.PointTransactionRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRedemptionsRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import com.rivercom.claw_machine_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.flywaydb.core.Flyway;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsService {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;
    private final Flyway flyway;
    private final MachineCampaignRepository machineCampaignRepository;
    private final IncomeRecordsRepository incomeRecordsRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final PrizeRepository prizeRepository;
    private final MachineExpenseRecordsRepository machineExpenseRecordsRepository;
    private final PrizeRedemptionsRepository prizeRedemptionsRepository;
    private final UserRepository userRepository;

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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible exportar la base de datos.");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible importar la base de datos.");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible eliminar la base de datos.");
        }

        recreateEmptyDatabase();
    }

    @Transactional
    public void resetAllUserPoints() {
        List<User> usersWithPoints = userRepository.findByCurrentPointsGreaterThan(0);

        if (usersWithPoints.isEmpty()) {
            return;
        }

        List<PointTransaction> resetTransactions = new ArrayList<>();

        for (User user : usersWithPoints) {
            int previousBalance = user.getCurrentPoints();
            user.setCurrentPoints(0);

            PointTransaction transaction = new PointTransaction();
            transaction.setUser(user);
            transaction.setTransactionType(TransactionType.ADJUSTMENT);
            transaction.setPointsDelta(-previousBalance);
            transaction.setPreviousBalance(previousBalance);
            transaction.setNewBalance(0);
            transaction.setNotes("Reinicio manual de puntos desde reportes");
            resetTransactions.add(transaction);
        }

        userRepository.saveAll(usersWithPoints);
        pointTransactionRepository.saveAll(resetTransactions);
    }

    public WeeklyReportPeriod getCurrentWeeklyReportPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        return new WeeklyReportPeriod(weekStart, weekEnd);
    }

    public PdfReportExport generateCurrentWeeklySummaryReport() {
        WeeklyReportPeriod period = getCurrentWeeklyReportPeriod();
        return generateWeeklySummaryReport(period.weekStart(), period.weekEnd());
    }

    public PdfReportExport generateWeeklySummaryReport(LocalDate weekStart, LocalDate weekEnd) {
        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.plusDays(1).atStartOfDay().minusNanos(1);

        List<PointTransaction> earnedTransactions = pointTransactionRepository.findByTransactionTypeAndCreatedAtBetween(
                TransactionType.EARN,
                startDateTime,
                endDateTime
        );
        List<PrizeRedemption> registeredRedemptions = prizeRedemptionsRepository.findByRedeemedAtBetween(startDateTime, endDateTime);
        List<MachineExpenseRecords> quickRedemptions = machineExpenseRecordsRepository.findByRegisteredAtBetween(startDateTime, endDateTime);
        List<IncomeRecords> incomeRecords = incomeRecordsRepository.findByRegisteredAtBetween(startDateTime, endDateTime);

        int earnedPointsTotal = earnedTransactions.stream()
                .map(PointTransaction::getPointsDelta)
                .filter(points -> points != null && points > 0)
                .mapToInt(Integer::intValue)
                .sum();

        List<Map<String, Object>> earnedPointsByUserRows = earnedTransactions.stream()
                .filter(transaction -> transaction.getPointsDelta() != null && transaction.getPointsDelta() > 0)
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getUser().getName(),
                        LinkedHashMap::new,
                        Collectors.summingInt(PointTransaction::getPointsDelta)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("userName", entry.getKey());
                    row.put("pointsText", entry.getValue() + " pts");
                    return row;
                })
                .toList();

        int registeredSpentPointsTotal = registeredRedemptions.stream()
                .map(PrizeRedemption::getPointsSpent)
                .filter(points -> points != null && points > 0)
                .mapToInt(Integer::intValue)
                .sum();

        BigDecimal incomeTotal = incomeRecords.stream()
                .map(IncomeRecords::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal registeredExpenseTotal = registeredRedemptions.stream()
                .map(redemption -> redemption.getPrize().getCost())
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal quickExpenseTotal = quickRedemptions.stream()
                .map(MachineExpenseRecords::getTotalCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = registeredExpenseTotal.add(quickExpenseTotal);

        List<Map<String, Object>> summaryRows = List.of(
                createWeeklySummaryRow(
                        "Lapso de fecha",
                        "Semana actual de lunes a domingo",
                        formatDateRange(weekStart, weekEnd)
                ),
                createWeeklySummaryRow(
                        "Puntos canjeados",
                        "Registrados: " + registeredSpentPointsTotal + " pts (" + registeredRedemptions.size()
                                + " canjes) | Rapidos / no registrados: 0 pts (" + quickRedemptions.size() + " operaciones)",
                        registeredSpentPointsTotal + " pts"
                ),
                createWeeklySummaryRow(
                        "Dinero ganado",
                        "Ingresos registrados: " + incomeRecords.size(),
                        formatCurrency(incomeTotal)
                ),
                createWeeklySummaryRow(
                        "Dinero salido",
                        "Registrados: " + formatCurrency(registeredExpenseTotal)
                                + " | Rapidos / no registrados: " + formatCurrency(quickExpenseTotal),
                        formatCurrency(totalExpense)
                )
        );

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Reporte semanal");
        parameters.put("REPORT_GENERATED_AT", toDate(LocalDateTime.now()));
        parameters.put("TOTAL_EARNED_POINTS", earnedPointsTotal + " pts");
        parameters.put("EARNED_POINTS_BY_USER_DATA", createDataSource(
                earnedPointsByUserRows.isEmpty() ? List.of(Map.of("userName", "Sin acumulaciones", "pointsText", "0 pts")) : earnedPointsByUserRows
        ));
        parameters.put("WEEKLY_SUMMARY_DATA", createDataSource(summaryRows));

        JasperPrint jasperPrint = fillSingleRecordReport("reports/weekly-summary-report.jrxml", parameters);
        return createPdfExport(
                "reporte-semanal-" + weekStart.format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf",
                List.of(jasperPrint)
        );
    }

    public PdfReportExport generateAddPointsTicket(Long transactionId) {
        PointTransaction transaction = pointTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La transaccion no existe."));

        List<Map<String, Object>> redeemablePrizes = prizeRepository.findByIsActive(true).stream()
                .filter(prize -> prize.getPointsCost() != null
                        && prize.getPointsCost() > 0
                        && prize.getPointsCost() <= transaction.getNewBalance())
                .sorted(Comparator.comparing(Prize::getPointsCost).thenComparing(Prize::getName))
                .map(prize -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("prizeName", prize.getName());
                    row.put("prizeCost", prize.getPointsCost());
                    return row;
                })
                .toList();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("USER_NAME", transaction.getUser().getName());
        parameters.put("USER_PHONE", transaction.getUser().getPhone());
        parameters.put("USER_POINTS_BEFORE", transaction.getPreviousBalance());
        parameters.put("POINTS_ADDED", transaction.getPointsDelta());
        parameters.put("USER_POINTS_AFTER", transaction.getNewBalance());
        parameters.put("REPORT_TIMESTAMP", toDate(transaction.getCreatedAt()));
        parameters.put("REDEEMABLE_PRIZES_DATA", createDataSource(redeemablePrizes));

        JasperPrint jasperPrint = fillSingleRecordReport("reports/add-points-ticket.jrxml", parameters);
        return createPdfExport(
                "ticket-agregar-puntos-" + transactionId + ".pdf",
                List.of(jasperPrint)
        );
    }

    public PdfReportExport generateQuickRedemptionTicket(List<Long> expenseIds) {
        if (expenseIds == null || expenseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar al menos un canje rapido.");
        }

        List<MachineExpenseRecords> records = machineExpenseRecordsRepository.findByIdIn(expenseIds);
        Map<Long, MachineExpenseRecords> recordsById = records.stream()
                .collect(Collectors.toMap(MachineExpenseRecords::getId, Function.identity()));

        List<MachineExpenseRecords> orderedRecords = new ArrayList<>();
        for (Long expenseId : expenseIds) {
            MachineExpenseRecords record = recordsById.get(expenseId);
            if (record == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uno de los canjes rapidos no existe.");
            }

            orderedRecords.add(record);
        }

        MachineExpenseRecords firstRecord = orderedRecords.get(0);
        int totalQuantity = orderedRecords.stream()
                .map(MachineExpenseRecords::getQuantity)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        int totalCost = orderedRecords.stream()
                .map(MachineExpenseRecords::getTotalCost)
                .filter(value -> value != null)
                .mapToInt(BigDecimal::intValue)
                .sum();

        String operationItemsText = orderedRecords.stream()
                .map(record -> String.format(
                        "Premio: %s%nCategoria: %s%nCantidad: %d",
                        record.getPrize().getName(),
                        record.getPrize().getCategory().getName(),
                        record.getQuantity()
                ))
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("OPERATION_ITEMS_TEXT", operationItemsText);
        parameters.put("TOTAL_QUANTITY", totalQuantity);
        parameters.put("TOTAL_COST", totalCost);
        parameters.put("REPORT_TIMESTAMP", toDate(firstRecord.getRegisteredAt()));

        JasperPrint jasperPrint = fillSingleRecordReport("reports/quick-redemption-ticket.jrxml", parameters);

        return createPdfExport(
                "ticket-canje-rapido-" + LocalDateTime.now().format(FILE_NAME_FORMATTER) + ".pdf",
                List.of(jasperPrint)
        );
    }

    public PdfReportExport generateUserRedemptionTicket(Long redemptionId) {
        PrizeRedemption redemption = prizeRedemptionsRepository.findById(redemptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El canje no existe."));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("USER_NAME", redemption.getUser().getName());
        parameters.put("USER_PHONE", redemption.getUser().getPhone());
        parameters.put("PRIZE_NAME", redemption.getPrize().getName());
        parameters.put("PRIZE_CATEGORY", redemption.getPrize().getCategory().getName());
        parameters.put("POINTS_BEFORE", redemption.getPointTransaction().getPreviousBalance());
        parameters.put("POINTS_SPENT", redemption.getPointsSpent());
        parameters.put("POINTS_AFTER", redemption.getPointTransaction().getNewBalance());
        parameters.put("REPORT_TIMESTAMP", toDate(redemption.getRedeemedAt()));

        JasperPrint jasperPrint = fillSingleRecordReport("reports/user-redemption-ticket.jrxml", parameters);
        return createPdfExport(
                "ticket-canje-usuario-" + redemptionId + ".pdf",
                List.of(jasperPrint)
        );
    }

    public List<CampaignAddPointsTransactionDTO> listAddPointsTransactions(Long campaignId, boolean unassignedOnly) {
        if (unassignedOnly) {
            return pointTransactionRepository.findByTransactionTypeAndCampaignIsNullOrderByCreatedAtDesc(TransactionType.EARN)
                    .stream()
                    .map(this::toCampaignAddPointsTransaction)
                    .toList();
        }

        if (campaignId == null) {
            return pointTransactionRepository.findByTransactionTypeOrderByCreatedAtDesc(TransactionType.EARN)
                    .stream()
                    .map(this::toCampaignAddPointsTransaction)
                    .toList();
        }

        machineCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La campaña no existe."));

        return pointTransactionRepository.findByTransactionTypeAndCampaignIdOrderByCreatedAtDesc(TransactionType.EARN, campaignId)
                .stream()
                .map(this::toCampaignAddPointsTransaction)
                .toList();
    }

    public List<CampaignPrizeRedemptionDTO> listCampaignPrizeRedemptions(Long campaignId) {
        MachineCampaign campaign = machineCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La campaña no existe."));

        return prizeRedemptionsRepository.findByCampaignIdOrderByRedeemedAtDesc(campaignId)
                .stream()
                .map(redemption -> new CampaignPrizeRedemptionDTO(
                        redemption.getId(),
                        campaign.getId(),
                        campaign.getName(),
                        redemption.getUser().getName(),
                        redemption.getUser().getPhone(),
                        redemption.getPrize().getName(),
                        redemption.getPrize().getCategory().getName(),
                        redemption.getPointsSpent(),
                        redemption.getPointTransaction().getPreviousBalance(),
                        redemption.getPointTransaction().getNewBalance(),
                        formatDateTime(redemption.getRedeemedAt())
                ))
                .toList();
    }

    public List<CampaignQuickRedemptionDTO> listCampaignQuickRedemptions(Long campaignId) {
        MachineCampaign campaign = machineCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La campaña no existe."));

        Map<String, List<MachineExpenseRecords>> recordsByOperation = machineExpenseRecordsRepository
                .findByCampaignIdOrderByRegisteredAtDesc(campaignId)
                .stream()
                .collect(Collectors.groupingBy(
                        MachineExpenseRecords::getOperationGroupId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return recordsByOperation.values().stream()
                .map(records -> toQuickRedemptionDTO(campaign, records))
                .toList();
    }

    private CampaignQuickRedemptionDTO toQuickRedemptionDTO(MachineCampaign campaign, List<MachineExpenseRecords> records) {
        MachineExpenseRecords firstRecord = records.get(0);
        List<CampaignQuickRedemptionItemDTO> items = records.stream()
                .map(record -> new CampaignQuickRedemptionItemDTO(
                        record.getId(),
                        record.getPrize().getName(),
                        record.getPrize().getCategory().getName(),
                        record.getQuantity(),
                        toInteger(record.getUnitCost()),
                        toInteger(record.getTotalCost()),
                        record.getRestocked()
                ))
                .toList();

        int totalQuantity = records.stream()
                .map(MachineExpenseRecords::getQuantity)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        int totalCost = records.stream()
                .map(MachineExpenseRecords::getTotalCost)
                .filter(value -> value != null)
                .mapToInt(BigDecimal::intValue)
                .sum();

        boolean restocked = records.stream().allMatch(record -> Boolean.TRUE.equals(record.getRestocked()));

        return new CampaignQuickRedemptionDTO(
                firstRecord.getOperationGroupId(),
                campaign.getId(),
                campaign.getName(),
                totalQuantity,
                totalCost,
                restocked,
                formatDateTime(firstRecord.getRegisteredAt()),
                records.stream().map(MachineExpenseRecords::getId).toList(),
                items
        );
    }

    private CampaignAddPointsTransactionDTO toCampaignAddPointsTransaction(PointTransaction transaction) {
        MachineCampaign campaign = transaction.getCampaign();
        return new CampaignAddPointsTransactionDTO(
                transaction.getId(),
                campaign != null ? campaign.getId() : null,
                campaign != null ? campaign.getName() : null,
                transaction.getUser().getName(),
                transaction.getUser().getPhone(),
                transaction.getPointsDelta(),
                transaction.getPreviousBalance(),
                transaction.getNewBalance(),
                formatDateTime(transaction.getCreatedAt())
        );
    }

    private JasperPrint fillSingleRecordReport(String classpathReport, Map<String, Object> parameters) {
        try {
            JasperReport report = compileReport(classpathReport);
            return JasperFillManager.fillReport(report, parameters, new JREmptyDataSource(1));
        } catch (JRException | IOException exception) {
            log.error("No fue posible generar el reporte {}", classpathReport, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible generar el ticket.");
        }
    }

    private JasperReport compileReport(String classpathReport) throws IOException, JRException {
        ClassPathResource resource = new ClassPathResource(classpathReport);
        try (InputStream inputStream = resource.getInputStream()) {
            return JasperCompileManager.compileReport(inputStream);
        }
    }

    private PdfReportExport createPdfExport(String fileName, List<JasperPrint> jasperPrints) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrints));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
            exporter.exportReport();
            return new PdfReportExport(fileName, outputStream.toByteArray());
        } catch (JRException | IOException exception) {
            log.error("No fue posible exportar el reporte PDF {}", fileName, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible exportar el ticket.");
        }
    }

    private JRDataSource createDataSource(List<Map<String, Object>> items) {
        List<Map<String, ?>> rows = new ArrayList<>();

        if (items.isEmpty()) {
            Map<String, Object> placeholderRow = new HashMap<>();
            placeholderRow.put("prizeName", "Aun no hay premios disponibles");
            placeholderRow.put("prizeCost", 0);
            rows.add(placeholderRow);
        } else {
            rows.addAll(items);
        }

        return new JRMapCollectionDataSource(rows);
    }

    private Date toDate(LocalDateTime value) {
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Map<String, Object> createWeeklySummaryRow(String concept, String detail, String value) {
        Map<String, Object> row = new HashMap<>();
        row.put("concept", concept);
        row.put("detail", detail);
        row.put("value", value);
        return row;
    }

    private Integer toInteger(BigDecimal value) {
        return value == null ? 0 : value.intValue();
    }

    private String formatDateRange(LocalDate weekStart, LocalDate weekEnd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return weekStart.format(formatter) + " al " + weekEnd.format(formatter);
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal normalizedAmount = amount == null ? BigDecimal.ZERO : amount;
        return "$" + normalizedAmount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }

        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
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

    public record PdfReportExport(String fileName, byte[] content) {
    }

    public record WeeklyReportPeriod(LocalDate weekStart, LocalDate weekEnd) {
    }
}
