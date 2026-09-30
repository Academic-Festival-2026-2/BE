package com.binteum.domain.participation.repository;

import com.binteum.domain.participation.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepository extends
        JpaRepository<Participation, Long> {
}
