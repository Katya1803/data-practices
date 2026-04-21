package com.dvdrental.service;

import com.dvdrental.config.CacheConfig;
import com.dvdrental.dto.CustomerDto;
import com.dvdrental.dto.CustomerStatsDto;
import com.dvdrental.dto.PageResponse;
import com.dvdrental.dto.RentalDto;
import com.dvdrental.repository.CustomerRepository;
import com.dvdrental.repository.RentalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;

    public PageResponse<CustomerDto> getCustomers(Pageable pageable) {
        return new PageResponse<>(customerRepository.findAll(pageable).map(CustomerDto::new));
    }

    @Cacheable(value = CacheConfig.CACHE_FILM, key = "'customer:' + #id")
    public CustomerDto getCustomerById(int id) {
        return customerRepository.findById(id)
                .map(CustomerDto::new)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    public PageResponse<RentalDto> getCustomerRentals(int customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId))
            throw new EntityNotFoundException("Customer not found: " + customerId);
        return new PageResponse<>(
                rentalRepository.findByCustomerId(customerId, pageable).map(RentalDto::new));
    }

    public CustomerStatsDto getCustomerStats(int customerId) {
        List<Object[]> rows = customerRepository.getCustomerStats(customerId);
        if (rows.isEmpty())
            throw new EntityNotFoundException("Customer not found: " + customerId);
        return new CustomerStatsDto(rows.get(0));
    }

    public List<CustomerDto> getOverdueCustomers() {
        return customerRepository.findOverdueCustomers()
                .stream().map(CustomerDto::new).toList();
    }
}
