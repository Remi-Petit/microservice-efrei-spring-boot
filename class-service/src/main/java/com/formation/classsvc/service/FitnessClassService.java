package com.formation.classsvc.service;

import com.formation.classsvc.dto.FitnessClassRequest;
import com.formation.classsvc.dto.FitnessClassResponse;
import com.formation.classsvc.exception.ClassNotModifiableException;
import com.formation.classsvc.exception.FitnessClassNotFoundException;
import com.formation.classsvc.exception.InvalidSpotsOperationException;
import com.formation.classsvc.exception.NoSpotsAvailableException;
import com.formation.classsvc.mapper.FitnessClassMapper;
import com.formation.classsvc.model.Category;
import com.formation.classsvc.model.ClassStatus;
import com.formation.classsvc.model.FitnessClass;
import com.formation.classsvc.model.Level;
import com.formation.classsvc.repository.FitnessClassRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FitnessClassService {

    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 45, 60, 90);

    private final FitnessClassRepository repository;

    public FitnessClassService(FitnessClassRepository repository) {
        this.repository = repository;
    }

    /**
     * Liste paginee des cours, filtrable par categorie, niveau, localisation,
     * instructeur et plage de dates.
     */
    public Page<FitnessClassResponse> findAll(Category category, Level level, String location,
                                              String instructor, LocalDate dateFrom, LocalDate dateTo,
                                              Pageable pageable) {
        return repository.findAll(specification(category, level, location, instructor, dateFrom, dateTo), pageable)
                .map(FitnessClassMapper::toResponse);
    }

    public FitnessClassResponse findById(Long id) {
        return FitnessClassMapper.toResponse(findClass(id));
    }

    @Transactional
    public FitnessClassResponse create(FitnessClassRequest request) {
        validateDuration(request.durationMinutes());
        FitnessClass saved = repository.save(FitnessClassMapper.toEntity(request));
        return FitnessClassMapper.toResponse(saved);
    }

    @Transactional
    public FitnessClassResponse update(Long id, FitnessClassRequest request) {
        FitnessClass c = findClass(id);
        assertModifiable(c);

        validateDuration(request.durationMinutes());
        // On ne peut pas reduire la capacite totale en dessous des places deja reserves.
        if (request.maxParticipants() < c.getCurrentParticipants()) {
            throw new InvalidSpotsOperationException(
                    "Capacite maximum trop basse : " + c.getCurrentParticipants() + " place(s) deja reservee(s)");
        }

        c.setName(request.name());
        c.setDescription(request.description());
        c.setInstructor(request.instructor());
        c.setGymLocation(request.gymLocation());
        c.setCategory(request.category());
        c.setLevel(request.level());
        c.setDurationMinutes(request.durationMinutes());
        c.setMaxParticipants(request.maxParticipants());
        c.setPrice(request.price());
        c.setDateTime(request.dateTime());
        return FitnessClassMapper.toResponse(repository.save(c));
    }

    /**
     * Annule un cours (statut {@code CANCELLED}). Un cours deja annule ou
     * deja termine ne peut plus etre annule a nouveau.
     */
    @Transactional
    public void cancel(Long id) {
        FitnessClass c = findClass(id);
        if (c.getStatus() == ClassStatus.COMPLETED) {
            throw new ClassNotModifiableException("Impossible d'annuler un cours deja termine");
        }
        if (c.getStatus() == ClassStatus.CANCELLED) {
            throw new ClassNotModifiableException("Le cours est deja annule");
        }
        c.setStatus(ClassStatus.CANCELLED);
        repository.save(c);
    }

    /**
     * Reserve {@code spots} places. La coordonnee est verifiee ici (volet "check")
     * mais le verrouillage optimiste garantit qu'une reservation concurrente ne
     * peut pas depasser la capacite (volet "use" du probleme TOCTOU).
     *
     * <p>Appele par {@code booking-service} via Feign.</p>
     */
    @Transactional
    public FitnessClassResponse incrementParticipants(Long id, int spots) {
        FitnessClass c = findClass(id);
        assertOpenForBooking(c);
        try {
            c.incrementParticipants(spots);
            // saveAndFlush force le flush immediatement (et donc la verification optimiste)
            // pour que l'OptimisticLockException soit traduite ici en conflit metier.
            return FitnessClassMapper.toResponse(repository.saveAndFlush(c));
        } catch (OptimisticLockException ex) {
            throw new NoSpotsAvailableException("Conflit de concurrence : la place a ete prise entre-temps");
        }
    }

    /**
     * Libere {@code spots} places (annulation de reservation, no-show...).
     * Appele par {@code booking-service} via Feign.
     */
    @Transactional
    public FitnessClassResponse decrementParticipants(Long id, int spots) {
        FitnessClass c = findClass(id);
        try {
            c.decrementParticipants(spots);
            return FitnessClassMapper.toResponse(repository.saveAndFlush(c));
        } catch (OptimisticLockException ex) {
            throw new InvalidSpotsOperationException("Conflit de concurrence : operation sur les places impossible");
        }
    }

    private void assertModifiable(FitnessClass c) {
        if (c.getStatus() == ClassStatus.CANCELLED) {
            throw new ClassNotModifiableException("Impossible de modifier un cours annule");
        }
        if (c.getStatus() == ClassStatus.COMPLETED) {
            throw new ClassNotModifiableException("Impossible de modifier un cours termine");
        }
    }

    private void assertOpenForBooking(FitnessClass c) {
        if (c.getStatus() != ClassStatus.SCHEDULED) {
            throw new ClassNotModifiableException("Impossible de reserver un cours " + c.getStatus().name().toLowerCase());
        }
        if (c.getDateTime().isBefore(java.time.LocalDateTime.now())) {
            throw new ClassNotModifiableException("Impossible de reserver un cours dont la date est passee");
        }
    }

    private void validateDuration(Integer duration) {
        if (duration == null || !ALLOWED_DURATIONS.contains(duration)) {
            throw new InvalidSpotsOperationException("La duree doit etre l'une des valeurs : " + ALLOWED_DURATIONS);
        }
    }

    private FitnessClass findClass(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new FitnessClassNotFoundException(id));
    }

    private Specification<FitnessClass> specification(Category category, Level level, String location,
                                                      String instructor, LocalDate dateFrom, LocalDate dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("gymLocation")), "%" + location.toLowerCase() + "%"));
            }
            if (instructor != null && !instructor.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("instructor")), "%" + instructor.toLowerCase() + "%"));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateTime"), dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateTime"), dateTo.atTime(LocalTime.MAX)));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
