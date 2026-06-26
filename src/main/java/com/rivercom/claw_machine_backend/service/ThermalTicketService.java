package com.rivercom.claw_machine_backend.service;

import com.rivercom.claw_machine_backend.domain.entity.MachineExpenseRecords;
import com.rivercom.claw_machine_backend.domain.entity.PointTransaction;
import com.rivercom.claw_machine_backend.domain.entity.Prize;
import com.rivercom.claw_machine_backend.domain.entity.PrizeRedemption;
import com.rivercom.claw_machine_backend.repository.MachineExpenseRecordsRepository;
import com.rivercom.claw_machine_backend.repository.PointTransactionRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRedemptionsRepository;
import com.rivercom.claw_machine_backend.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThermalTicketService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ThermalPrinterService thermalPrinterService;
    private final PointTransactionRepository pointTransactionRepository;
    private final PrizeRepository prizeRepository;
    private final PrizeRedemptionsRepository prizeRedemptionsRepository;
    private final MachineExpenseRecordsRepository machineExpenseRecordsRepository;

    @Value("${app.thermal-printer.line-width:32}")
    private int configuredLineWidth;

    public void printAddPointsTicket(Long transactionId) {
        PointTransaction transaction = pointTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La transaccion no existe."));

        List<PrizeLine> redeemablePrizes = prizeRepository.findByIsActive(true).stream()
                .filter(prize -> prize.getPointsCost() != null
                        && prize.getPointsCost() > 0
                        && prize.getPointsCost() <= transaction.getNewBalance())
                .sorted(Comparator.comparing(Prize::getPointsCost).thenComparing(Prize::getName))
                .map(prize -> new PrizeLine(prize.getName(), prize.getPointsCost()))
                .toList();

        EscPosTicket ticket = newTicket();
        ticket.centerBold("COMPROBANTE DE PUNTOS");
        ticket.separator();
        ticket.boldLine("Informacion del usuario");
        ticket.keyValue("Nombre", transaction.getUser().getName());
        ticket.keyValue("Telefono", transaction.getUser().getPhone());
        ticket.blank();
        ticket.boldLine("Operacion: agregar puntos");
        ticket.keyValue("Puntos previos", transaction.getPreviousBalance());
        ticket.keyValue("Puntos agregados", transaction.getPointsDelta());
        ticket.keyValue("Puntos actuales", transaction.getNewBalance(), true);
        ticket.dateTime(transaction.getCreatedAt());
        ticket.separator();
        ticket.centerBold("Usted puede canjear los siguientes premios actualmente");

        if (redeemablePrizes.isEmpty()) {
            ticket.center("Aun no hay premios disponibles");
        } else {
            redeemablePrizes.forEach(prize -> ticket.twoColumns(prize.name(), prize.pointsCost() + " pts"));
        }

        ticket.separator();
        ticket.center("Tus puntos quedan registrados en sistema.");
        ticket.center("Este comprobante es unicamente informativo.");
        thermalPrinterService.print(ticket.finish());
    }

    public void printQuickRedemptionTicket(List<Long> expenseIds) {
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
        EscPosTicket ticket = newTicket();
        ticket.centerBold("COMPROBANTE DE CANJE");
        ticket.separator();
        ticket.boldLine("Informacion de la operacion");
        for (int index = 0; index < orderedRecords.size(); index++) {
            MachineExpenseRecords record = orderedRecords.get(index);
            if (index > 0) {
                ticket.blank();
            }
            ticket.keyValue("Premio", record.getPrize().getName());
            ticket.keyValue("Cantidad", record.getQuantity());
        }
        ticket.blank();
        ticket.boldLine("Resumen");
        ticket.keyValue("Cantidad total", totalQuantity);
        ticket.center("Gracias por jugar. Disfruta tu premio.");
        ticket.dateTime(firstRecord.getRegisteredAt());
        ticket.separator();
        ticket.centerBold("Mensaje importante");
        ticket.center("Recuerde que si usted se registra puede acumular puntos para obtener premios mas grandes.");
        ticket.center("Realiza tu registro en caja.");
        ticket.separator();
        ticket.center("Gracias por su visita.");
        ticket.center("Conserve este comprobante.");
        thermalPrinterService.print(ticket.finish());
    }

    public void printUserRedemptionTicket(Long redemptionId) {
        PrizeRedemption redemption = prizeRedemptionsRepository.findById(redemptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El canje no existe."));

        EscPosTicket ticket = newTicket();
        ticket.centerBold("COMPROBANTE DE CANJE");
        ticket.separator();
        ticket.boldLine("Informacion del usuario");
        ticket.keyValue("Nombre", redemption.getUser().getName());
        ticket.keyValue("Telefono", redemption.getUser().getPhone());
        ticket.blank();
        ticket.boldLine("Informacion del canje");
        ticket.keyValue("Premio", redemption.getPrize().getName());
        ticket.keyValue("Categoria", redemption.getPrize().getCategory().getName());
        ticket.keyValue("Puntos antes", redemption.getPointTransaction().getPreviousBalance());
        ticket.keyValue("Puntos canjeados", redemption.getPointsSpent());
        ticket.keyValue("Puntos restantes", redemption.getPointTransaction().getNewBalance(), true);
        ticket.blank();
        ticket.center("Gracias por jugar. Disfruta tu premio.");
        ticket.dateTime(redemption.getRedeemedAt());
        ticket.separator();
        ticket.centerBold("Mensaje importante");
        ticket.center("Siga acumulando puntos para obtener premios mas grandes en sus proximos canjes.");
        ticket.separator();
        ticket.center("Gracias por su visita.");
        thermalPrinterService.print(ticket.finish());
    }

    private EscPosTicket newTicket() {
        return new EscPosTicket(Math.max(24, configuredLineWidth));
    }

    private record PrizeLine(String name, Integer pointsCost) {
    }

    private static class EscPosTicket {

        private static final byte ESC = 0x1B;
        private final int lineWidth;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private EscPosTicket(int lineWidth) {
            this.lineWidth = lineWidth;
            write(ESC, '@');
            align(1);
        }

        private void centerBold(String text) {
            align(1);
            bold(true);
            wrapped(text).forEach(this::line);
            bold(false);
            align(0);
        }

        private void center(String text) {
            align(1);
            wrapped(text).forEach(this::line);
            align(0);
        }

        private void boldLine(String text) {
            bold(true);
            wrapped(text).forEach(this::line);
            bold(false);
        }

        private void keyValue(String key, Object value) {
            keyValue(key, value, false);
        }

        private void keyValue(String key, Object value, boolean bold) {
            if (bold) {
                bold(true);
            }
            wrapped(key + ": " + safe(value)).forEach(this::line);
            if (bold) {
                bold(false);
            }
        }

        private void twoColumns(String left, String right) {
            String cleanLeft = sanitize(left);
            String cleanRight = sanitize(right);
            int leftWidth = Math.max(1, lineWidth - cleanRight.length() - 1);

            if (cleanLeft.length() > leftWidth) {
                wrapped(cleanLeft).forEach(this::line);
                line(padLeft(cleanRight, lineWidth));
                return;
            }

            line(cleanLeft + " ".repeat(lineWidth - cleanLeft.length() - cleanRight.length()) + cleanRight);
        }

        private void dateTime(LocalDateTime timestamp) {
            LocalDateTime value = timestamp == null ? LocalDateTime.now() : timestamp;
            line(value.format(DATE_FORMATTER) + " " + value.format(TIME_FORMATTER));
        }

        private void separator() {
            line("-".repeat(lineWidth));
        }

        private void blank() {
            line("");
        }

        private byte[] finish() {
            blank();
            blank();
            blank();
            write(0x1D, 'V', 0x42, 0x00);
            return outputStream.toByteArray();
        }

        private List<String> wrapped(String value) {
            String cleanValue = sanitize(value);
            List<String> lines = new ArrayList<>();

            for (String rawLine : cleanValue.split("\\R", -1)) {
                String remaining = rawLine.trim();
                if (remaining.isEmpty()) {
                    lines.add("");
                    continue;
                }

                while (remaining.length() > lineWidth) {
                    int cut = remaining.lastIndexOf(' ', lineWidth);
                    if (cut <= 0) {
                        cut = lineWidth;
                    }
                    lines.add(remaining.substring(0, cut).trim());
                    remaining = remaining.substring(cut).trim();
                }

                lines.add(remaining);
            }

            return lines;
        }

        private void line(String value) {
            outputStream.writeBytes(sanitize(value).getBytes(StandardCharsets.US_ASCII));
            outputStream.write('\n');
        }

        private void align(int alignment) {
            write(ESC, 'a', alignment);
        }

        private void bold(boolean enabled) {
            write(ESC, 'E', enabled ? 1 : 0);
        }

        private void write(int... values) {
            for (int value : values) {
                outputStream.write(value);
            }
        }

        private static String padLeft(String value, int width) {
            String cleanValue = sanitize(value);
            if (cleanValue.length() >= width) {
                return cleanValue;
            }
            return " ".repeat(width - cleanValue.length()) + cleanValue;
        }

        private static String safe(Object value) {
            return value == null ? "" : String.valueOf(value);
        }

        private static String sanitize(Object value) {
            String normalized = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");
            return normalized.replaceAll("[^\\x20-\\x7E\\n\\r]", "");
        }
    }
}
