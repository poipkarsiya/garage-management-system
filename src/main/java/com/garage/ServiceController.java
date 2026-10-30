
package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.Service;
import com.garage.garagemanagementsystem.repository.ServiceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
public class ServiceController {

    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("/services")
    public String listServices(Model model) {
        model.addAttribute("services", serviceRepository.findAll());
        return "services";
    }

    @GetMapping("/services/add")
    public String addService(Model model) {
        model.addAttribute("service", new Service());
        model.addAttribute("isEdit", false);
        return "service-form";
    }

    @GetMapping("/services/edit/{id}")
    public String editService(@PathVariable Long id, Model model) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Service not found: " + id));

        model.addAttribute("service", service);
        model.addAttribute("isEdit", true);
        return "service-form";
    }

    @PostMapping("/services/save")
    public String saveService(
            @ModelAttribute("service") Service service,
            Model model) {

        if (service.getName() == null || service.getName().isBlank()) {
            model.addAttribute("errorMessage", "Please enter a service name.");
            model.addAttribute("isEdit", service.getId() != null);
            return "service-form";
        }

        if (service.getBasePrice() != null
                && service.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("errorMessage", "Price cannot be negative.");
            model.addAttribute("isEdit", service.getId() != null);
            return "service-form";
        }

        if (service.getEstimatedHours() != null
                && service.getEstimatedHours() < 0) {
            model.addAttribute("errorMessage", "Estimated hours cannot be negative.");
            model.addAttribute("isEdit", service.getId() != null);
            return "service-form";
        }

        try {
            serviceRepository.saveAndFlush(service);
            return "redirect:/services";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute(
                    "errorMessage",
                    "Service could not be saved. Check the server console for the database error."
            );
            model.addAttribute("isEdit", service.getId() != null);
            return "service-form";
        }
    }

    @PostMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Long id) {
        try {
            serviceRepository.deleteById(id);
            serviceRepository.flush();
            return "redirect:/services";

        } catch (DataIntegrityViolationException e) {
            return "redirect:/services?error=service-in-use";
        }
    }
}
