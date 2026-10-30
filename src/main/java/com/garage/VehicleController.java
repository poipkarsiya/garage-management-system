package com.garage.controller;

import com.garage.garagemanagementsystem.entity.Customer;
import com.garage.garagemanagementsystem.entity.Vehicle;
import com.garage.garagemanagementsystem.repository.CustomerRepository;
import com.garage.garagemanagementsystem.repository.VehicleRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    public VehicleController(
            VehicleRepository vehicleRepository,
            CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/vehicles")
    public String vehicles(Model model) {
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "vehicles";
    }

    @GetMapping("/vehicles/add")
    public String addVehicle(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        return "vehicle-add";
    }

    @PostMapping("/vehicles/save")
    public String saveVehicle(
            @RequestParam Long customerId,
            @RequestParam String registrationNumber,
            @RequestParam String vehicleType,
            @RequestParam String model,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String fuelType,
            Model viewModel) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Selected customer not found"));

        Vehicle vehicle = new Vehicle();
        vehicle.setCustomer(customer);
        vehicle.setRegistrationNumber(registrationNumber.trim().toUpperCase());
        vehicle.setVehicleNumber(registrationNumber.trim().toUpperCase());
        vehicle.setVehicleType(vehicleType);
        vehicle.setModel(model.trim());
        vehicle.setYear(year);
        vehicle.setVehicleYear(year);
        vehicle.setFuelType(fuelType);

        try {
            vehicleRepository.saveAndFlush(vehicle);
            return "redirect:/vehicles";
        } catch (DataIntegrityViolationException ex) {
            viewModel.addAttribute("customers", customerRepository.findAll());
            viewModel.addAttribute("errorMessage",
                    "Could not save this vehicle. Check whether the registration number already exists or violates a database constraint.");
            return "vehicle-add";
        }
    }

    @GetMapping("/vehicles/delete/{id}")
    public String deleteVehicle(@PathVariable Long id,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
    try {
        vehicleRepository.deleteById(id);
        vehicleRepository.flush();

        redirectAttributes.addFlashAttribute(
            "successMessage", "Vehicle deleted successfully."
        );

    } catch (org.springframework.dao.DataIntegrityViolationException e) {
        redirectAttributes.addFlashAttribute(
            "errorMessage",
            "This vehicle is linked to service bookings. Please remove or reassign its bookings before deleting it."
        );
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute(
            "errorMessage", "Could not delete vehicle. Please check the related records."
        );
    }

    return "redirect:/vehicles";
}
}
