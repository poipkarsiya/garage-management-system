package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.ServiceBooking;
import com.garage.garagemanagementsystem.entity.Vehicle;
import com.garage.garagemanagementsystem.entity.Service;
import com.garage.garagemanagementsystem.repository.ServiceBookingRepository;
import com.garage.garagemanagementsystem.repository.ServiceRepository;
import com.garage.garagemanagementsystem.repository.VehicleRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class ServiceBookingController {

    private final ServiceBookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRepository serviceRepository;
    private final JdbcTemplate jdbcTemplate;

    public ServiceBookingController(
            ServiceBookingRepository bookingRepository,
            VehicleRepository vehicleRepository,
            ServiceRepository serviceRepository,
            JdbcTemplate jdbcTemplate) {

        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceRepository = serviceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // Display all service bookings
    @GetMapping("/service-bookings")
    public String bookings(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "service-bookings";
    }

    // Display the add-booking form
    @GetMapping("/service-bookings/add")
    public String addBooking(Model model) {
        model.addAttribute("booking", new ServiceBooking());
        loadFormData(model);
        return "service-booking-add";
    }

    // Save a new service booking
    @PostMapping("/service-bookings/save")
    public String saveBooking(
            @ModelAttribute("booking") ServiceBooking booking,
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("serviceId") Long serviceId,
            @RequestParam(value = "mechanicId", required = false) Long mechanicId,
            Model model) {

        try {
            // Validate vehicle selection
            Vehicle vehicle = vehicleRepository.findById(vehicleId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected vehicle was not found."));

            // Validate service selection
            Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected service was not found."));

            // Only allow 2-wheelers and 3-wheelers
            String vehicleType = vehicle.getVehicleType();

            if (vehicleType == null
                    || !(vehicleType.equalsIgnoreCase("2 Wheeler")
                    || vehicleType.equalsIgnoreCase("3 Wheeler"))) {

                throw new IllegalArgumentException(
                        "Only 2-wheelers and 3-wheelers can be booked.");
            }

            // Validate booking date
            LocalDate bookingDate = booking.getBookingDate();
            LocalDate completionDate = booking.getEstimatedCompletion();

            if (bookingDate == null) {
                throw new IllegalArgumentException(
                        "Please select a booking date.");
            }

            if (completionDate != null
                    && completionDate.isBefore(bookingDate)) {

                throw new IllegalArgumentException(
                        "Estimated completion date cannot be before the booking date.");
            }

            // Validate optional mechanic ID against users table
            if (mechanicId != null) {
                Long mechanicCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE id = ?",
                        Long.class,
                        mechanicId);

                if (mechanicCount == null || mechanicCount == 0) {
                    throw new IllegalArgumentException(
                            "The mechanic ID does not exist. Leave it blank or enter a valid user ID.");
                }
            }

            // Set entity relationships and mechanic
            booking.setVehicle(vehicle);
            booking.setService(service);
            booking.setMechanicId(mechanicId);

            // Set defaults when fields are empty
            if (booking.getPriority() == null
                    || booking.getPriority().isBlank()) {
                booking.setPriority("MEDIUM");
            }

            if (booking.getStatus() == null
                    || booking.getStatus().isBlank()) {
                booking.setStatus("PENDING");
            }

            // Save and flush so database errors appear here
            bookingRepository.saveAndFlush(booking);

            return "redirect:/service-bookings";

        } catch (Exception e) {
            e.printStackTrace();

            model.addAttribute(
                    "errorMessage",
                    "Booking could not be saved: "
                            + (e.getMessage() == null
                            ? e.getClass().getSimpleName()
                            : e.getMessage()));

            // Restore the form's dropdown data after an error
            loadFormData(model);

            return "service-booking-add";
        }
    }

    // Load eligible vehicles and available services into the form
    private void loadFormData(Model model) {
        model.addAttribute(
                "vehicles",
                vehicleRepository.findAll().stream()
                        .filter(v -> v.getVehicleType() != null)
                        .filter(v ->
                                v.getVehicleType().equalsIgnoreCase("2 Wheeler")
                                || v.getVehicleType().equalsIgnoreCase("3 Wheeler"))
                        .toList());

        model.addAttribute("services", serviceRepository.findAll());
    }

    // Delete a booking
    @GetMapping("/service-bookings/delete/{id}")
    public String deleteBooking(@PathVariable("id") Long id) {
        bookingRepository.deleteById(id);
        return "redirect:/service-bookings";
    }
}
