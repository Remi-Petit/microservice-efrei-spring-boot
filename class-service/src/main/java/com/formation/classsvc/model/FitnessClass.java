package com.formation.classsvc.model;

import com.formation.classsvc.exception.InvalidSpotsOperationException;
import com.formation.classsvc.exception.NoSpotsAvailableException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un cours de sport propose par une salle partenaire.
 *
 * <p>Le champ {@code @Version} active le <b>verrouillage optimiste</b> de JPA :
 * deux transactions concurrentes qui modifient le meme cours ne peuvent pas
 * ecraser leurs modifications. La seconde a echouer recoit une
 * {@code OptimisticLockException} -> c'est le mecanisme qui empeche les
 * surreservations lorsque plusieurs utilisateurs reservent en meme temps.</p>
 */
@Entity
@Table(name = "fitness_classes")
public class FitnessClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String instructor;

    @Column(nullable = false)
    private String gymLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer currentParticipants;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassStatus status;

    /**
     * Version de l'entite utilisee par le verrouillage optimiste.
     * Incrementee automatiquement par Hibernate a chaque flush.
     */
    @Version
    private Long version;

    public FitnessClass() {
    }

    public FitnessClass(String name, String description, String instructor, String gymLocation,
                        Category category, Level level, Integer durationMinutes,
                        Integer maxParticipants, Integer currentParticipants,
                        BigDecimal price, LocalDateTime dateTime, ClassStatus status) {
        this.name = name;
        this.description = description;
        this.instructor = instructor;
        this.gymLocation = gymLocation;
        this.category = category;
        this.level = level;
        this.durationMinutes = durationMinutes;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = currentParticipants;
        this.price = price;
        this.dateTime = dateTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getGymLocation() {
        return gymLocation;
    }

    public void setGymLocation(String gymLocation) {
        this.gymLocation = gymLocation;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public ClassStatus getStatus() {
        return status;
    }

    public void setStatus(ClassStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Verifie qu'il reste suffisamment de places puis incremente le compteur.
     * Appelee dans une transaction : le verrouillage optimiste (champ
     * {@code @Version}) garantit que deux reservations concurrentes ne peuvent
     * pas valider toutes les deux la meme place.
     *
     * @throws NoSpotsAvailableException si le nombre de places demandees depasse la capacite
     */
    public void incrementParticipants(int spots) {
        if (this.currentParticipants + spots > this.maxParticipants) {
            throw new NoSpotsAvailableException(
                    "Plus de places disponibles : " + this.currentParticipants + "/" + this.maxParticipants + " deja prises");
        }
        this.currentParticipants += spots;
    }

    /**
     * Libere {@code spots} places (annulation, no-show...).
     *
     * @throws InvalidSpotsOperationException si le nombre de places a liberer
     *                                        depasse le nombre de participants actuellement reserves
     */
    public void decrementParticipants(int spots) {
        if (this.currentParticipants < spots) {
            throw new InvalidSpotsOperationException(
                    "Impossible de liberer " + spots + " place(s) : seulement " + this.currentParticipants + " occupee(s)");
        }
        this.currentParticipants -= spots;
    }
}
