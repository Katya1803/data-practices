package com.dvdrental.service;

import com.dvdrental.dto.PageResponse;
import com.dvdrental.dto.PaymentDto;
import com.dvdrental.dto.PaymentRequest;
import com.dvdrental.kafka.event.PaymentProcessedEvent;
import com.dvdrental.kafka.producer.EventProducer;
import com.dvdrental.model.Customer;
import com.dvdrental.model.Payment;
import com.dvdrental.model.Rental;
import com.dvdrental.model.Staff;
import com.dvdrental.repository.CustomerRepository;
import com.dvdrental.repository.PaymentRepository;
import com.dvdrental.repository.RentalRepository;
import com.dvdrental.repository.StaffRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final EventProducer eventProducer;

    @Transactional(readOnly = true)
    public PageResponse<PaymentDto> getCustomerPayments(int customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId))
            throw new EntityNotFoundException("Customer not found: " + customerId);
        return new PageResponse<>(
                paymentRepository.findByCustomerCustomerId(customerId, pageable).map(PaymentDto::new));
    }

    @Transactional
    public PaymentDto createPayment(PaymentRequest request) {
        Rental rental = rentalRepository.findById(request.getRentalId())
                .orElseThrow(() -> new EntityNotFoundException("Rental not found: " + request.getRentalId()));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + request.getStaffId()));

        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setCustomer(customer);
        payment.setStaff(staff);
        payment.setAmount(request.getAmount());
        payment.setPaymentDate(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        eventProducer.sendPaymentProcessed(new PaymentProcessedEvent(
                payment.getPaymentId(),
                rental.getRentalId(),
                customer.getCustomerId(),
                payment.getAmount(),
                payment.getPaymentDate()
        ));

        log.info("Payment created: id={} rentalId={} amount={}", payment.getPaymentId(),
                rental.getRentalId(), payment.getAmount());

        return new PaymentDto(payment);
    }
}
