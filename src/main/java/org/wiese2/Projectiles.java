package org.wiese2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Projectiles implements ClientModInitializer {
	public static final String ModId = "projectiles";

	public static final List<Vec3> trajectoryPoints = new ArrayList<>();
	public static HitResult hitResult = null;

	public static KeyMapping visibilityKey;
	public static boolean isVisible = false;

	@Override
	public void onInitializeClient() {
		visibilityKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.projectiles.visibility",
				GLFW.GLFW_KEY_LEFT_ALT,
				KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> calculateTrajectory(client));
	}

	private static void calculateTrajectory(Minecraft client) {
		trajectoryPoints.clear();
		hitResult = null;

		if (client.player == null) {
			return;
		}

		isVisible = visibilityKey.isDown();

		while (visibilityKey.consumeClick()) {
		}

		if (!isVisible) {
			return;
		}

		LocalPlayer player = client.player;

		ItemStack stack = player.getMainHandItem();
		Item item = stack.getItem();

		if (!isProjectileItem(item)) {
			stack = player.getOffhandItem();
			item = stack.getItem();

			if (!isProjectileItem(item)) {
				return;
			}
		}

		float speed = 1.5f;
		float gravity = 0.03f;
		float drag = 0.99f;
		float pitchOffset = 0f;

		if (item instanceof BowItem) {
			if (!player.isUsingItem() || !player.getUseItem().is(item)) {
				return;
			}

			int useTicks = 72000 - player.getUseItemRemainingTicks();
			float pull = BowItem.getPowerForTime(useTicks);

			speed = pull * 3.0f;
			gravity = 0.05f;

			if (speed < 0.1f) {
				return;
			}
		} else if (item instanceof CrossbowItem) {
			speed = 3.15f;
			gravity = 0.05f;
		} else if (item instanceof TridentItem) {
			if (!player.isUsingItem() || !player.getUseItem().is(item)) {
				return;
			}

			speed = 2.5f;
			gravity = 0.05f;
		} else if (item instanceof ThrowablePotionItem) {
			speed = 0.5f;
			gravity = 0.05f;
			pitchOffset = -20.0f;
		}

		float yaw = player.getYRot();
		float pitch = player.getXRot() + pitchOffset;

		float x = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
		float y = -Mth.sin(pitch * Mth.DEG_TO_RAD);
		float z = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);

		Vec3 velocity = new Vec3(x, y, z).normalize().scale(speed);
		Vec3 pos = player.getEyePosition().subtract(0, 0.1, 0);

		trajectoryPoints.add(pos);

		for (int i = 0; i < 200; i++) {
			Vec3 nextPos = pos.add(velocity);

			HitResult hit = player.level().clip(
					new ClipContext(
							pos,
							nextPos,
							ClipContext.Block.OUTLINE,
							ClipContext.Fluid.NONE,
							player));

			AABB box = new AABB(pos, nextPos).inflate(1.0);
			List<Entity> entities = player.level().getEntitiesOfClass(
					Entity.class, box,
					entity -> !entity.isSpectator() && entity.isAlive() && entity != player);

			Entity hitEntity = null;
			Vec3 entityHitPos = null;
			double closestDist = Double.MAX_VALUE;

			for (Entity entity : entities) {
				AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());

				Optional<Vec3> entityHit = entityBox.clip(pos, nextPos);

				if (entityHit.isPresent()) {
					double dist = pos.distanceToSqr(entityHit.get());
					if (dist < closestDist) {
						closestDist = dist;
						hitEntity = entity;
						entityHitPos = entityHit.get();
					}
				}
			}

			if (hitEntity != null && (hit == null || hit.getType() == HitResult.Type.MISS
					|| pos.distanceToSqr(hit.getLocation()) > closestDist)) {
				trajectoryPoints.add(entityHitPos);

				hitResult = new EntityHitResult(hitEntity, entityHitPos);

				break;
			} else if (hit != null && hit.getType() != HitResult.Type.MISS) {
				trajectoryPoints.add(hit.getLocation());

				hitResult = hit;

				break;
			}

			trajectoryPoints.add(nextPos);

			pos = nextPos;
			velocity = velocity.scale(drag).subtract(0, gravity, 0);
		}
	}

	private static boolean isProjectileItem(Item item) {
		return item instanceof BowItem ||
				item instanceof CrossbowItem ||
				item instanceof SnowballItem ||
				item instanceof EggItem ||
				item instanceof EnderpearlItem ||
				item instanceof ThrowablePotionItem ||
				item instanceof TridentItem;
	}
}