package com.formation.classsvc.repository;

import com.formation.classsvc.model.FitnessClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FitnessClassRepository extends JpaRepository<FitnessClass, Long>, JpaSpecificationExecutor<FitnessClass> {
}
