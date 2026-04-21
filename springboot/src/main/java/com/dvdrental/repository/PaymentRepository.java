package com.dvdrental.repository;

import com.dvdrental.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Page<Payment> findByCustomerCustomerId(int customerId, Pageable pageable);
}
