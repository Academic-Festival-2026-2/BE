package com.binteum.domain.classroom.repository;

import com.binteum.domain.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends
    JpaRepository<Classroom, Long>{
}
