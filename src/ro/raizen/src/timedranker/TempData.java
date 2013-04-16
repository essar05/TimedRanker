package ro.raizen.src.timedranker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;


public class TempData {
	private TimedRanker plugin;
	public HashMap<String, Integer> data = new HashMap<String, Integer>();  
	
	public TempData(TimedRanker plugin) {
		this.plugin = plugin;
	}
	
	public Integer get(String key) {
		return data.get(key);
	}

	public void set(String key, Integer value) {
		data.put(key, value);
	}
	
	public boolean isset(String key) {
		return data.containsKey(key);
	}
	
	public void save() {
		//Save temp data to the sql db
		Set<String> keys = data.keySet(); //get keys in hashmap and iterate through them
		for(String s : keys) {
			try {
				ResultSet result = plugin.sqlite.query("SELECT count(*) AS cnt, playtime FROM playtime WHERE playername = '" + s + "';");
				int count = result.getInt("cnt");
				if(count > 0) { //if the current player is in the table, update his playtime
					int TotalTime = result.getInt("playtime");
					TotalTime += data.get(s);
					plugin.sqlite.query("UPDATE playtime SET playtime='" + TotalTime + "' WHERE playername = '" + s + "';");
				}
				else { //otherwise insert him in the table
					plugin.sqlite.query("INSERT INTO playtime(id, playername, playtime) VALUES (NULL, '" + s + "', '" + data.get(s) + "');");
				}
				result.close();
			} catch (SQLException e) {
				plugin.clog.info(String.format("[%s] %s", plugin.getDescription().getName(), e.getMessage()));
			}
		}
		plugin.debugInfo("TempData saved to database");
	}
	
	public void purge() {
		data.clear();
	}
	
}
