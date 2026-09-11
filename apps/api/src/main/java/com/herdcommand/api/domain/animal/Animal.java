package com.herdcommand.api.domain.animal;

import com.herdcommand.api.domain.audit.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "animal")
public class Animal extends AuditedEntity {

    @Column(name = "farm_id", nullable = false, updatable = false)
    private UUID farmId;

    @Column(name = "identification_number", nullable = false, length = 40)
    private String identificationNumber;

    @Column(length = 100)
    private String name;

    @Column(name = "breed_id", nullable = false)
    private UUID breedId;

    @Column(name = "status_code", nullable = false, length = 40)
    private String statusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_code", nullable = false, length = 20)
    private GenderCode genderCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Animal() {}

    public Animal(
            UUID farmId,
            String identificationNumber,
            String name,
            UUID breedId,
            String statusCode,
            GenderCode genderCode,
            LocalDate dateOfBirth,
            UUID groupId) {
        this.farmId = farmId;
        this.identificationNumber = identificationNumber;
        this.name = name;
        this.breedId = breedId;
        this.statusCode = statusCode;
        this.genderCode = genderCode;
        this.dateOfBirth = dateOfBirth;
        this.groupId = groupId;
    }

    public UUID getFarmId() {
        return farmId;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getBreedId() {
        return breedId;
    }

    public void setBreedId(UUID breedId) {
        this.breedId = breedId;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public GenderCode getGenderCode() {
        return genderCode;
    }

    public void setGenderCode(GenderCode genderCode) {
        this.genderCode = genderCode;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public void archive(Instant when) {
        this.archivedAt = when;
    }
}
