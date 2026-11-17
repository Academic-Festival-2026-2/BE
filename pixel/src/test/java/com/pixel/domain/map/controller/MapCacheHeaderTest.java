package com.pixel.domain.map.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pixel.domain.map.service.MapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Spring Security 필터를 거친 실제 응답 헤더를 확인한다.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MapCacheHeaderTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MapService mapService;

  @Test
  void boundariesAndRegionsAreCachedForOneHour() throws Exception {
    mockMvc.perform(get("/api/map/boundaries"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "max-age=3600"));
    mockMvc.perform(get("/api/map/regions"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "max-age=3600"));
  }

  @Test
  void dongsAreNotCached() throws Exception {
    mockMvc.perform(get("/api/map/dongs").param("timeBand", "DAY"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control",
            "no-cache, no-store, max-age=0, must-revalidate"));
  }
}
