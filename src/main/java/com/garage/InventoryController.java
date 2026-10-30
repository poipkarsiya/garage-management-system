
package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.Inventory;
import com.garage.garagemanagementsystem.repository.InventoryRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", inventoryRepository.findAll());
        return "inventory";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("item", new Inventory());
        model.addAttribute("pageTitle", "Add Spare Part");
        model.addAttribute("formAction", "/inventory/save");
        return "inventory-add";
    }

@PostMapping("/save")
public String save(@ModelAttribute("item") Inventory item,
                   BindingResult result,
                   Model model) {

    validate(item, result);

    if (result.hasErrors()) {
        model.addAttribute("pageTitle", "Add Spare Part");
        model.addAttribute("formAction", "/inventory/save");
        return "inventory-add";
    }

    try {
        inventoryRepository.save(item);
        return "redirect:/inventory";
    } catch (Exception e) {
        model.addAttribute("pageTitle", "Add Spare Part");
        model.addAttribute("formAction", "/inventory/save");
        model.addAttribute("errorMessage",
                "Could not save the spare part. Check the application console for the database error.");
        return "inventory-add";
    }
}

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Inventory item = inventoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Part not found: " + id));

        model.addAttribute("item", item);
        model.addAttribute("pageTitle", "Edit Spare Part");
        model.addAttribute("formAction", "/inventory/update/" + id);
        return "inventory-add";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("item") Inventory item,
                         BindingResult result,
                         Model model) {

        if (!inventoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Part not found: " + id);
        }

        validate(item, result);

        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Spare Part");
            model.addAttribute("formAction", "/inventory/update/" + id);
            return "inventory-add";
        }

        item.setId(id);
        inventoryRepository.save(item);
        return "redirect:/inventory";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        if (inventoryRepository.existsById(id)) {
            inventoryRepository.deleteById(id);
        }
        return "redirect:/inventory";
    }

    private void validate(Inventory item, BindingResult result) {
        if (item.getPartName() == null || item.getPartName().isBlank()) {
            result.rejectValue("partName", "required", "Part name is required.");
        }

        if (item.getQuantity() == null || item.getQuantity() < 0) {
            result.rejectValue("quantity", "invalid", "Quantity cannot be negative.");
        }

        if (item.getUnitPrice() == null
                || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            result.rejectValue("unitPrice", "invalid", "Price cannot be negative.");
        }

        if (item.getReorderLevel() == null || item.getReorderLevel() < 0) {
            result.rejectValue("reorderLevel", "invalid",
                    "Reorder level cannot be negative.");
        }
    }
}