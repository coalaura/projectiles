package org.wiese2;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class ProjectilesRenderTypes {
	private ProjectilesRenderTypes() {
	}

	public static final RenderType TRAJECTORY = RenderType.create(
			"projectiles_trajectory",
			1536,
			false,
			true,
			RenderPipelines.DEBUG_QUADS,
			RenderType.CompositeState.builder()
					.setTextureState(RenderStateShard.NO_TEXTURE)
					.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
					.setOutputState(RenderStateShard.MAIN_TARGET)
					.createCompositeState(false));

	public static final RenderType HITBOX_OUTLINE = RenderType.create(
			"projectiles_hitbox_outline",
			1536,
			false,
			false,
			RenderPipelines.DEBUG_QUADS,
			RenderType.CompositeState.builder()
					.setTextureState(RenderStateShard.NO_TEXTURE)
					.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
					.setOutputState(RenderStateShard.MAIN_TARGET)
					.createCompositeState(false));
}