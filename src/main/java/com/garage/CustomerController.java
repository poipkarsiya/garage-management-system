
package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.Customer;
import com.garage.garagemanagementsystem.entity.User;
import com.garage.garagemanagementsystem.repository.CustomerRepository;
import com.garage.garagemanagementsystem.repository.UserRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@Controller
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerController(
            CustomerRepository customerRepository,
            UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        return "customers";
    }

    @GetMapping("/customers/add")
    public String addCustomer(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer-add";
    }

    @PostMapping("/customers/add")
    @Transactional
    public String saveCustomer(
            @ModelAttribute Customer customer,
            @RequestParam(defaultValue = "false") boolean createLogin,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            Model model) {

        if (createLogin) {
            if (username == null || username.isBlank()
                    || password == null || password.isBlank()) {
                model.addAttribute("error",
                        "Username and password are required to create a login.");
                model.addAttribute("customer", customer);
                return "customer-add";
            }

            username = username.trim();

            if (userRepository.findByUsername(username).isPresent()) {
                model.addAttribute("error",
                        "That username already exists. Please choose another.");
                model.addAttribute("customer", customer);
                return "customer-add";
            }
        }

        Customer savedCustomer = customerRepository.save(customer);

        if (createLogin) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(savedCustomer.getEmail());
            user.setFullName(savedCustomer.getFullName());
            user.setRole("USER");
            user.setEnabled(true);

            userRepository.save(user);

            savedCustomer.setUser(user);
            customerRepository.save(savedCustomer);
        }

        return "redirect:/customers";
    }

    @PostMapping("/customers/save")
    @Transactional
    public String saveCustomerAlternative(
            @ModelAttribute Customer customer) {
        customerRepository.save(customer);
        return "redirect:/customers";
    }

    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        customerRepository.deleteById(id);
        return "redirect:/customers";
    }

    @GetMapping("/customers/edit/{id}")
    public String editCustomer(@PathVariable Long id, Model model) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow();
        model.addAttribute("customer", customer);
        return "customer-edit";
    }

    @PostMapping("/customers/update")
    public String updateCustomer(@ModelAttribute Customer customer) {
        customerRepository.save(customer);
        return "redirect:/customers";
    }
}
