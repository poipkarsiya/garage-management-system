package com.garage.garagemanagementsystem.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    public DashboardController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // Total customers
        Integer totalCustomers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers",
                Integer.class
        );

        // Total vehicles
        Integer totalVehicles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vehicles",
                Integer.class
        );

        // Total bookings
        Integer totalBookings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM service_bookings",
                Integer.class
        );

        // Revenue from paid invoices
        BigDecimal totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(final_amount), 0) " +
                "FROM invoices WHERE UPPER(TRIM(status)) = 'PAID'",
                BigDecimal.class
        );

        // Latest eight bookings
        List<Map<String, Object>> recentActivities =
                jdbcTemplate.queryForList(
                        "SELECT id, vehicle_id, booking_date, status, created_at " +
                        "FROM service_bookings " +
                        "ORDER BY created_at DESC, id DESC LIMIT 8"
                );

        model.addAttribute("totalCustomers",
                totalCustomers == null ? 0 : totalCustomers);

        model.addAttribute("totalVehicles",
                totalVehicles == null ? 0 : totalVehicles);

        model.addAttribute("totalBookings",
                totalBookings == null ? 0 : totalBookings);

        model.addAttribute("totalRevenue",
                totalRevenue == null ? BigDecimal.ZERO : totalRevenue);

        model.addAttribute("recentActivities", recentActivities);

        return "dashboard";
    }
}
