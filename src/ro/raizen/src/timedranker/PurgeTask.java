package ro.raizen.src.timedranker;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PurgeTask implements Runnable {

	private TimedRanker plugin;
	
	public PurgeTask(TimedRanker plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void run() {
		try {
			long maxOffline = 1000 * 60 * 60 * 24 * plugin.cfg.getConfig().getInt("purgeAfter");
			ResultSet result = plugin.sql.query("SELECT * FROM `playtime`");
			while(result.next()) {
				String playername = result.getString("playername");
				if(plugin.getServer().getOfflinePlayer(playername) != null && !plugin.getServer().getOfflinePlayer(playername).isOnline()) {
					long offlinePeriod = System.currentTimeMillis() - plugin.getServer().getOfflinePlayer(playername).getLastPlayed();
					
					if(offlinePeriod > maxOffline) {
						plugin.sql.query("DELETE FROM `playtime` WHERE `playername` = '" + playername + "';");
					}
				}
			}
			plugin.debugInfo("Database purge task ran successfully");
		} catch (SQLException e) {
			plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
		}
		
	}

}
