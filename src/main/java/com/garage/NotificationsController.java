
package com.garage.garagemanagementsystem.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class NotificationsController {

    private final JdbcTemplate jdbcTemplate;

    public NotificationsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {

        // 1. Low-stock inventory alerts
        List<Map<String, Object>> lowStockAlerts =
                jdbcTemplate.queryForList(
                        "SELECT id, part_name, part_number, quantity, reorder_level " +
                        "FROM inventory " +
                        "WHERE quantity IS NOT NULL " +
                        "AND reorder_level IS NOT NULL " +
                        "AND quantity <= reorder_level " +
                        "ORDER BY quantity ASC"
                );

        // 2. Overdue invoices:
        // Due date is before today and status is not paid.
        List<Map<String, Object>> overdueAlerts =
                jdbcTemplate.queryForList(
                        "SELECT id, customer_id, final_amount, due_date, status " +
                        "FROM invoices " +
                        "WHERE due_date IS NOT NULL " +
                        "AND due_date < CURRENT_DATE " +
                        "AND (status IS NULL OR " +
                        "UPPER(TRIM(status)) NOT IN " +
                        "('PAID', 'COMPLETED', 'CANCELLED', 'CANCELED')) " +
                        "ORDER BY due_date ASC"
                );

        // 3. Upcoming services:
        // Estimated completion is today through the next 7 days.
        // Exclude completed and cancelled bookings.
        List<Map<String, Object>> upcomingServices =
                jdbcTemplate.queryForList(
                        "SELECT id, vehicle_id, booking_date, " +
                        "estimated_completion, status, priority " +
                        "FROM service_bookings " +
                        "WHERE estimated_completion >= CURRENT_DATE " +
                        "AND estimated_completion <= DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) " +
                        "AND (status IS NULL OR " +
                        "UPPER(TRIM(status)) NOT IN " +
                        "('COMPLETED', 'CANCELLED', 'CANCELED')) " +
                        "ORDER BY estimated_completion ASC"
                );

        // Supply all lists required by notifications.html
        model.addAttribute("lowStockAlerts", lowStockAlerts);
        model.addAttribute("overdueAlerts", overdueAlerts);
        model.addAttribute("upcomingServices", upcomingServices);

        // Supply summary card counts
        int lowStockCount = lowStockAlerts.size();
        int overdueCount = overdueAlerts.size();
        int upcomingCount = upcomingServices.size();

        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("upcomingCount", upcomingCount);

        model.addAttribute(
                "totalNotifications",
                lowStockCount + overdueCount + upcomingCount
        );

        return "notifications";
    }
}
