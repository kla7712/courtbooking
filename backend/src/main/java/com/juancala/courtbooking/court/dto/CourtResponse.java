package com.juancala.courtbooking.court.dto;

import com.juancala.courtbooking.court.Court;
import com.juancala.courtbooking.court.Surface;

public record CourtResponse(
        Long id,
        String name,
        Surface surface,
        boolean indoor,
        boolean hasLighting,
        boolean active
) {

    public static CourtResponse from(Court court) {
        return new CourtResponse(
                court.getId(),
                court.getName(),
                court.getSurface(),
                court.isIndoor(),
                court.isHasLighting(),
                court.isActive()
        );
    }
}
