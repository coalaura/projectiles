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
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Projectiles implements ClientModInitializer {
	public static final String ModId = "projectiles";

	public static ProjectilesConfig config = new ProjectilesConfig();

	public static final int[] BlockColor = new int[] { 90, 125, 235 };
	public static final int[] EntityColor = new int[] { 225, 70, 70 };

	public static final List<Vec3> trajectoryPoints = new ArrayList<>();
	public static HitResult hitResult = null;

	public static KeyMapping visibilityKey;

	public static boolean isVisible = false;
	public static boolean isToggled = false;
	public static int[] color = BlockColor;

	@Override
	public void onInitializeClient() {
		visibilityKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.projectiles.visibility", GLFW.GLFW_KEY_LEFT_ALT, KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> calculateTrajectory(client));
	}

	private static void calculateTrajectory(Minecraft client) {
		trajectoryPoints.clear();
		hitResult = null;

		if (client.player == null) {
			return;
		}

		switch (config.visibilityMode) {
			case ALWAYS -> {
				isVisible = true;

				while (visibilityKey.consumeClick()) {
				}
			}
			case TOGGLE -> {
				while (visibilityKey.consumeClick()) {
					isToggled = !isToggled;
				}

				isVisible = isToggled;
			}
			case HOLD -> {
				isVisible = visibilityKey.isDown();

				while (visibilityKey.consumeClick()) {
				}
			}
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

		if (config.immersiveColors) {
			color = getItemColor(stack);
		} else {
			color = new int[] { 90, 125, 235 };
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
			ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);

			if (projectiles == null || projectiles.isEmpty()) {
				return;
			}

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
		float pitch = player.getXRot();

		float x = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
		float y = -Mth.sin((pitch + pitchOffset) * Mth.DEG_TO_RAD);
		float z = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);

		Vec3 velocity = new Vec3(x, y, z).normalize().scale(speed);
		Vec3 pos = player.getEyePosition().subtract(0, 0.1, 0);

		// throwable inherits player velocity
		boolean isThrowable = isThrowableItem(item);

		if (isThrowable) {
			Vec3 playerVel = player.getDeltaMovement();

			if (player.onGround()) {
				playerVel = new Vec3(playerVel.x, 0.0, playerVel.z);
			}

			velocity = velocity.add(playerVel);
		}

		trajectoryPoints.add(pos);

		for (int i = 0; i < 200; i++) {
			if (isThrowable) {
				velocity = velocity.subtract(0, gravity, 0);
			}

			Vec3 nextPos = pos.add(velocity);

			HitResult hit = player.level().clip(
					new ClipContext(
							pos,
							nextPos,
							ClipContext.Block.COLLIDER,
							ClipContext.Fluid.NONE,
							player));

			AABB box = new AABB(pos, nextPos).inflate(1.0);

			List<Entity> entities = player.level().getEntitiesOfClass(Entity.class, box,
					entity -> isValidEntity(entity, player));

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
				Vec3 hitLoc = hit.getLocation();

				if (isThrowable) {
					Vec3 correction = pos.subtract(nextPos).normalize().scale(0.125);

					hitLoc = hitLoc.add(correction);
				}

				trajectoryPoints.add(hitLoc);

				hitResult = hit;

				break;
			}

			trajectoryPoints.add(nextPos);

			pos = nextPos;

			if (!isThrowable) {
				velocity = velocity.scale(drag).subtract(0, gravity, 0);
			} else {
				velocity = velocity.scale(drag);
			}
		}
	}

	public static boolean isProjectileItem(Item item) {
		return item instanceof BowItem ||
				item instanceof CrossbowItem ||
				item instanceof SnowballItem ||
				item instanceof EggItem ||
				item instanceof EnderpearlItem ||
				item instanceof ThrowablePotionItem ||
				item instanceof TridentItem;
	}

	private static boolean isThrowableItem(Item item) {
		return item instanceof SnowballItem
				|| item instanceof EggItem
				|| item instanceof EnderpearlItem
				|| item instanceof ThrowablePotionItem;
	}

	private static boolean isValidEntity(Entity entity, LocalPlayer player) {
		if (entity == player) {
			return false;
		}

		if (entity.isSpectator() || !entity.isAlive()) {
			return false;
		}

		return !(entity instanceof ItemEntity) && !(entity instanceof Projectile) && !(entity instanceof ExperienceOrb);
	}

	private static int[] getItemColor(ItemStack stack) {
		Item item = stack.getItem();

		if (item instanceof ThrowablePotionItem) {
			PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

			int color = contents.getColor();

			return new int[] { (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF };
		}

		if (item instanceof EggItem) {
			if (item == Items.BLUE_EGG) {
				return new int[] { 180, 208, 188 }; // minty blue-green
			}

			if (item == Items.BROWN_EGG) {
				return new int[] { 212, 116, 84 }; // warm terracotta
			}

			return new int[] { 204, 184, 140 }; // pale beige
		}

		if (item instanceof EnderpearlItem) {
			return new int[] { 52, 156, 140 }; // deep sea green
		}

		if (item instanceof SnowballItem) {
			return new int[] { 198, 226, 226 }; // icy cyan-white
		}

		if (item instanceof TridentItem) {
			return new int[] { 90, 160, 140 }; // weathered teal
		}

		if (item instanceof CrossbowItem) {
			return new int[] { 150, 95, 35 }; // dark wood/leather
		}

		if (item instanceof BowItem) {
			return new int[] { 165, 125, 45 }; // oak wood
		}

		return BlockColor; // fallback blue
	}
}