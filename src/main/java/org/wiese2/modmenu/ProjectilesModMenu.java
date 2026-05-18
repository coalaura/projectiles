package org.wiese2.modmenu;

import org.wiese2.Projectiles;

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

		public ProjectilesConfigScreen(Screen parent) {
			super(Component.literal("Sightline Config"));

			this.parent = parent;

			this.immersiveColors = Projectiles.config.immersiveColors;

			this.originalImmersiveColors = this.immersiveColors;
		}

		@Override
		protected void init() {
			int buttonW = 150;
			int buttonH = 20;
			int rowY = 60;

			int contentW = buttonW * 2 + 10;
			int startX = (this.width - contentW) / 2;
			int buttonX = startX + contentW - buttonW;

			addRenderableWidget(Button.builder(getToggleText(immersiveColors), btn -> {
				immersiveColors = !immersiveColors;

				Projectiles.config.immersiveColors = immersiveColors;

				btn.setMessage(getToggleText(immersiveColors));
			}).bounds(buttonX, rowY, buttonW, buttonH).build());

			int bottomY = this.height - 30;

			addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
				Projectiles.config.immersiveColors = originalImmersiveColors;

				this.minecraft.setScreen(parent);
			}).bounds(startX, bottomY, buttonW, buttonH).build());

			addRenderableWidget(Button.builder(Component.literal("Save & Quit"), btn -> {
				Projectiles.config.immersiveColors = immersiveColors;

				Projectiles.config.save();

				this.minecraft.setScreen(parent);
			}).bounds(startX + buttonW + 10, bottomY, buttonW, buttonH).build());
		}

		private Component getToggleText(boolean value) {
			return value
					? Component.literal("Yes").withStyle(ChatFormatting.GREEN)
					: Component.literal("No").withStyle(ChatFormatting.RED);
		}

		@Override
		public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
			super.render(graphics, mouseX, mouseY, delta);

			graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

			int contentW = 150 * 2 + 10;
			int startX = (this.width - contentW) / 2;

			graphics.drawString(this.font, Component.literal("Immersive Colors"), startX, 60 + 6, 0xFFFFFFFF);
		}

		@Override
		public void onClose() {
			Projectiles.config.immersiveColors = originalImmersiveColors;

			this.minecraft.setScreen(parent);
		}
	}
}