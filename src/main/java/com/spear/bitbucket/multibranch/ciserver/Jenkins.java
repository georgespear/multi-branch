package com.spear.bitbucket.multibranch.ciserver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.spear.bitbucket.multibranch.item.Job;
import com.spear.bitbucket.multibranch.item.Server;
import com.spear.bitbucket.multibranch.item.Job.Trigger;

public class Jenkins {

	private static final String PLUGIN_KEY = "com.spear.bitbucket.multibranch";
	private final PluginSettings pluginSettings;
	private static final Logger logger = Logger.getLogger(Jenkins.class.getName());

	public Jenkins(PluginSettingsFactory factory) {
		this.pluginSettings = factory.createSettingsForKey(PLUGIN_KEY);
	}

	public void setSettings(String url, String user, String token, boolean altUrl) {
		if (url != null && !url.isEmpty()) {
			String altUrlString = altUrl ? "true" : "false";
			pluginSettings.put(".jenkinsSettings", url + ";" + user + ";" + token + ";" + altUrlString);
		} else {
			pluginSettings.remove(".jenkinsSettings");
		}
	}

	public void setUserSettings(String user, String token) {
		if (user != null && !user.trim().isEmpty() && token != null && !token.isEmpty()) {
			pluginSettings.put(".jenkinsUser." + user, token);
		} else {
			pluginSettings.remove(".jenkinsUser." + user);
		}
	}

	public Server getSettings() {
		Object settingObj = pluginSettings.get(".jenkinsSettings");
		if (settingObj != null) {
			String[] serverProps = settingObj.toString().split(";");
			boolean altUrl = serverProps.length > 3 && serverProps[3].equals("true") ? true : false;
			return new Server(serverProps[0], serverProps[1], serverProps[2], altUrl);
		} else {
			return null;
		}
	}

	public String getUserToken(String user) {
		if (getUserSettings(user) != null) {
			return user + ":" + getUserSettings(user);
		}
		return null;
	}

	public String getUserSettings(String user) {
		Object settingObj = pluginSettings.get(".jenkinsUser." + user);
		if (settingObj != null) {
			return settingObj.toString();
		} else {
			return null;
		}
	}

	public String[] triggerJob(Job job, BuildInfo buildInfo, String jenkinsMultiProjectName, Trigger trigger) {
		String buildUrl = "";
		Server server = getSettings();
		if (server == null) {
			return new String[] { "error", "Jenkins settings are not setup" };
		}

		String ciServer = server.getBaseUrl();

		buildUrl = ciServer + "/job/" + jenkinsMultiProjectName;
		logger.log(Level.INFO, "Triggered by event: " + trigger.name());
		switch (trigger) {
		case ADD:
			return httpPost(buildUrl + "/build");
		case DELETE:
			return httpPost(buildUrl + "/branch/" + buildInfo.getFromRef().replace("/", "%252F") + "/doDelete");
		case MANUAL:
			return httpPost(buildUrl + "/branch/" + buildInfo.getFromRef().replace("/", "%252F") + "/buildWithParameters?" + buildInfo.getQueryParams());
		case PRMERGED:
			return httpPost(buildUrl + "/branch/" + buildInfo.getToRef().replace("/", "%252F") + "/buildWithParameters?" + buildInfo.getQueryParams());
		case PULLREQUEST:
			return httpPost(buildUrl + "/branch/" + buildInfo.getFromRef().replace("/", "%252F") + "/buildWithParameters?" + buildInfo.getQueryParams());
		case PUSH:
			return httpPost(buildUrl + "/branch/" + buildInfo.getFromRef().replace("/", "%252F") + "/buildWithParameters?" + buildInfo.getQueryParams());
		default:
			return null;

		}

	}

	public String[] httpPost(String buildUrl) {
		buildUrl = buildUrl.replace(" ", "%20");
		logger.log(Level.INFO, "Calling Jenkins on URL: " , buildUrl);
		String[] results = new String[2];
		int status = 0;
		// Trigger build using build URL from hook setting
		try {
			URL url = new URL(buildUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");

			connection.setReadTimeout(30000);
			connection.setInstanceFollowRedirects(true);
			HttpURLConnection.setFollowRedirects(true);

			connection.connect();

			status = connection.getResponseCode();
			results[0] = Integer.toString(status);
			if (status == 201) {
				results[1] = "201: Build triggered";
				return results;
			} else {
				results[1] = status + ": " + connection.getResponseMessage();
				return results;
			}

			// log.debug("HTTP response:\n" + body.toString());
		} catch (MalformedURLException e) {
			// log.error("Malformed URL:" + e);
		} catch (IOException e) {
			// log.error("Some IO exception occurred", e);
		} catch (Exception e) {
			// log.error("Something else went wrong: ", e);
		}

		results[0] = "error";
		results[1] = status + ": unknown error";
		return results;
	}
}
