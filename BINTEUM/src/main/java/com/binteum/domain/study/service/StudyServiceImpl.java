package com.binteum.domain.study.service;

import com.binteum.domain.classroom.entity.Classroom;
import com.binteum.domain.classroom.exception.ClassroomException;
import com.binteum.domain.classroom.exception.code.ClassroomErrorCode;
import com.binteum.domain.classroom.repository.ClassroomRepository;
import com.binteum.domain.participation.entity.Participation;
import com.binteum.domain.participation.enums.ParticipationStatus;
import com.binteum.domain.participation.repository.ParticipationRepository;
import com.binteum.domain.participation.repository.StudyParticipantCount;
import com.binteum.domain.study.converter.StudyConverter;
import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyDetailResponse;
import com.binteum.domain.study.dto.StudyListResponse;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.dto.StudySummaryResponse;
import com.binteum.domain.study.entity.Study;
import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyDisplayStatus;
import com.binteum.domain.study.enums.StudyListTab;
import com.binteum.domain.study.enums.StudyStatus;
import com.binteum.domain.study.exception.StudyException;
import com.binteum.domain.study.exception.code.StudyErrorCode;
import com.binteum.domain.study.repository.StudyRepository;
import com.binteum.domain.user.entity.User;
import com.binteum.domain.user.exception.UserException;
import com.binteum.domain.user.exception.code.UserErrorCode;
import com.binteum.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  @Override
  @Transactional(readOnly = true)
  public StudyListResponse getStudies(StudyListTab tab, String keyword, Long buildingId,
      StudyCategory category, int page, int size) {
    LocalDateTime now = LocalDateTime.now(KST);
    LocalDate today = now.toLocalDate();

    LocalDate fromDate = switch (tab) {
      case TODAY -> today;
      case TOMORROW -> today.plusDays(1);
      case WEEK -> today;
    };
    LocalDate toDate = switch (tab) {
      case TODAY -> today.plusDays(1);
      case TOMORROW -> today.plusDays(2);
      case WEEK -> today.plusDays(7);
    };

    LocalDateTime from = fromDate.atStartOfDay();
    LocalDateTime to = toDate.atStartOfDay();

    String trimmedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.strip();

    Pageable pageable = PageRequest.of(page, size,
        Sort.by(Sort.Order.asc("startTime"), Sort.Order.asc("studyId")));

    Page<Study> studyPage = studyRepository.findList(StudyStatus.ACTIVE, now, from, to,
        trimmedKeyword, buildingId, category, pageable);

    List<Study> studies = studyPage.getContent();
    List<Long> studyIds = studies.stream().map(Study::getStudyId).toList();
    Map<Long, Long> countMap = countJoinedParticipants(studyIds);
    List<StudySummaryResponse> cards = studies.stream().map(study -> {
      long currentParticipants = countMap.getOrDefault(study.getStudyId(), 0L);
      StudyDisplayStatus displayStatus = study.calculateDisplayStatus(currentParticipants, now);
      return StudyConverter.toStudySummaryResponse(study, currentParticipants, displayStatus);
    }).toList();

    return StudyConverter.toStudyListResponse(studyPage, cards);
  }

  private Map<Long, Long> countJoinedParticipants(List<Long> studyIds) {
    if (studyIds.isEmpty()) {
      return Map.of();
    }
    return participationRepository.countByStudyIds(studyIds, ParticipationStatus.JOINED).stream()
        .collect(
            Collectors.toMap(StudyParticipantCount::getStudyId, StudyParticipantCount::getCount));
  }
}
