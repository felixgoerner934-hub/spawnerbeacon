package com.spawnerbeacon;

import java.util.Locale;
import java.util.function.DoubleConsumer;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Einfacher Slider fuer einen Zahlenbereich [min, max]. */
public class ConfigSlider extends AbstractSliderButton {
	private final String label;
	private final double min;
	private final double max;
	private final String format;
	private final DoubleConsumer setter;

	public ConfigSlider(int x, int y, int width, int height, String label, double min, double max,
			double current, String format, DoubleConsumer setter) {
		super(x, y, width, height, Component.empty(), (current - min) / (max - min));
		this.label = label;
		this.min = min;
		this.max = max;
		this.format = format;
		this.setter = setter;
		updateMessage();
	}

	private double actual() {
		return min + (max - min) * this.value;
	}

	@Override
	protected void updateMessage() {
		if (label == null) {
			return;
		}
		setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, format, actual())));
	}

	@Override
	protected void applyValue() {
		setter.accept(actual());
	}
}
