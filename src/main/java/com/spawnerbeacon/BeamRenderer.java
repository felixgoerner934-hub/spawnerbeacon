package com.spawnerbeacon;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/**
 * Zeichnet die Strahlen durch Waende hindurch. Aufbau nach dem offiziellen Fabric-Beispiel
 * "Rendering in der Welt" (eigene Render-Pipeline ohne Tiefentest).
 */
public final class BeamRenderer {
	private static final RenderPipeline BEAM_PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath(SpawnerBeaconClient.MOD_ID, "pipeline/beam_through_walls"))
					.withDepthStencilState(Optional.empty())
					.build());

	private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

	private static volatile List<BeamState> beams = List.of();
	private static MappableRingBuffer vertexBuffer;
	private static boolean closed;

	private BeamRenderer() {
	}

	public static void register() {
		LevelRenderEvents.END_EXTRACTION.register(BeamRenderer::extract);
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(BeamRenderer::render);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close());
	}

	/** Extraktionsphase: Daten aus der Welt sammeln. */
	private static void extract(LevelExtractionContext context) {
		beams = SpawnerTracker.collect();
	}

	/** Zeichenphase: nur noch mit den extrahierten, unveraenderlichen Daten arbeiten. */
	private static void render(LevelRenderContext context) {
		List<BeamState> list = beams;
		if (closed || list.isEmpty()) {
			return;
		}

		PoseStack poseStack = context.poseStack();
		Vec3 cam = context.levelState().cameraRenderState.pos;
		Matrix4fc matrix = poseStack.last().pose();

		BufferBuilder buffer = new BufferBuilder(ALLOCATOR,
				BEAM_PIPELINE.getVertexFormatMode(), BEAM_PIPELINE.getVertexFormat());

		for (BeamState s : list) {
			// Koordinaten relativ zur Kamera (genauer als grosse Weltkoordinaten)
			float minX = (float) (s.x() - s.halfWidth() - cam.x);
			float maxX = (float) (s.x() + s.halfWidth() - cam.x);
			float minZ = (float) (s.z() - s.halfWidth() - cam.z);
			float maxZ = (float) (s.z() + s.halfWidth() - cam.z);
			float minY = (float) (s.y() - cam.y);
			float maxY = (float) (s.topY() - cam.y);
			addBox(matrix, buffer, minX, minY, minZ, maxX, maxY, maxZ, s.r(), s.g(), s.b(), s.a());
		}

		drawThroughWalls(Minecraft.getInstance(), buffer);
	}

	private static void addBox(Matrix4fc m, BufferBuilder b, float minX, float minY, float minZ,
			float maxX, float maxY, float maxZ, float r, float g, float bl, float a) {
		// Front
		b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
		// Back
		b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
		// Left
		b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
		// Right
		b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
		// Top
		b.addVertex(m, minX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, maxY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, maxY, minZ).setColor(r, g, bl, a);
		// Bottom
		b.addVertex(m, minX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, minZ).setColor(r, g, bl, a);
		b.addVertex(m, maxX, minY, maxZ).setColor(r, g, bl, a);
		b.addVertex(m, minX, minY, maxZ).setColor(r, g, bl, a);
	}

	private static void drawThroughWalls(Minecraft client, BufferBuilder buffer) {
		MeshData builtBuffer = buffer.buildOrThrow();
		MeshData.DrawState drawParameters = builtBuffer.drawState();
		VertexFormat format = drawParameters.format();

		GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
		draw(client, BEAM_PIPELINE, builtBuffer, drawParameters, vertices, format);

		// Buffer rotieren, damit wir keinen benutzen, den die GPU gerade liest
		vertexBuffer.rotate();
	}

	private static GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
		int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

		if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
			if (vertexBuffer != null) {
				vertexBuffer.close();
			}
			vertexBuffer = new MappableRingBuffer(
					() -> SpawnerBeaconClient.MOD_ID + " beam vertex buffer",
					GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
					vertexBufferSize);
		}

		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
				vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
		}

		return vertexBuffer.currentBuffer();
	}

	private static void draw(Minecraft client, RenderPipeline pipeline, MeshData builtBuffer,
			MeshData.DrawState drawParameters, GpuBuffer vertices, VertexFormat format) {
		GpuBuffer indices;
		VertexFormat.IndexType indexType;

		if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
			builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
			indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
			indexType = builtBuffer.drawState().indexType();
		} else {
			RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
			indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
			indexType = shapeIndexBuffer.type();
		}

		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

		try (RenderPass renderPass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(
						() -> SpawnerBeaconClient.MOD_ID + " beam render pass",
						client.getMainRenderTarget().getColorTextureView(),
						OptionalInt.empty(),
						client.getMainRenderTarget().getDepthTextureView(),
						OptionalDouble.empty())) {
			renderPass.setPipeline(pipeline);

			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicTransforms);

			renderPass.setVertexBuffer(0, vertices);
			renderPass.setIndexBuffer(indices, indexType);

			renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
		}

		builtBuffer.close();
	}

	private static void close() {
		closed = true;
		ALLOCATOR.close();
		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
	}
}
