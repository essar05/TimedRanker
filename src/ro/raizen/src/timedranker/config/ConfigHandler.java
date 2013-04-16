package ro.raizen.src.timedranker.config;

import ro.raizen.src.timedranker.TimedRanker;

public class ConfigHandler {
	
	private TimedRanker plugin;
	public ConfigHandler(TimedRanker plugin) {
		this.plugin = plugin;
	}
	
	public void saveDefaultConfiguration() {
		plugin.getConfig().options().copyDefaults(true);
		plugin.saveDefaultConfig();
	}
}