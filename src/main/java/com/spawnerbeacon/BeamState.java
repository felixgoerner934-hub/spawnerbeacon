package com.spawnerbeacon;

/** Unveraenderlicher Zustand eines Strahls (wird in der Extraktionsphase erzeugt). */
public record BeamState(double x, double y, double z, double topY, float halfWidth,
		float r, float g, float b, float a) {
}
