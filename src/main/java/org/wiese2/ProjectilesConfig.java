package org.wiese2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class ProjectilesConfig {
	private static final Gson GsonBuilder = new GsonBuilder().setPrettyPrinting().create();
	private static final Path ConfigPath = FabricLoader.getInstance().getConfigDir().resolve("projectiles.json");

	public boolean immersiveColors = false;

	public ProjectilesConfig() {
		load();
	}

	public void load() {
		if (Files.exists(ConfigPath)) {
			try {
				ProjectilesConfig loaded = GsonBuilder.fromJson(Files.readString(ConfigPath), ProjectilesConfig.class);

				this.immersiveColors = loaded.immersiveColors;
			} catch (Exception e) {
				// ignored, keep defaults
			}
		}
	}

	public void save() {
		try {
			Files.writeString(ConfigPath, GsonBuilder.toJson(this));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}