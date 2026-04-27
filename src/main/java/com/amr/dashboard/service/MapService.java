package com.amr.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MapService {

    private final Map<String, MapData> maps = new ConcurrentHashMap<>();

    public void updateMap(String robotId, JsonNode msg) {
        try {
            JsonNode info = msg.path("info");
            int width = info.path("width").asInt();
            int height = info.path("height").asInt();
            double resolution = info.path("resolution").asDouble();
            double originX = info.path("origin").path("position").path("x").asDouble();
            double originY = info.path("origin").path("position").path("y").asDouble();

            JsonNode dataNode = msg.path("data");
            int[] data = new int[width * height];
            for (int i = 0; i < dataNode.size() && i < data.length; i++) {
                data[i] = dataNode.get(i).asInt();
            }

            maps.put(robotId, new MapData(width, height, resolution, originX, originY, data));
            log.debug("[MapService][{}] 맵 업데이트: {}x{}, res={}m/px", robotId, width, height, resolution);
        } catch (Exception e) {
            log.warn("[MapService][{}] 맵 파싱 오류: {}", robotId, e.getMessage());
        }
    }

    public byte[] getMapPng(String robotId) throws IOException {
        MapData map = maps.get(robotId);
        if (map == null) return null;

        BufferedImage img = new BufferedImage(map.width, map.height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < map.data.length; i++) {
            int val = map.data[i];
            int color;
            if (val == -1) color = 0x808080;     // unknown - gray
            else if (val == 0) color = 0xEEEEEE; // free - light gray
            else color = 0x1a1a2e;               // occupied - dark
            img.setRGB(i % map.width, map.height - 1 - i / map.width, color);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    public MapData getMapData(String robotId) {
        return maps.get(robotId);
    }

    public record MapData(int width, int height, double resolution, double originX, double originY, int[] data) {}
}
