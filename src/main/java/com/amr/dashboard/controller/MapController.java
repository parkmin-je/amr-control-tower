package com.amr.dashboard.controller;

import com.amr.dashboard.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping(value = "/api/robot/{robotId}/map", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getMapImage(@PathVariable String robotId) throws IOException {
        byte[] png = mapService.getMapPng(robotId);
        if (png == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(png);
    }

    @GetMapping("/api/robot/{robotId}/map/info")
    public ResponseEntity<Map<String, Object>> getMapInfo(@PathVariable String robotId) {
        MapService.MapData data = mapService.getMapData(robotId);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "width", data.width(),
                "height", data.height(),
                "resolution", data.resolution(),
                "originX", data.originX(),
                "originY", data.originY()
        ));
    }
}
