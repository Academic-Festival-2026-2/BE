package com.binteum.domain.classschedule.repository;

import com.binteum.domain.classschedule.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassScheduleRepository
    extends JpaRepository<ClassSchedule, Long> {

  List<ClassSchedule>
  findByClassroom_RoomIdAndDayOfWeekOrderByStartTimeAsc(
      Long roomId,
      String dayOfWeek
  );
}