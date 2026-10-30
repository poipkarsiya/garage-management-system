
package com.garage.garagemanagementsystem.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class ReportsController {

    private final JdbcTemplate jdbcTemplate;

    public ReportsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        LocalDate end = parseDate(endDate, LocalDate.now());
        LocalDate start = parseDate(
                startDate, end.withDayOfMonth(1));

        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        String startText = start.toString();
        String endExclusive = end.plusDays(1).toString();

        // Invoice totals for the selected period
        Map<String, Object> revenue = jdbcTemplate.queryForMap(
                """
                SELECT
                    COALESCE(SUM(final_amount), 0) AS billed,
                    COALESCE(SUM(CASE WHEN status = 'PAID'
                        THEN final_amount ELSE 0 END), 0) AS collected,
                    COALESCE(SUM(CASE WHEN status IN ('UNPAID', 'OVERDUE')
                        THEN final_amount ELSE 0 END), 0) AS outstanding,
                    COUNT(*) AS invoice_count,
                    COALESCE(SUM(CASE WHEN status = 'PAID'
                        THEN 1 ELSE 0 END), 0) AS paid_count,
                    COALESCE(SUM(CASE WHEN status = 'UNPAID'
                        THEN 1 ELSE 0 END), 0) AS unpaid_count,
                    COALESCE(SUM(CASE WHEN status = 'OVERDUE'
                        THEN 1 ELSE 0 END), 0) AS overdue_count
                FROM invoices
                WHERE created_at >= ?
                  AND created_at < ?
                """,
                startText, endExclusive
        );

        // Overall inventory information
        Map<String, Object> inventory = jdbcTemplate.queryForMap(
                """
                SELECT
                    COUNT(*) AS part_count,
                    COALESCE(SUM(quantity), 0) AS total_units,
                    COALESCE(SUM(quantity * unit_price), 0) AS stock_value,
                    COALESCE(SUM(CASE
                        WHEN quantity <= reorder_level THEN 1
                        ELSE 0 END), 0) AS low_stock_count
                FROM inventory
                """
        );

        List<Map<String, Object>> lowStockItems =
                jdbcTemplate.queryForList(
                        """
                        SELECT part_name, part_number, quantity,
                               reorder_level, unit_price, supplier
                        FROM inventory
                        WHERE quantity <= reorder_level
                        ORDER BY quantity ASC
                        """
                );

        // Monthly invoice totals for the selected period
        List<Map<String, Object>> monthlyRevenue =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            DATE_FORMAT(created_at, '%Y-%m') AS month,
                            COUNT(*) AS invoice_count,
                            COALESCE(SUM(final_amount), 0) AS billed,
                            COALESCE(SUM(CASE WHEN status = 'PAID'
                                THEN final_amount ELSE 0 END), 0) AS collected
                        FROM invoices
                        WHERE created_at >= ?
                          AND created_at < ?
                        GROUP BY DATE_FORMAT(created_at, '%Y-%m')
                        ORDER BY month
                        """,
                        startText, endExclusive
                );

        // Booking status summary
        List<Map<String, Object>> bookingSummary =
                jdbcTemplate.queryForList(
                        """
                        SELECT status, COUNT(*) AS total
                        FROM service_bookings
                        GROUP BY status
                        ORDER BY status
                        """
                );

        Long totalBookings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_bookings",
                Long.class
        );

        model.addAttribute("startDate", start.toString());
        model.addAttribute("endDate", end.toString());
        model.addAttribute("revenue", revenue);
        model.addAttribute("inventory", inventory);
        model.addAttribute("lowStockItems", lowStockItems);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("bookingSummary", bookingSummary);
        model.addAttribute("totalBookings",
                totalBookings == null ? 0L : totalBookings);

        return "reports";
    }

    @GetMapping("/reports/export")
    public void exportReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {

        LocalDate end = parseDate(endDate, LocalDate.now());
        LocalDate start = parseDate(
                startDate, end.withDayOfMonth(1));

        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        List<Map<String, Object>> invoices =
                jdbcTemplate.queryForList(
                        """
                        SELECT id, customer_id, booking_id,
                               total_amount, tax, discount,
                               final_amount, status, due_date,
                               paid_date, created_at
                        FROM invoices
                        WHERE created_at >= ?
                          AND created_at < ?
                        ORDER BY created_at DESC
                        """,
                        start.toString(), end.plusDays(1).toString()
                );

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=ssmotors-report.csv"
        );

        var writer = response.getWriter();
        writer.println(
                "Invoice ID,Customer ID,Booking ID,Total Amount,Tax," +
                "Discount,Final Amount,Status,Due Date,Paid Date,Created At"
        );

        for (Map<String, Object> row : invoices) {
            writer.println(
                    csv(row.get("id")) + "," +
                    csv(row.get("customer_id")) + "," +
                    csv(row.get("booking_id")) + "," +
                    csv(row.get("total_amount")) + "," +
                    csv(row.get("tax")) + "," +
                    csv(row.get("discount")) + "," +
                    csv(row.get("final_amount")) + "," +
                    csv(row.get("status")) + "," +
                    csv(row.get("due_date")) + "," +
                    csv(row.get("paid_date")) + "," +
                    csv(row.get("created_at"))
            );
        }

        writer.flush();
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException ex) {
            return fallback;
        }
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString().replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}