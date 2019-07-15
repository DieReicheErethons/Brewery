package com.dre.brewery.filedata;

import com.dre.brewery.P;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


/**
 * Update Checker modified from the Gravity Update Checker Example:
 * https://github.com/gravitylow/ServerModsAPI-Example/blob/master/Update.java
 */
public class UpdateChecker implements Runnable {
	// The project's unique ID
	private static final int projectID = 68006;

	// Used for locating version numbers in file names
	private static final String DELIMETER = "^v|[\\s_-]v";

	// Keys for extracting file information from JSON response
	private static final String API_NAME_VALUE = "name";
/*	private static final String API_LINK_VALUE = "downloadUrl";
	private static final String API_RELEASE_TYPE_VALUE = "releaseType";
	private static final String API_FILE_NAME_VALUE = "fileName";
	private static final String API_GAME_VERSION_VALUE = "gameVersion";*/

	// Static information for querying the API
	private static final String API_QUERY = "/servermods/files?projectIds=";
	private static final String API_HOST = "https://api.curseforge.com";

	public static String update = null;

	public static void notify(final Player player) {
		if (update == null || !player.isOp()) {
			return;
		}
		P.p.msg(player, update);
	}

	@Override
	public void run() {
		query();
	}

	/**
	 * Query the API to find the latest approved file's details.
	 */
	public void query() {
		URL url;

		try {
			// Create the URL to query using the project's ID
			url = new URL(API_HOST + API_QUERY + projectID);
		} catch (MalformedURLException e) {
			// There was an error creating the URL

			e.printStackTrace();
			return;
		}

		try {
			// Open a connection and query the project
			URLConnection conn = url.openConnection();

			/*if (apiKey != null) {
				// Add the API key to the request if present
				conn.addRequestProperty("X-API-Key", apiKey);
			}*/

			// Add the user-agent to identify the program
			conn.addRequestProperty("User-Agent", "Brewery UpdateChecker (by Gravity)");

			// Read the response of the query
			final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			JsonParser parser = new JsonParser();


			// Parse the array of files from the query's response
			JsonArray array = parser.parse(reader).getAsJsonArray();

			if (array.size() > 0) {
				// Get the newest file's details
				JsonObject latest = array.get(array.size() - 1).getAsJsonObject();

				// Get the version's title
				String versionName = latest.get(API_NAME_VALUE).getAsString();

				/*// Get the version's link
				String versionLink = (String) latest.get(API_LINK_VALUE);

				// Get the version's release type
				String versionType = (String) latest.get(API_RELEASE_TYPE_VALUE);

				// Get the version's file name
				String versionFileName = (String) latest.get(API_FILE_NAME_VALUE);

				// Get the version's game version
				String versionGameVersion = (String) latest.get(API_GAME_VERSION_VALUE);*/

				String[] split = versionName.split(DELIMETER);
				if (split.length < 2) {
					P.p.log("Malformed Remote File Name, could not check for updates");
				} else {
					String version = split[1];
					if (!P.p.getDescription().getVersion().equals(split[1].split(" ")[0])) {
						String[] verNew = version.split("\\.");
						String[] verOld = P.p.getDescription().getVersion().split("\\.");

						for (int i = 0; i < verNew.length; i++) {
							if (i < verOld.length && P.p.parseInt(verOld[i]) > P.p.parseInt(verNew[i])) {
								break;
							} else if (i >= verOld.length || P.p.parseInt(verOld[i]) < P.p.parseInt(verNew[i])) {
								P.p.log("Update available for Brewery-" + P.p.getDescription().getVersion() + ": " + versionName);
								update = "Update available: v" + version;
								break;
							}
						}
					}
				}

			} else {
				P.p.log("There are no files for this project");
			}
		} catch (IOException e) {
			// There was an error reading the query
			P.p.errorLog("Could not check for Updates. This error can probably be ignored");
			e.printStackTrace();
		}
	}
}
