package com.spawnerbeacon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;

/** Merkt sich alle geladenen Spawner und baut daraus die Strahlen. */
public final class SpawnerTracker {
	private static final Set<SpawnerBlockEntity> SPAWNERS = ConcurrentHashMap.newKeySet();

	private SpawnerTracker() {
	}

	public static void register() {
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof SpawnerBlockEntity spawner) {
				SPAWNERS.add(spawner);
			}
		});
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof SpawnerBlockEntity spawner) {
				SPAWNERS.remove(spawner);
			}
		});
	}

	public static List<BeamState> collect() {
		BeaconConfig cfg = BeaconConfig.get();
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || !cfg.enabled) {
			return List.of();
		}

		float half = (float) (cfg.thickness / 2.0);
		float alpha = (float) cfg.opacity;
		List<BeamState> out = new ArrayList<>();

		Iterator<SpawnerBlockEntity> it = SPAWNERS.iterator();
		while (it.hasNext()) {
			SpawnerBlockEntity be = it.next();
			if (be.isRemoved() || be.getLevel() != level) {
				it.remove();
				continue;
			}
			BlockPos pos = be.getBlockPos();
			if (cfg.maxY <= pos.getY()) {
				continue;
			}
			int rgb = cfg.colorFor(typeOf(be, level, pos));
			out.add(new BeamState(
					pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, cfg.maxY, half,
					((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, alpha));
		}
		return List.copyOf(out);
	}

	/** Liefert z. B. "zombie" oder "other", wenn die Art nicht bestimmt werden kann. */
	private static String typeOf(SpawnerBlockEntity be, ClientLevel level, BlockPos pos) {
		try {
			Entity e = be.getSpawner().getOrCreateDisplayEntity(level, pos);
			if (e != null) {
				return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
			}
		} catch (Exception ignored) {
			// faellt unten auf "other" zurueck
		}
		return "other";
	}
}
