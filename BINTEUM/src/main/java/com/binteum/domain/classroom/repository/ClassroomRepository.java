package com.binteum.domain.classroom.repository;

import com.binteum.domain.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

  List<Classroom> findByBuilding_BuildingId(Long buildingId);
}