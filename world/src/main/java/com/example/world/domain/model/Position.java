package com.example.world.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Position {
    Integer mapId;
    float x;
    float y;
    float z;
    float orientation;
}