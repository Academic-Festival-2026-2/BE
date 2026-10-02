package com.binteum.domain.classroom.dto;

public record ClassroomResponse(
    Long roomId,
    String roomNumber,
    Integer capacity,
    Boolean hasOutlet
) {

}