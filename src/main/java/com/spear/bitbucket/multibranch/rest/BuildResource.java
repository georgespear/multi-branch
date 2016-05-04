package com.spear.bitbucket.multibranch.rest;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.rest.RestResource;
import com.atlassian.bitbucket.rest.util.ResourcePatterns;
import com.atlassian.bitbucket.rest.util.RestUtils;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.spear.bitbucket.multibranch.ciserver.BuildInfo;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.helper.SettingsService;
import com.spear.bitbucket.multibranch.helper.Trigger;
import com.sun.jersey.spi.resource.Singleton;

@Path(ResourcePatterns.REPOSITORY_URI)
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ RestUtils.APPLICATION_JSON_UTF8 })
@Singleton
@AnonymousAllowed
public class BuildResource extends RestResource {

	private SettingsService settingsService;
	private Jenkins jenkins;
	private final AuthenticationContext authenticationContext;

	public BuildResource(I18nService i18nService, SettingsService settingsService, Jenkins jenkins,
			AuthenticationContext authenticationContext) {
		super(i18nService);
		this.settingsService = settingsService;
		this.jenkins = jenkins;
		this.authenticationContext = authenticationContext;
	}

	@POST
	@Path(value = "triggerBuild")
	public Response triggerBuild(@Context final Repository repository, BuildInfo buildInfo) {
		if (authenticationContext.isAuthenticated()) {
			String[] getResults = new String[2];
			Map<String, String> data = new HashMap<String, String>();
			Settings settings = settingsService.getSettings(repository);

			if (settings == null) {
				return Response.status(404).build();
			}
			String jenkinsProjectName = settingsService.getRepoSettings(settings).getJenkinsProjectName();

			if (buildInfo.getToRef() != null && !buildInfo.getToRef().trim().equals(""))
				buildInfo.setDescription("Manual trigger for " + buildInfo.getFromRef() + " -> " + buildInfo.getToRef());
			else
				buildInfo.setDescription("Manual trigger for " + buildInfo.getFromRef());

			buildInfo.setTrigger(Trigger.MANUAL);
			getResults = jenkins.triggerJob(buildInfo, jenkinsProjectName);

			data.put("status", getResults[0]);
			data.put("message", getResults[1]);
			return Response.ok(data).build();
		}
		return null;
	}

	public static class JobGenerator {
		private String projectName;

		public String getProjectName() {
			return projectName;
		}

		public void setProjectName(String projectName) {
			this.projectName = projectName;
		}
	}
}