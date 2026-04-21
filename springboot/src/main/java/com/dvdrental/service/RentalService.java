package com.dvdrental.service;

import com.dvdrental.config.CacheConfig;
import com.dvdrental.dto.PageResponse;
import com.dvdrental.dto.RentalDto;
import com.dvdrental.dto.RentalRequest;
import com.dvdrental.kafka.event.RentalCreatedEvent;
import com.dvdrental.kafka.event.RentalReturnedEvent;
import com.dvdrental.kafka.producer.EventProducer;
import com.dvdrental.model.Customer;
import com.dvdrental.model.Inventory;
import com.dvdrental.model.Rental;
import com.dvdrental.model.Staff;
import com.dvdrental.repository.CustomerRepository;
import com.dvdrental.repository.InventoryRepository;
import com.dvdrental.repository.RentalRepository;
import com.dvdrental.repository.StaffRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final EventProducer eventProducer;

    @Transactional(readOnly = true)
    public PageResponse<RentalDto> getActiveRentals(Pageable pageable) {
        return new PageResponse<>(rentalRepository.findByReturnDateIsNull(pageable).map(RentalDto::new));
    }

    @Transactional(readOnly = true)
    public List<RentalDto> getOverdueRentals() {
        // Load overdue rental IDs via native query, then fetch fully with EntityGraph
        List<Integer> overdueIds = rentalRepository.findOverdueRentalsRaw()
                .stream().map(Rental::getRentalId).toList();
        return overdueIds.stream()
                .map(id -> rentalRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Rental not found: " + id)))
                .map(RentalDto::new)
                .toList();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_FILM_AVAILABLE, allEntries = true)
    public RentalDto createRental(RentalRequest request) {
        // 1. Validate inventory exists and is available
        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found: " + request.getInventoryId()));

        if (rentalRepository.isInventoryRented(inventory.getInventoryId())) {
            throw new IllegalStateException("Inventory " + request.getInventoryId() + " is already rented out");
        }

        // 2. Load customer and staff
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + request.getStaffId()));

        // 3. Create and save rental
        Rental rental = new Rental();
        rental.setInventory(inventory);
        rental.setCustomer(customer);
        rental.setStaff(staff);
        rental.setRentalDate(LocalDateTime.now());
        rental.setLastUpdate(LocalDateTime.now());
        rental = rentalRepository.save(rental);

        // 4. Produce Kafka event
        eventProducer.sendRentalCreated(new RentalCreatedEvent(
                rental.getRentalId(),
                customer.getCustomerId(),
                inventory.getFilm().getFilmId(),
                inventory.getInventoryId(),
                rental.getRentalDate()
        ));

        log.info("Rental created: id={} customer={} film={}", rental.getRentalId(),
                customer.getCustomerId(), inventory.getFilm().getFilmId());

        return new RentalDto(rental);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_FILM_AVAILABLE, allEntries = true)
    public RentalDto returnRental(int rentalId) {
        // 1. Find and validate rental
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new EntityNotFoundException("Rental not found: " + rentalId));

        if (rental.getReturnDate() != null) {
            throw new IllegalStateException("Rental " + rentalId + " has already been returned");
        }

        // 2. Set return date
        LocalDateTime now = LocalDateTime.now();
        rental.setReturnDate(now);
        rental.setLastUpdate(now);
        rental = rentalRepository.save(rental);

        // 3. Calculate days rented
        long daysRented = ChronoUnit.DAYS.between(rental.getRentalDate(), now);

        // 4. Produce Kafka event
        eventProducer.sendRentalReturned(new RentalReturnedEvent(
                rental.getRentalId(),
                rental.getCustomer().getCustomerId(),
                rental.getInventory().getFilm().getFilmId(),
                now,
                daysRented
        ));

        log.info("Rental returned: id={} daysRented={}", rentalId, daysRented);

        return new RentalDto(rental);
    }
}
