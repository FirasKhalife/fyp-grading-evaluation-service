package com.fypgrading.reviewservice.controller;

import com.fypgrading.reviewservice.service.GradingService;
import com.fypgrading.reviewservice.service.dto.GradingDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradingController {

    private final GradingService gradingService;

    public GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @GetMapping("/")
    public ResponseEntity<List<GradingDTO>> getGradings() {
        List<GradingDTO> gradings = gradingService.getGradings();
        return ResponseEntity.ok().body(gradings);
    }

    @PostMapping("/draft")
    public ResponseEntity<GradingDTO> draftGrading(@RequestBody GradingDTO gradingDTO) {
        GradingDTO createdGrading = gradingService.draftGrading(gradingDTO);
        return ResponseEntity.ok().body(createdGrading);
    }

    @PostMapping("/submit")
    public ResponseEntity<GradingDTO> submitGrading(@RequestBody GradingDTO gradingDTO) {
        GradingDTO createdGrading = gradingService.submitGrading(gradingDTO);
        return ResponseEntity.ok().body(createdGrading);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GradingDTO> updateGrading(@PathVariable Integer id, @RequestBody GradingDTO gradingDTO) {
        GradingDTO updateGrading = gradingService.updateGrading(id, gradingDTO);
        return ResponseEntity.ok().body(updateGrading);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GradingDTO> deleteGrading(@PathVariable Integer id) {
        GradingDTO deletedGrading = gradingService.deleteGrading(id);
        return ResponseEntity.ok().body(deletedGrading);
    }
}
