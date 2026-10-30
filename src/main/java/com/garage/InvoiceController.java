package com.garage.garagemanagementsystem.controller;

import com.garage.garagemanagementsystem.entity.Invoice;
import com.garage.garagemanagementsystem.entity.Customer;
import com.garage.garagemanagementsystem.entity.ServiceBooking;
import com.garage.garagemanagementsystem.repository.InvoiceRepository;
import com.garage.garagemanagementsystem.repository.CustomerRepository;
import com.garage.garagemanagementsystem.repository.ServiceBookingRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ServiceBookingRepository bookingRepository;

    public InvoiceController(
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            ServiceBookingRepository bookingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
    }
    
    @GetMapping("/invoices")
    public String invoices(Model model) {
        LocalDate today = LocalDate.now();
        for (Invoice invoice : invoiceRepository.findAll()) {
            if ("UNPAID".equalsIgnoreCase(invoice.getStatus())
            && invoice.getDueDate() != null
            && invoice.getDueDate().isBefore(today)) {
                
                invoice.setStatus("OVERDUE");
                invoiceRepository.save(invoice);
            }
        }

    model.addAttribute("invoices", invoiceRepository.findAll());
    return "invoices";
}

    @GetMapping("/invoices/add")
    public String addInvoice(Model model) {
        model.addAttribute("invoice", new Invoice());
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("bookings", bookingRepository.findAll());
        return "invoice-add";
    }

    @PostMapping("/invoices/save")
    public String saveInvoice(
            @RequestParam Long customerId,
            @RequestParam(required = false) Long bookingId,
            @RequestParam BigDecimal totalAmount,
            @RequestParam(defaultValue = "0") BigDecimal tax,
            @RequestParam(defaultValue = "0") BigDecimal discount,
            @RequestParam(required = false) LocalDate dueDate) {

        if (totalAmount.signum() < 0 ||
                tax.signum() < 0 ||
                discount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Amounts cannot be negative.");
        }

        BigDecimal finalAmount = totalAmount.add(tax).subtract(discount);

        if (finalAmount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Discount cannot exceed total amount plus tax.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found."));

        Invoice invoice = new Invoice();
        invoice.setCustomer(customer);

        if (bookingId != null) {
            ServiceBooking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Booking not found."));

            boolean alreadyExists = invoiceRepository.findAll().stream()
                    .anyMatch(existing -> existing.getBooking() != null
                            && bookingId.equals(
                                    existing.getBooking().getId()));

            if (alreadyExists) {
                throw new IllegalArgumentException(
                        "An invoice already exists for this booking.");
            }

            invoice.setBooking(booking);
        }

        invoice.setTotalAmount(totalAmount);
        invoice.setTax(tax);
        invoice.setDiscount(discount);
        invoice.setFinalAmount(finalAmount);
        invoice.setStatus("UNPAID");
        invoice.setDueDate(dueDate);

        invoiceRepository.save(invoice);

        return "redirect:/invoices";
    }

    @PostMapping("/invoices/pay/{id}")
    public String markPaid(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invoice not found."));

        invoice.setStatus("PAID");
        invoice.setPaidDate(LocalDate.now());
        invoiceRepository.save(invoice);

        return "redirect:/invoices";
    }

    @PostMapping("/invoices/unpay/{id}")
    public String markUnpaid(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invoice not found."));

        invoice.setStatus("UNPAID");
        invoice.setPaidDate(null);
        invoiceRepository.save(invoice);

        return "redirect:/invoices";
    }

    @PostMapping("/invoices/delete/{id}")
    public String deleteInvoice(@PathVariable Long id) {
        invoiceRepository.deleteById(id);
        return "redirect:/invoices";
    }
}
