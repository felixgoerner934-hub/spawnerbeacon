package com.spawnerbeacon;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Einstellungsmenue (Taste Ue). */
public class ConfigScreen extends Screen {
	private static final int COL_W = 150;
	private static final int GAP = 10;
	private static final int ROW_H = 20;

	private final BeaconConfig cfg = BeaconConfig.get();

	public ConfigScreen() {
		super(Component.translatable("screen.spawnerbeacon.title"));
	}

	private static String tr(String key) {
		return Component.translatable(key).getString();
	}

	private Component toggleText() {
		return Component.translatable(cfg.enabled
				? "screen.spawnerbeacon.enabled_on"
				: "screen.spawnerbeacon.enabled_off");
	}

	private static String typeName(String type) {
		if (type.equals("other")) {
			return tr("screen.spawnerbeacon.type.other");
		}
		return tr("entity.minecraft." + type);
	}

	private EditBox hexBox(int x, int y, int w, String initial, Consumer<String> onValid) {
		EditBox box = new EditBox(this.font, x, y, w, 18, Component.empty());
		box.setMaxLength(7);
		box.setValue("#" + initial);
		box.setResponder(text -> {
			String hex = BeaconConfig.normalizeHex(text);
			if (hex != null) {
				onValid.accept(hex);
			}
		});
		this.addRenderableWidget(box);
		return box;
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int left = cx - COL_W - GAP / 2;
		int right = cx + GAP / 2;
		int y = 28;

		// Zeile 1: An/Aus + Dicke
		this.addRenderableWidget(Button.builder(toggleText(), b -> {
			cfg.enabled = !cfg.enabled;
			b.setMessage(toggleText());
		}).bounds(left, y, COL_W, ROW_H).build());
		this.addRenderableWidget(new ConfigSlider(right, y, COL_W, ROW_H,
				tr("screen.spawnerbeacon.thickness"), 0.1, 5.0, cfg.thickness, "%.1f",
				v -> cfg.thickness = v));
		y += 24;

		// Zeile 2: Deckkraft + Hoehe
		this.addRenderableWidget(new ConfigSlider(left, y, COL_W, ROW_H,
				tr("screen.spawnerbeacon.opacity"), 0.05, 1.0, cfg.opacity, "%.2f",
				v -> cfg.opacity = v));
		this.addRenderableWidget(new ConfigSlider(right, y, COL_W, ROW_H,
				tr("screen.spawnerbeacon.height"), 64, 512, cfg.maxY, "%.0f",
				v -> cfg.maxY = (int) Math.round(v)));
		y += 24;

		// Zeile 3: Standardfarbe (Label wird in extractRenderState gezeichnet) + Reset
		hexBox(left + 95, y, 55, cfg.defaultColor, hex -> cfg.defaultColor = hex);
		this.addRenderableWidget(Button.builder(Component.translatable("screen.spawnerbeacon.reset"), b -> {
			cfg.resetToDefaults();
			this.rebuildWidgets();
		}).bounds(right, y - 1, COL_W, ROW_H).build());

		// Spawner-Arten: zwei Spalten mit je 6 Zeilen
		int typesTop = 114;
		for (int i = 0; i < BeaconConfig.TYPES.size(); i++) {
			String type = BeaconConfig.TYPES.get(i);
			int col = i / 6;
			int row = i % 6;
			int x = (col == 0 ? left : right) + 95;
			int ry = typesTop + row * 20;
			hexBox(x, ry, 55, cfg.typeColors.get(type), hex -> cfg.typeColors.put(type, hex));
		}

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
				.bounds(cx - 100, this.height - 28, 200, ROW_H).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int cx = this.width / 2;
		int left = cx - COL_W - GAP / 2;
		int right = cx + GAP / 2;

		String title = this.title.getString();
		graphics.text(this.font, title, cx - this.font.width(title) / 2, 10, 0xFFFFFFFF, true);

		// Standardfarbe-Label
		graphics.text(this.font, tr("screen.spawnerbeacon.default_color"), left, 28 + 24 * 2 + 5,
				0xFF000000 | cfg.colorFor("__default__"), true);

		graphics.text(this.font, tr("screen.spawnerbeacon.types_title"), left, 100, 0xFFAAAAAA, true);

		for (int i = 0; i < BeaconConfig.TYPES.size(); i++) {
			String type = BeaconConfig.TYPES.get(i);
			int col = i / 6;
			int row = i % 6;
			int x = col == 0 ? left : right;
			int ry = 114 + row * 20;
			graphics.text(this.font, typeName(type), x, ry + 5, 0xFF000000 | cfg.colorFor(type), true);
		}
	}

	@Override
	public void onClose() {
		cfg.save();
		super.onClose();
	}
}
