package com.binteum.domain.study.service;

import com.binteum.domain.classroom.entity.Classroom;
import com.binteum.domain.classroom.repository.ClassroomRepository;
import com.binteum.domain.participation.entity.Participation;
import com.binteum.domain.participation.repository.ParticipationRepository;
import com.binteum.domain.study.converter.StudyConverter;
import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.entity.Study;
import com.binteum.domain.study.repository.StudyRepository;
import com.binteum.domain.user.entity.User;
import com.binteum.domain.user.repository.UserRepository;
import com.binteum.global.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final ParticipationRepository participationRepository;

    @Override
    @Transactional
    public StudyResponse createStudy(Long userId, StudyCreateRequest request) {
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (startTime == null || endTime == null
                || !startTime.isAfter(LocalDateTime.now())
                || !startTime.isBefore(endTime)
                || !startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new generalException(ErrorCode.INVALID_TIME_RANGE);
        }

        User host = userRepository.findById(userId)
                .orElseThrow(()->new generalException(ErrorCode.USER_NOT_FOUND));
        Classroom classroom = classroomRepository.findById(request.getRoomId())
                .orElseThrow(()->new GeneralException(ERRORCODE_CLASSROOM_NOT_FOUND));
        Study study = studyRepository.save(StudyConverter.toStudy(request,classroom,host));
        participationRepository.save(Participation.join(host,study));
        return StudyConverter.toStudyResponse(study,1L);
    }


}
