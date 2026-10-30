
package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.DiagnosisResult;
import com.garage.garagemanagementsystem.service.DiagnosisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @GetMapping("/ai-diagnosis")
    public String showDiagnosisPage(Model model) {
        model.addAttribute("symptoms", "");
        model.addAttribute("vehicleType", "2 Wheeler");
        model.addAttribute("fuelType", "Petrol");
        return "ai-diagnosis";
    }

    @PostMapping("/ai-diagnosis")
    public String diagnose(
            @RequestParam("symptoms") String symptoms,
            @RequestParam(value = "vehicleType", required = false) String vehicleType,
            @RequestParam(value = "fuelType", required = false) String fuelType,
            Model model) {

        model.addAttribute("symptoms", symptoms);
        model.addAttribute("vehicleType", vehicleType);
        model.addAttribute("fuelType", fuelType);

        if (symptoms == null || symptoms.isBlank()) {
            model.addAttribute("errorMessage", "Please enter the vehicle symptoms.");
            return "ai-diagnosis";
        }

        DiagnosisResult result = diagnosisService.diagnose(
                symptoms,
                vehicleType,
                fuelType
        );

        model.addAttribute("result", result);
        return "ai-diagnosis";
    }
}
