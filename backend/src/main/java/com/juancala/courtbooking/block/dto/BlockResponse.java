package com.juancala.courtbooking.block.dto;

import com.juancala.courtbooking.block.CourtBlock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record BlockResponse(
        Long id,
        Long courtId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String reason
) {

    public static BlockResponse from(CourtBlock block, ZoneId zone) {
        ZonedDateTime start = block.getStartTime().atZone(zone);
        ZonedDateTime end = block.getEndTime().atZone(zone);
        return new BlockResponse(
                block.getId(),
                block.getCourt().getId(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                block.getReason());
    }
}
