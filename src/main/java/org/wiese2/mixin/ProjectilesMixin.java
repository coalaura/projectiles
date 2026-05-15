package org.wiese2.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wiese2.Projectiles;
import org.wiese2.ProjectilesRenderTypes;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
public class ProjectilesMixin {
	private static final int[] BlockColor = new int[] { 90, 125, 235 };
	private static final int[] EntityColor = new int[] { 225, 70, 70 };

	private static final float TrajectoryThickness = 0.025f;
	private static final float HitboxThickness = 0.05f;

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void renderTrajectory(CallbackInfo ci) {
		if (Projectiles.trajectoryPoints.size() < 2) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();

		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.getPosition();

		Matrix4f matrix = new Matrix4f()
				.rotation(new org.joml.Quaternionf(camera.rotation()).invert())
				.translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);

		RenderType lineType = ProjectilesRenderTypes.TRAJECTORY;
		BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer lineBuilder = bufferSource.getBuffer(lineType);

		float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

		Vec3 eye = mc.player.getEyePosition(tickDelta);
		Vec3 startPos = Projectiles.trajectoryPoints.get(0);

		int handMultiplier = mc.options.mainHand().get() == HumanoidArm.RIGHT ? 1 : -1;

		float yaw = (float) Math.toRadians(-mc.player.getViewYRot(tickDelta));
		float pitch = (float) Math.toRadians(-mc.player.getViewXRot(tickDelta));

		Vec3 forward = mc.player.getViewVector(tickDelta);

		Vec3 up = new Vec3(-Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch), -Math.sin(pitch) * Math.cos(yaw))
				.normalize();
		Vec3 right = forward.cross(up).normalize();

		Vec3 offset = new Vec3(0.2, -0.06, 0.2);

		if (camera.isDetached()) {
			offset = Vec3.ZERO;
		}

		Vec3 handToEyeDelta = right.scale(handMultiplier * offset.x).add(up.scale(offset.y))
				.add(forward.scale(offset.z)).add(eye.subtract(startPos));

		int[] color = null;
		AABB box = null;

		if (Projectiles.hitResult instanceof BlockHitResult blockHit) {
			color = BlockColor;

			box = new AABB(blockHit.getBlockPos());
		} else if (Projectiles.hitResult instanceof EntityHitResult entityHit) {
			color = EntityColor;

			Entity entity = entityHit.getEntity();

			Vec3 lerpedPos = new Vec3(
					Mth.lerp(tickDelta, entity.xOld, entity.getX()),
					Mth.lerp(tickDelta, entity.yOld, entity.getY()),
					Mth.lerp(tickDelta, entity.zOld, entity.getZ()));

			float w = entity.getBbWidth() / 2.0f;
			float h = entity.getBbHeight();
			float r = entity.getPickRadius() + 0.02f;

			box = new AABB(-w - r, -r, -w - r, w + r, h + r, w + r).move(lerpedPos);
		}

		int size = Projectiles.trajectoryPoints.size();

		for (int i = 0; i < size - 1; i++) {
			Vec3 lerpedDelta1 = handToEyeDelta.scale((size - (double) i) / size);
			Vec3 lerpedDelta2 = handToEyeDelta.scale((size - (double) (i + 1)) / size);

			Vec3 p1 = Projectiles.trajectoryPoints.get(i).add(lerpedDelta1);
			Vec3 p2 = Projectiles.trajectoryPoints.get(i + 1).add(lerpedDelta2);

			float progress = i / (float) (size - 1);
			int alpha = (int) Mth.lerp(progress, 255, 192);

			addTrajectorySegment(lineBuilder, matrix, p1, p2, TrajectoryThickness, color[0], color[1], color[2], alpha,
					camera);
		}

		bufferSource.endBatch(lineType);

		if (box != null) {
			RenderType outlineType = ProjectilesRenderTypes.HITBOX_OUTLINE;
			VertexConsumer outlineBuilder = bufferSource.getBuffer(outlineType);

			addBoxOutline(outlineBuilder, matrix, box, color[0], color[1], color[2], 224, HitboxThickness, camera);

			bufferSource.endBatch(outlineType);
		}
	}

	private void addBoxOutline(VertexConsumer bufferBuilder, Matrix4f matrix, AABB box, int r, int g, int b, int a,
			float thickness, Camera camera) {
		float minX = (float) box.minX;
		float minY = (float) box.minY;
		float minZ = (float) box.minZ;

		float maxX = (float) box.maxX;
		float maxY = (float) box.maxY;
		float maxZ = (float) box.maxZ;

		// Bottom
		addLine(bufferBuilder, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a, thickness, camera);

		// Top
		addLine(bufferBuilder, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a, thickness, camera);

		// Verticals
		addLine(bufferBuilder, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, thickness, camera);
		addLine(bufferBuilder, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, thickness, camera);
	}

	private void addLine(VertexConsumer bufferBuilder, Matrix4f matrix, float x1, float y1, float z1, float x2,
			float y2,
			float z2, int r, int g, int b, int a, float thickness, Camera camera) {
		addTrajectorySegment(bufferBuilder, matrix, new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), thickness, r, g, b, a,
				camera);
	}

	private void addTrajectorySegment(
			VertexConsumer builder,
			Matrix4f matrix,
			Vec3 from,
			Vec3 to,
			float thickness,
			int r,
			int g,
			int b,
			int a,
			Camera camera) {

		Vec3 dir = to.subtract(from);

		float len = (float) dir.length();

		if (len < 0.0001f) {
			return;
		}

		dir = dir.scale(1.0 / len);

		Vec3 camPos = camera.getPosition();

		Vec3 toCam = camPos.subtract(from).normalize();
		Vec3 perp = dir.cross(toCam).normalize().scale(thickness * 0.5f);

		if (perp.lengthSqr() < 0.0001f) {
			perp = new Vec3(0, 1, 0);

			if (Math.abs(dir.y) > 0.99f) {
				perp = new Vec3(1, 0, 0);
			}

			perp = dir.cross(perp).normalize().scale(thickness * 0.5f);
		}

		Vec3 extend = dir.scale(thickness * 0.5f);

		Vec3 p0 = from.subtract(extend).subtract(perp);
		Vec3 p1 = from.subtract(extend).add(perp);

		Vec3 p2 = to.add(extend).add(perp);
		Vec3 p3 = to.add(extend).subtract(perp);

		builder.addVertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a);
		builder.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a);
		builder.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a);
		builder.addVertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a);
	}
}