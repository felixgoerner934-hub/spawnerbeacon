package com.spawnerbeacon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/** Einstellungen der Mod, gespeichert in config/spawnerbeacon.json */
public final class BeaconConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Pattern HEX = Pattern.compile("^#?([0-9a-fA-F]{6})$");

	/** Spawner-Arten, die im Menue einstellbar sind (Reihenfolge = Reihenfolge im Menue). */
	public static final List<String> TYPES = List.of(
			"zombie", "skeleton", "spider", "cave_spider", "blaze", "silverfish",
			"magma_cube", "husk", "stray", "slime", "creeper", "other");

	private static final Map<String, String> DEFAULT_COLORS = Map.ofEntries(
			Map.entry("zombie", "55FF55"),
			Map.entry("skeleton", "FFFFFF"),
			Map.entry("spider", "FF5555"),
			Map.entry("cave_spider", "00FFFF"),
			Map.entry("blaze", "FFAA00"),
			Map.entry("silverfish", "AAAAAA"),
			Map.entry("magma_cube", "FF5500"),
			Map.entry("husk", "D2B48C"),
			Map.entry("stray", "99CCFF"),
			Map.entry("slime", "A0FF00"),
			Map.entry("creeper", "00AA00"),
			Map.entry("other", "FF55FF"));

	public boolean enabled = true;
	public double thickness = 0.5;
	public double opacity = 0.55;
	public int maxY = 300;
	public String defaultColor = "FFFFFF";
	public Map<String, String> typeColors = new LinkedHashMap<>();

	private static BeaconConfig instance;

	public static BeaconConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("spawnerbeacon.json");
	}

	public static BeaconConfig load() {
		BeaconConfig cfg = null;
		try {
			Path f = file();
			if (Files.exists(f)) {
				cfg = GSON.fromJson(Files.readString(f), BeaconConfig.class);
			}
		} catch (Exception e) {
			System.err.println("[SpawnerBeacon] Konfiguration konnte nicht gelesen werden, nutze Standardwerte: " + e);
		}
		if (cfg == null) {
			cfg = new BeaconConfig();
		}
		cfg.sanitize();
		instance = cfg;
		return cfg;
	}

	public void save() {
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), GSON.toJson(this));
		} catch (IOException e) {
			System.err.println("[SpawnerBeacon] Konfiguration konnte nicht gespeichert werden: " + e);
		}
	}

	public void resetToDefaults() {
		BeaconConfig d = new BeaconConfig();
		d.sanitize();
		this.enabled = d.enabled;
		this.thickness = d.thickness;
		this.opacity = d.opacity;
		this.maxY = d.maxY;
		this.defaultColor = d.defaultColor;
		this.typeColors = d.typeColors;
	}

	private void sanitize() {
		if (typeColors == null) {
			typeColors = new LinkedHashMap<>();
		}
		for (String t : TYPES) {
			String v = normalizeHex(typeColors.get(t));
			typeColors.put(t, v != null ? v : DEFAULT_COLORS.get(t));
		}
		String d = normalizeHex(defaultColor);
		defaultColor = d != null ? d : "FFFFFF";
		thickness = Math.max(0.1, Math.min(5.0, thickness));
		opacity = Math.max(0.05, Math.min(1.0, opacity));
		maxY = Math.max(64, Math.min(512, maxY));
	}

	/** Gibt "RRGGBB" (gross) zurueck oder null, wenn der Text kein gueltiger Hex-Wert ist. */
	public static String normalizeHex(String s) {
		if (s == null) {
			return null;
		}
		var m = HEX.matcher(s.trim());
		return m.matches() ? m.group(1).toUpperCase() : null;
	}

	/** Farbe (0xRRGGBB) fuer eine Spawner-Art, z. B. "zombie". */
	public int colorFor(String type) {
		String hex = typeColors.get(type);
		if (hex == null) {
			hex = defaultColor;
		}
		try {
			return Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			return 0xFFFFFF;
		}
	}
}
