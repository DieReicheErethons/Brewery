package com.dre.brewery.utility;

import com.dre.brewery.BPlayer;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.filedata.BConfig;

import java.sql.*;
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


	public void updatePlayer(UUID uuid, BPlayer bPlayer, boolean offlineDrain) {
		SQLData_BP bP = new SQLData_BP();
		bP.uuid = uuid;
		bP.drunkeness = bPlayer.getDrunkeness();
		bP.offlineDrunk = bPlayer.getOfflineDrunkeness();
		bP.quality = bPlayer.getQualityData();
		bP.data = null;
		bP.offlineDrain = offlineDrain;

		addSaveData(bP);
	}

	public void updateData(String name, String data) {
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
				BreweryPlugin.getInstance().errorLog("SQL saving queue overrun, disabling SQL saving");
			}
		} catch (InterruptedException | SQLException e) {
			e.printStackTrace();
		}
	}

	// Run Async
	public void fetchPlayerLoginData(final UUID uuid) {
		try {
			if (!checkConnection()) {
				if (!openConnection()) {
					BreweryPlugin.getInstance().errorLog("Opening SQL Connection failed");
					return;
				}
			}

			Statement statement = connection.createStatement();
			if (statement.execute("SELECT * FROM Brewery_Z_BPlayers WHERE uuid = '" + uuid.toString() + "';")) {
				final ResultSet result = statement.getResultSet();
				if (result.next()) {
					BreweryPlugin.getScheduler().runTask(() -> {
						try {
							new BPlayer(uuid.toString(), result.getInt("quality"), result.getInt("drunkeness"), result.getInt("offlineDrunk"));
						} catch (SQLException e) {
							e.printStackTrace();
						}
					});
					return;
				}
			}
			BreweryPlugin.getScheduler().runTask(() -> BPlayer.sqlRemoved(uuid));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initAsyncTask() {
		if (sqlTaskRunning) return;
		sqlTaskRunning = true;
		BreweryPlugin.getScheduler().runTaskAsynchronously(new SQLSaver());
	}


	public boolean init(String user, String password) {
		this.user = user;
		this.password = password;

		if (BConfig.sqlHost == null || BConfig.sqlPort == null || user == null || BConfig.sqlDB == null || password == null) {
			BreweryPlugin.getInstance().errorLog("Mysql settings not correctly defined!");
			return false;
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
			String str = "jdbc:mysql://" + BConfig.sqlHost + ":" + BConfig.sqlPort + "/" + BConfig.sqlDB;
			connection = DriverManager.getConnection(str, user, password);

			Statement statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS Brewery_Z_BPlayers (" +
				"uuid CHAR(36) NOT NULL, " +
				"quality INT, " +
				"drunkeness INT, " +
				"offlineDrunk INT, " +
				"data VARCHAR(127), " +
				"PRIMARY KEY (uuid));");
			statement = connection.createStatement();
			statement.execute("CREATE TABLE IF NOT EXISTS Brewery_Z_BData (" +
				"id SMALLINT AUTO_INCREMENT, " +
				"name VARCHAR(127) NOT NULL UNIQUE, " +
				"data TEXT, " +
				"PRIMARY KEY (id));");

			connector = str;

		} catch (SQLException | ClassNotFoundException e) {
			if (BreweryPlugin.debug) {
				e.printStackTrace();
			} else {
				BreweryPlugin.getInstance().errorLog("SQL Exception occured, set 'debug: true' for more info");
				BreweryPlugin.getInstance().errorLog(e.getMessage());
				Throwable cause = e.getCause();
				if (cause != null) {
					BreweryPlugin.getInstance().errorLog(cause.getMessage());
				}
			}
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
		public boolean offlineDrain;
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
								BreweryPlugin.getInstance().errorLog("Opening SQL Connection failed");
								return;
							}
						}

						if (o instanceof SQLData_BP) {
							SQLData_BP d = ((SQLData_BP) o);
							PreparedStatement ps;

							if (d.offlineDrain) {
								// We need to check if the player data is changed by some other server
								// and if so, we ignore him from now on
								ps = connection.prepareStatement("SELECT offlineDrunk FROM Brewery_Z_BPlayers WHERE uuid = ?;");
								ps.setString(1, d.uuid.toString());

								ResultSet resultSet = ps.executeQuery();
								if (resultSet.next()) {
									int storedOfflineDrunk = resultSet.getInt("offlineDrunk");
									if (storedOfflineDrunk != d.offlineDrunk) {
										// The player is not offlineDrunk anymore,
										// Someone else is changing the mysql data
										BreweryPlugin.getScheduler().runTask(() -> BPlayer.sqlRemoved(d.uuid));
										continue;
									}
								}
							}

							ps = connection.prepareStatement("REPLACE INTO Brewery_Z_BPlayers (uuid, quality, drunkeness, offlineDrunk, data) VALUES (?, ?, ?, ?, ?);");
							ps.setString(1, d.uuid.toString());
							ps.setInt(2, d.quality);
							ps.setInt(3, d.drunkeness);
							ps.setInt(4, d.offlineDrunk);
							ps.setString(5, d.data);

							ps.executeUpdate();
						} else if (o instanceof SQLData_BD) {
							SQLData_BD d = ((SQLData_BD) o);
							PreparedStatement ps = connection.prepareStatement("REPLACE INTO Brewery_Z_BData (name, data) VALUES (?, ?);");
							ps.setString(1, d.name);
							ps.setString(2, d.data);

							ps.executeUpdate();
						} else if (o instanceof SQLRemove_BP) {
							SQLRemove_BP r = ((SQLRemove_BP) o);
							PreparedStatement ps = connection.prepareStatement("DELETE FROM Brewery_Z_BPlayers WHERE uuid = ?;");
							ps.setString(1, r.uuid.toString());

							ps.executeUpdate();
						} else if (o instanceof SQLRemove_BD) {
							SQLRemove_BD r = ((SQLRemove_BD) o);
							PreparedStatement ps = connection.prepareStatement("DELETE FROM Brewery_Z_BData WHERE name = ?;");
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
