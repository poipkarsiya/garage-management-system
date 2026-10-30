package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.User;
import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserHomeController {

    private final JdbcTemplate jdbcTemplate;

    public UserHomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Find the customer associated with the logged-in user.
    private Long getCustomerId(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null
                || !Boolean.TRUE.equals(user.getEnabled())
                || !"USER".equalsIgnoreCase(user.getRole())) {
            return null;
        }

        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM customers WHERE user_id = ?",
                (rs, rowNum) -> rs.getLong("id"),
                user.getId()
        );

        return ids.isEmpty() ? null : ids.get(0);
    }

    // MY VEHICLES
    @GetMapping("/vehicles")
    public String myVehicles(HttpSession session, Model model) {
        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> vehicles = jdbcTemplate.queryForList(
                "SELECT * FROM vehicles WHERE customer_id = ? ORDER BY id DESC",
                customerId
        );

        model.addAttribute("vehicles", vehicles);
        return "user-vehicles";
    }

    // MY BOOKINGS
    @GetMapping("/bookings")
    public String myBookings(HttpSession session, Model model) {
        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> bookings = jdbcTemplate.queryForList(
                "SELECT sb.id, sb.booking_date, sb.estimated_completion, " +
                "sb.status, sb.priority, sb.notes, " +
                "v.registration_number, v.make, v.model, " +
                "s.name AS service_name " +
                "FROM service_bookings sb " +
                "JOIN vehicles v ON sb.vehicle_id = v.id " +
                "JOIN services s ON sb.service_id = s.id " +
                "WHERE v.customer_id = ? " +
                "ORDER BY sb.id DESC",
                customerId
        );

        model.addAttribute("bookings", bookings);
        return "user-bookings";
    }

    // MY INVOICES
    @GetMapping("/invoices")
    public String myInvoices(HttpSession session, Model model) {
        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> invoices = jdbcTemplate.queryForList(
                "SELECT i.id, i.total_amount, i.tax, i.discount, " +
                "i.final_amount, i.status, i.due_date, i.paid_date, " +
                "i.created_at, sb.booking_date, s.name AS service_name " +
                "FROM invoices i " +
                "LEFT JOIN service_bookings sb ON i.booking_id = sb.id " +
                "LEFT JOIN services s ON sb.service_id = s.id " +
                "WHERE i.customer_id = ? " +
                "ORDER BY i.id DESC",
                customerId
        );

        model.addAttribute("invoices", invoices);
        return "user-invoices";
    }

    // OPEN BOOK SERVICE FORM
    @GetMapping("/book-service")
    public String bookService(HttpSession session, Model model) {
        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        model.addAttribute("vehicles", jdbcTemplate.queryForList(
                "SELECT id, registration_number, make, model " +
                "FROM vehicles WHERE customer_id = ? ORDER BY id DESC",
                customerId
        ));

        model.addAttribute("services", jdbcTemplate.queryForList(
                "SELECT id, name, base_price FROM services ORDER BY name"
        ));

        return "user-book-service";
    }

    // SAVE SERVICE BOOKING
    @PostMapping("/book-service")
    public String saveBooking(
            @RequestParam String vehicleChoice,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam Long serviceId,
            @RequestParam LocalDate bookingDate,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String registrationNumber,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String vehicleType,
            @RequestParam(required = false) Integer year,
            HttpSession session) {

        Long customerId = getCustomerId(session);

        if (customerId == null) {
            return "redirect:/login";
        }

        if (bookingDate == null || bookingDate.isBefore(LocalDate.now())) {
            return "redirect:/user/book-service?error=date";
        }

        try {
            // Check that the selected service exists.
            Integer serviceCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM services WHERE id = ?",
                    Integer.class,
                    serviceId
            );

            if (serviceCount == null || serviceCount == 0) {
                return "redirect:/user/book-service?error=service";
            }

            Long selectedVehicleId;

            if ("new".equalsIgnoreCase(vehicleChoice)) {

                // Validate the new vehicle details.
                if (registrationNumber == null || registrationNumber.isBlank()
                        || make == null || make.isBlank()
                        || model == null || model.isBlank()
                        || vehicleType == null || vehicleType.isBlank()) {
                    return "redirect:/user/book-service?error=details";
                }

                if (!vehicleType.equalsIgnoreCase("2 Wheeler")
                        && !vehicleType.equalsIgnoreCase("3 Wheeler")) {
                    return "redirect:/user/book-service?error=details";
                }

                // Do not allow a registration number already in use.
                Integer duplicateCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vehicles WHERE registration_number = ?",
                        Integer.class,
                        registrationNumber.trim()
                );

                if (duplicateCount != null && duplicateCount > 0) {
                    return "redirect:/user/book-service?error=details";
                }

                // Save the new vehicle and retrieve its generated ID.
                KeyHolder keyHolder = new GeneratedKeyHolder();

                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO vehicles " +
                            "(registration_number, customer_id, make, model, year, vehicle_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );

                    ps.setString(1, registrationNumber.trim());
                    ps.setLong(2, customerId);
                    ps.setString(3, make.trim());
                    ps.setString(4, model.trim());

                    if (year == null) {
                        ps.setNull(5, Types.INTEGER);
                    } else {
                        ps.setInt(5, year);
                    }

                    ps.setString(6, vehicleType);

                    return ps;
                }, keyHolder);

                Number generatedId = keyHolder.getKey();

                if (generatedId == null) {
                    return "redirect:/user/book-service?error=save";
                }

                selectedVehicleId = generatedId.longValue();

            } else if ("existing".equalsIgnoreCase(vehicleChoice)) {

                if (vehicleId == null) {
                    return "redirect:/user/book-service?error=vehicle";
                }

                // Ensure the existing vehicle belongs to this customer.
                Integer vehicleCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM vehicles " +
                        "WHERE id = ? AND customer_id = ?",
                        Integer.class,
                        vehicleId,
                        customerId
                );

                if (vehicleCount == null || vehicleCount == 0) {
                    return "redirect:/user/book-service?error=vehicle";
                }

                selectedVehicleId = vehicleId;

            } else {
                return "redirect:/user/book-service?error=vehicle";
            }

            // Save the booking.
            jdbcTemplate.update(
                    "INSERT INTO service_bookings " +
                    "(vehicle_id, service_id, booking_date, status, priority, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                    selectedVehicleId,
                    serviceId,
                    bookingDate,
                    "Pending",
                    "Normal",
                    notes == null || notes.isBlank() ? null : notes.trim()
            );

            return "redirect:/user/bookings";

        } catch (DataAccessException e) {
            e.printStackTrace();
            return "redirect:/user/book-service?error=save";
        }
    }
}
