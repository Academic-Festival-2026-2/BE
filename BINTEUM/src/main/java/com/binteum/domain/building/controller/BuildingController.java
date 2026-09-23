package com.binteum.domain.building.controller;

import com.binteum.domain.building.dto.BuildingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

  @GetMapping
  public ResponseEntity<List<BuildingResponse>> getBuildings() {
    return ResponseEntity.ok(List.of(
        new BuildingResponse(1L, "본관", 37.1234, 127.1234),
        new BuildingResponse(2L, "공학관", 37.1235, 127.1235)
    ));
  }
}


