package com.binteum.domain.study.service;

import com.binteum.domain.classroom.entity.Classroom;
import com.binteum.domain.classroom.exception.ClassroomException;
import com.binteum.domain.classroom.exception.code.ClassroomErrorCode;
import com.binteum.domain.classroom.repository.ClassroomRepository;
import com.binteum.domain.participation.entity.Participation;
import com.binteum.domain.participation.enums.ParticipationStatus;
import com.binteum.domain.participation.repository.ParticipationRepository;
import com.binteum.domain.study.converter.StudyConverter;
import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyDetailResponse;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.entity.Study;
import com.binteum.domain.study.enums.StudyDisplayStatus;
import com.binteum.domain.study.enums.StudyStatus;
import com.binteum.domain.study.exception.StudyException;
import com.binteum.domain.study.exception.code.StudyErrorCode;
import com.binteum.domain.study.repository.StudyRepository;
import com.binteum.domain.user.entity.User;
import com.binteum.domain.user.exception.UserException;
import com.binteum.domain.user.exception.code.UserErrorCode;
import com.binteum.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final StudyRepository studyRepository;
  private final UserRepository userRepository;
  private final ClassroomRepository classroomRepository;
  private final ParticipationRepository participationRepository;

  @Override
  @Transactional
  public StudyResponse createStudy(Long userId, StudyCreateRequest request) {
    LocalDateTime startTime = request.getStartTime();
    LocalDateTime endTime = request.getEndTime();
    if (!startTime.isAfter(LocalDateTime.now(KST))) {
      throw new StudyException(StudyErrorCode.STUDY_TIME_PAST);
    }
    if (!startTime.isBefore(endTime)) {
      throw new StudyException(StudyErrorCode.STUDY_TIME_ORDER);
    }
    if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
      throw new StudyException(StudyErrorCode.STUDY_TIME_DIFFERENT_DATE);
    }

    User host = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode._NOT_FOUND));
    Classroom classroom = classroomRepository.findById(request.getRoomId())
        .orElseThrow(() -> new ClassroomException(ClassroomErrorCode.CLASSROOM_NOT_FOUND));
    Study study = studyRepository.save(StudyConverter.toStudy(request, classroom, host));
    participationRepository.save(Participation.join(host, study));
    return StudyConverter.toStudyResponse(study, 1L);
  }

  @Override
  @Transactional(readOnly = true)
  public StudyDetailResponse getStudy(Long studyId, Long userId) {
    Study study = studyRepository.findDetailById(studyId)
        .orElseThrow(() -> new StudyException(StudyErrorCode.STUDY_NOT_FOUND));

    boolean isHost = study.getHost().getUserId().equals(userId);

    if (study.getStatus() == StudyStatus.CANCELLED && !isHost) {
      throw new StudyException(StudyErrorCode.STUDY_NOT_FOUND);
    }

    long currentParticipants = participationRepository.countByStudyAndStatus(study,
        ParticipationStatus.JOINED);
    boolean isJoined = participationRepository.existsByStudyAndUser_UserIdAndStatus(study, userId,
        ParticipationStatus.JOINED);

    LocalDateTime now = LocalDateTime.now(KST);
    StudyDisplayStatus displayStatus = study.calculateDisplayStatus(currentParticipants, now);

    return StudyConverter.toStudyDetailResponse(study, currentParticipants, displayStatus, isHost,
        isJoined);
  }

}
