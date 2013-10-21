package ro.raizen.src.timedranker;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
	private TimedRanker plugin;
	
	public Database(TimedRanker plugin) {
		this.plugin = plugin;
	}
	
	public ResultSet query(String query) {
		ResultSet result = null;
		if(plugin.mysql == null) {
			try {
				result = plugin.sqlite.query(query);
			} catch (SQLException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
			}
		} else {
			try {
				result = plugin.mysql.query(query);
			} catch (SQLException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
			}
		}
		return result;
	}

	public void close() {
		if(plugin.mysql == null) {
			plugin.sqlite.close();
		} else {
			plugin.mysql.close();
		}
	}
	
}
