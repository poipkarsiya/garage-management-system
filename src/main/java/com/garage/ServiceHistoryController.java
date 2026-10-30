
package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.ServiceHistory;
import com.garage.garagemanagementsystem.entity.Vehicle;
import com.garage.garagemanagementsystem.repository.ServiceHistoryRepository;
import com.garage.garagemanagementsystem.repository.VehicleRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class ServiceHistoryController {

    private final ServiceHistoryRepository historyRepository;
    private final VehicleRepository vehicleRepository;

    public ServiceHistoryController(
            ServiceHistoryRepository historyRepository,
            VehicleRepository vehicleRepository) {
        this.historyRepository = historyRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping("/service-history")
    public String listHistory(Model model) {
        model.addAttribute("histories", historyRepository.findAll());
        return "service-history";
    }

    @GetMapping("/service-history/add")
    public String addHistory(Model model) {
        model.addAttribute("history", new ServiceHistory());
        model.addAttribute("vehicles", vehicleRepository.findAll());
        return "service-history-add";
    }

    @PostMapping("/service-history/save")
    public String saveHistory(
            @RequestParam Long vehicleId,
            @RequestParam LocalDate serviceDate,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) BigDecimal cost,
            @RequestParam(required = false) String mechanicNotes,
            @RequestParam(required = false) String partsReplaced) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Vehicle not found"));

        ServiceHistory history = new ServiceHistory();
        history.setVehicle(vehicle);
        history.setServiceDate(serviceDate);
        history.setServiceType(serviceType);
        history.setCost(cost);
        history.setMechanicNotes(mechanicNotes);
        history.setPartsReplaced(partsReplaced);

        historyRepository.save(history);

        return "redirect:/service-history";
    }

    @PostMapping("/service-history/delete/{id}")
    public String deleteHistory(@PathVariable Long id) {
        historyRepository.deleteById(id);
        return "redirect:/service-history";
    }
}