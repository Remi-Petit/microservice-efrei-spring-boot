package com.formation.classsvc;

import com.formation.classsvc.dto.FitnessClassRequest;
import com.formation.classsvc.dto.FitnessClassResponse;
import com.formation.classsvc.exception.ClassNotModifiableException;
import com.formation.classsvc.exception.FitnessClassNotFoundException;
import com.formation.classsvc.exception.InvalidSpotsOperationException;
import com.formation.classsvc.exception.NoSpotsAvailableException;
import com.formation.classsvc.model.Category;
import com.formation.classsvc.model.ClassStatus;
import com.formation.classsvc.model.FitnessClass;
import com.formation.classsvc.model.Level;
import com.formation.classsvc.repository.FitnessClassRepository;
import com.formation.classsvc.service.FitnessClassService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FitnessClassServiceTest {

    @Mock
    private FitnessClassRepository repository;

    @InjectMocks
    private FitnessClassService service;

    private FitnessClass course(int max, int current) {
        FitnessClass c = new FitnessClass("Yoga du matin", "Cours doux", "Marie", "Paris",
                Category.YOGA, Level.BEGINNER, 60, max, current,
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(7), ClassStatus.SCHEDULED);
        c.setId(1L);
        c.setVersion(0L);
        return c;
    }

    @Test
    void create_initialiseAucunParticipantEtScheduled() {
        FitnessClassRequest request = request();
        when(repository.save(any(FitnessClass.class))).thenAnswer(invocation -> {
            FitnessClass c = invocation.getArgument(0);
            c.setId(1L);
            c.setVersion(0L);
            return c;
        });

        FitnessClassResponse result = service.create(request);

        assertThat(result.currentParticipants()).isEqualTo(0);
        assertThat(result.status()).isEqualTo(ClassStatus.SCHEDULED);
    }

    @Test
    void findById_coursInexistant_leveFitnessClassNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(FitnessClassNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void incrementParticipants_placesDisponibles_incremente() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(10, 5)));
        when(repository.saveAndFlush(any(FitnessClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FitnessClassResponse result = service.incrementParticipants(1L, 2);

        assertThat(result.currentParticipants()).isEqualTo(7);
    }

    @Test
    void incrementParticipants_plusDePlaces_leveNoSpotsAvailableException() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(10, 9)));

        assertThatThrownBy(() -> service.incrementParticipants(1L, 2))
                .isInstanceOf(NoSpotsAvailableException.class);
    }

    @Test
    void incrementParticipants_coursAnnule_leveClassNotModifiable() {
        FitnessClass c = course(10, 0);
        c.setStatus(ClassStatus.CANCELLED);
        when(repository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.incrementParticipants(1L, 1))
                .isInstanceOf(ClassNotModifiableException.class);
    }

    @Test
    void decrementParticipants_libereDesPlaces() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(10, 5)));
        when(repository.saveAndFlush(any(FitnessClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FitnessClassResponse result = service.decrementParticipants(1L, 2);

        assertThat(result.currentParticipants()).isEqualTo(3);
    }

    @Test
    void decrementParticipants_tropDePlaces_leveInvalidSpotsOperation() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(10, 1)));

        assertThatThrownBy(() -> service.decrementParticipants(1L, 2))
                .isInstanceOf(InvalidSpotsOperationException.class);
    }

    @Test
    void update_reductionCapaciteSousLesPlacesReservees_leveConflit() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(30, 15)));

        FitnessClassRequest reduce = new FitnessClassRequest("Yoga du matin", "Cours doux", "Marie", "Paris",
                Category.YOGA, Level.BEGINNER, 60, 10,
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(7));

        assertThatThrownBy(() -> service.update(1L, reduce))
                .isInstanceOf(InvalidSpotsOperationException.class);
    }

    @Test
    void update_dureeNonAutorisee_leveConflit() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(30, 0)));

        FitnessClassRequest invalid = new FitnessClassRequest("Yoga du matin", "Cours doux", "Marie", "Paris",
                Category.YOGA, Level.BEGINNER, 75, 15,
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(7));

        assertThatThrownBy(() -> service.update(1L, invalid))
                .isInstanceOf(InvalidSpotsOperationException.class);
    }

    @Test
    void cancel_passeLeCoursEnAnnule() {
        FitnessClass course = course(10, 0);
        when(repository.findById(1L)).thenReturn(Optional.of(course));

        service.cancel(1L);

        assertThat(course.getStatus()).isEqualTo(ClassStatus.CANCELLED);
    }

    private FitnessClassRequest request() {
        return new FitnessClassRequest("Yoga du matin", "Cours doux", "Marie", "Paris",
                Category.YOGA, Level.BEGINNER, 60, 20,
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(7));
    }
}
