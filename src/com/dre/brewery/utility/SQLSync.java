package com.dre.brewery.utility;

import com.dre.brewery.BPlayer;
import com.dre.brewery.P;
import com.dre.brewery.filedata.BConfig;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class SQLSync {
	private BlockingQueue<Object> saveDataQueue = new ArrayBlockingQueue<>(64);
	private static boolean sqlTaskRunning = false;

	private Connection connection;
	private String connector;
	private String user, password;


	public void updatePlayer(Player player, BPlayer bPlayer) {
		removePlayer(player.getUniqueId());
		SQLData_BP bP = new SQLData_BP();
		bP.uuid = player.getUniqueId();
		bP.drunkeness = bPlayer.getDrunkeness();
		bP.offlineDrunk = bPlayer.getOfflineDrunkeness();
		bP.quality = bPlayer.getQuality();
		bP.data = null;

		addSaveData(bP);
	}

	public void updateData(String name, String data) {
		removeData(name);
		SQLData_BD bD = new SQLData_BD();
		bD.name = name;
		bD.data = data;

		addSaveData(bD);
	}

	public void removePlayer(UUID uuid) {
		SQLRemove_BP r = new SQLRemove_BP();
		r.uuid = uuid;

		addSaveData(r);
	}

	public void removeData(String name) {
		SQLRemove_BD r = new SQLRemove_BD();
		r.name = name;

		addSaveData(r);
	}

	private void addSaveData(Object object) {
		initAsyncTask();
		try {
			if (!saveDataQueue.offer(object, 5, TimeUnit.SECONDS)) {
				BConfig.sqlSync = null;
				closeConnection();
				P.p.errorLog("SQL saving queue overrun, disabling SQL saving");
			}
		} catch (InterruptedException | SQLException e) {
			e.printStackTrace();
		}
	}

	private void initAsyncTask() {
		if (sqlTaskRunning) return;
		sqlTaskRunning = true;
		P.p.getServer().getScheduler().runTaskAsynchronously(P.p, new SQLSaver());
	}


	public boolean init(String user, String password) {
		this.user = user;
		this.password = password;

		if (BConfig.sqlHost == null || BConfig.sqlPort == null || user == null || BConfig.sqlDB == null || password == null) {
			P.p.errorLog("Mysql settings not correctly defined!");
			return false;
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
			String str = "jdbc:mysql://" + BConfig.sqlHost + ":" + BConfig.sqlPort + "/" + BConfig.sqlDB;
			connection = DriverManager.getConnection(str, user, password);

			Statement statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS BreweryZ_BPlayers (" +
				"uuid CHAR(36) NOT NULL, " +
				"quality INT, " +
				"drunkeness INT, " +
				"offlineDrunk INT, " +
				"data VARCHAR(127), " +
				"PRIMARY KEY (uuid));");
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS BreweryZ_BData (" +
				"id SMALLINT AUTOINCREMENT, " +
				"name VARCHAR(127) NOT NULL UNIQUE, " +
				"data TEXT, " +
				"PRIMARY KEY (id));");

			connector = str;
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean openConnection() {
		if (connector == null) {
			return false;
		}
		try {
			connection = DriverManager.getConnection(connector, user, password);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean checkConnection() throws SQLException {
		return connection != null && !connection.isClosed();
	}

	synchronized public boolean closeConnection() throws SQLException {
		if (connection == null) {
			return false;
		}
		connection.close();
		return true;
	}

	private static class SQLData_BP {
		public UUID uuid;
		public int quality;
		public int drunkeness;
		public int offlineDrunk;
		public String data;
	}

	private static class SQLData_BD {
		public String name;
		public String data;
	}

	private static class SQLRemove_BP {
		public UUID uuid;
	}

	private static class SQLRemove_BD {
		public String name;
	}

	private class SQLSaver implements Runnable {

		@Override
		public void run() {
			try {
				while (true) {
					try {
						Object o = saveDataQueue.take(); // Wait for next element in queue

						if (!checkConnection()) {
							if (!openConnection()) {
								P.p.errorLog("Opening SQL Connection failed");
								return;
							}
						}

						if (o instanceof SQLData_BP) {
							SQLData_BP d = ((SQLData_BP) o);
							PreparedStatement ps = connection.prepareStatement("INSERT INTO BreweryZ_BPlayers (uuid, quality, drunkeness, offlineDrunk, data) VALUES (?, ?, ?, ?, ?);");
							ps.setString(1, d.uuid.toString());
							ps.setInt(2, d.quality);
							ps.setInt(3, d.drunkeness);
							ps.setInt(4, d.offlineDrunk);
							ps.setString(5, d.data);

							ps.executeUpdate();
						} else if (o instanceof SQLData_BD) {
							SQLData_BD d = ((SQLData_BD) o);
							PreparedStatement ps = connection.prepareStatement("INSERT INTO BreweryZ_BData (name, data) VALUES (?, ?);");
							ps.setString(1, d.name);
							ps.setString(2, d.data);

							ps.executeUpdate();
						} else if (o instanceof SQLRemove_BP) {
							SQLRemove_BP r = ((SQLRemove_BP) o);
							PreparedStatement ps = connection.prepareStatement("DELETE FROM BreweryZ_BPlayers WHERE uuid = ?;");
							ps.setString(1, r.uuid.toString());

							ps.executeUpdate();
						} else if (o instanceof SQLRemove_BD) {
							SQLRemove_BD r = ((SQLRemove_BD) o);
							PreparedStatement ps = connection.prepareStatement("DELETE FROM BreweryZ_BData WHERE name = ?;");
							ps.setString(1, r.name);

							ps.executeUpdate();
						}

					} catch (InterruptedException e) {
						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} finally {
				sqlTaskRunning = false;
			}
		}
	}

}
