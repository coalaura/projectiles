package org.wiese2.modmenu;

import org.wiese2.Projectiles;
import org.wiese2.ProjectilesConfig;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ProjectilesModMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ProjectilesConfigScreen::new;
	}

	public static class ProjectilesConfigScreen extends Screen {
		private final Screen parent;

		private final boolean originalImmersiveColors;
		private boolean immersiveColors;

		private final ProjectilesConfig.VisibilityMode originalVisibilityMode;
		private ProjectilesConfig.VisibilityMode visibilityMode;

		private final boolean originalShowHitboxes;
		private boolean showHitboxes;

		private final ProjectilesConfig.LineThickness originalLineThickness;
		private ProjectilesConfig.LineThickness lineThickness;

		public ProjectilesConfigScreen(Screen parent) {
			super(Component.literal("Sightline Config"));

			this.parent = parent;

			this.immersiveColors = Projectiles.config.immersiveColors;
			this.originalImmersiveColors = this.immersiveColors;

			this.visibilityMode = Projectiles.config.visibilityMode;
			this.originalVisibilityMode = this.visibilityMode;

			this.showHitboxes = Projectiles.config.showHitboxes;
			this.originalShowHitboxes = this.showHitboxes;

			this.lineThickness = Projectiles.config.lineThickness;
			this.originalLineThickness = this.lineThickness;
		}

		@Override
		protected void init() {
			int buttonW = 150;
			int buttonH = 20;
			int rowY = 60;
			int rowSpacing = 24;

			int contentW = buttonW * 2 + 10; // 310
			int startX = (this.width - contentW) / 2;
			int buttonX = startX + contentW - buttonW;

			addRenderableWidget(Button.builder(getToggleText(immersiveColors), btn -> {
				immersiveColors = !immersiveColors;

				Projectiles.config.immersiveColors = immersiveColors;

				btn.setMessage(getToggleText(immersiveColors));
			}).bounds(buttonX, rowY, buttonW, buttonH).build());

			addRenderableWidget(Button.builder(getModeText(visibilityMode), btn -> {
				visibilityMode = ProjectilesConfig.VisibilityMode.values()[(visibilityMode.ordinal() + 1)
						% ProjectilesConfig.VisibilityMode.values().length];

				Projectiles.config.visibilityMode = visibilityMode;

				btn.setMessage(getModeText(visibilityMode));
			}).bounds(buttonX, rowY + rowSpacing, buttonW, buttonH).build());

			addRenderableWidget(Button.builder(getToggleText(showHitboxes), btn -> {
				showHitboxes = !showHitboxes;

				Projectiles.config.showHitboxes = showHitboxes;

				btn.setMessage(getToggleText(showHitboxes));
			}).bounds(buttonX, rowY + rowSpacing * 2, buttonW, buttonH).build());

			addRenderableWidget(Button.builder(getThicknessText(lineThickness), btn -> {
				lineThickness = ProjectilesConfig.LineThickness.values()[(lineThickness.ordinal() + 1)
						% ProjectilesConfig.LineThickness.values().length];

				Projectiles.config.lineThickness = lineThickness;

				btn.setMessage(getThicknessText(lineThickness));
			}).bounds(buttonX, rowY + rowSpacing * 3, buttonW, buttonH).build());

			int bottomY = this.height - 30;

			addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
				Projectiles.config.immersiveColors = originalImmersiveColors;
				Projectiles.config.visibilityMode = originalVisibilityMode;
				Projectiles.config.showHitboxes = originalShowHitboxes;
				Projectiles.config.lineThickness = originalLineThickness;

				this.minecraft.setScreen(parent);
			}).bounds(startX, bottomY, buttonW, buttonH).build());

			addRenderableWidget(Button.builder(Component.literal("Save & Quit"), btn -> {
				Projectiles.config.immersiveColors = immersiveColors;
				Projectiles.config.visibilityMode = visibilityMode;
				Projectiles.config.showHitboxes = showHitboxes;
				Projectiles.config.lineThickness = lineThickness;

				Projectiles.config.save();

				this.minecraft.setScreen(parent);
			}).bounds(startX + buttonW + 10, bottomY, buttonW, buttonH).build());
		}

		private Component getToggleText(boolean value) {
			return value
					? Component.literal("Yes").withStyle(ChatFormatting.GREEN)
					: Component.literal("No").withStyle(ChatFormatting.RED);
		}

		private Component getModeText(ProjectilesConfig.VisibilityMode mode) {
			String text = mode.name().charAt(0) + mode.name().substring(1).toLowerCase();

			return Component.literal(text);
		}

		private Component getThicknessText(ProjectilesConfig.LineThickness thickness) {
			String text = thickness.name().charAt(0) + thickness.name().substring(1).toLowerCase();

			return Component.literal(text);
		}

		@Override
		public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
			super.render(graphics, mouseX, mouseY, delta);

			graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

			int contentW = 150 * 2 + 10; // 310
			int startX = (this.width - contentW) / 2;
			int rowY = 60;
			int rowSpacing = 24;

			graphics.drawString(this.font, Component.literal("Immersive Colors"), startX, rowY + 6, 0xFFFFFFFF);
			graphics.drawString(this.font, Component.literal("Visibility Mode"), startX, rowY + rowSpacing + 6,
					0xFFFFFFFF);
			graphics.drawString(this.font, Component.literal("Show Hitboxes"), startX, rowY + rowSpacing * 2 + 6,
					0xFFFFFFFF);
			graphics.drawString(this.font, Component.literal("Line Thickness"), startX, rowY + rowSpacing * 3 + 6,
					0xFFFFFFFF);
		}

		@Override
		public void onClose() {
			Projectiles.config.immersiveColors = originalImmersiveColors;
			Projectiles.config.visibilityMode = originalVisibilityMode;
			Projectiles.config.showHitboxes = originalShowHitboxes;
			Projectiles.config.lineThickness = originalLineThickness;

			this.minecraft.setScreen(parent);
		}
	}
}