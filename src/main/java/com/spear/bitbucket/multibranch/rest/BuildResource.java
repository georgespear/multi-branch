package com.spear.bitbucket.multibranch.rest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.poi.util.StringUtil;
import org.json.JSONObject;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.spear.bitbucket.multibranch.ciserver.BuildInfo;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.helper.SettingsService;
import com.spear.bitbucket.multibranch.item.Job;
import com.spear.bitbucket.multibranch.item.Job.Trigger;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.rest.util.ResourcePatterns;
import com.atlassian.bitbucket.rest.RestResource;
import com.atlassian.bitbucket.rest.util.RestUtils;
import com.atlassian.bitbucket.scm.http.HttpScmProtocol;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.sun.jersey.spi.resource.Singleton;

import antlr.StringUtils;

@Path(ResourcePatterns.REPOSITORY_URI)
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ RestUtils.APPLICATION_JSON_UTF8 })
@Singleton
@AnonymousAllowed
public class BuildResource extends RestResource {

	private SettingsService settingsService;
	private Jenkins jenkins;
	private final AuthenticationContext authenticationContext;
	private final HttpScmProtocol httpScmProtocol;

	public BuildResource(I18nService i18nService,
			SettingsService settingsService, 
			Jenkins jenkins,
			AuthenticationContext authenticationContext, HttpScmProtocol httpScmProtocol) {
		super(i18nService);
		this.settingsService = settingsService;
		this.jenkins = jenkins;
		this.authenticationContext = authenticationContext;
		this.httpScmProtocol = httpScmProtocol;
	}

	@POST
	@Path(value = "triggerBuild/{id}")
	public Response triggerBuild(@Context final Repository repository, @PathParam("id") String id, @Context UriInfo uriInfo) {
		if (authenticationContext.isAuthenticated()){
			String[] getResults = new String[2];
			Map<String, String> data = new HashMap<String, String>();
			Settings settings = settingsService.getSettings(repository);

			if (settings == null){
				return Response.status(404).build();
			}
			String jenkinsProjectName = settingsService.getJenkinsProjectName(settings.asMap());
			List<Job> settingsList = settingsService.getJobs(settings.asMap());
			Job jobToBuild = resolveJobConfigFromUriMap(Integer.parseInt(id), settingsList);
			
			if (jobToBuild == null){
				getResults[0] = "error";
				getResults[1] = "Settings not found for this job";
			} else {
				BuildInfo buildInfo= resolveBuildInfoFromMap(uriInfo.getQueryParameters());
				getResults = jenkins.triggerJob(jobToBuild, buildInfo, jenkinsProjectName, Trigger.MANUAL);
			}
			
			data.put("status", getResults[0]);
			data.put("message", getResults[1]);
			return Response.ok(data).build();
		}
		return null;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path(value = "generateJob")
	public Response generateJob(@Context final Repository repository, JobGenerator jobGenerator) {
		if (authenticationContext.isAuthenticated()){
			Map<String, String> data = new HashMap<String, String>();
			Settings settings = settingsService.getSettings(repository);
			if (settings == null){
				return Response.status(404).build();
			}
			// String jenkinsProjectName = settingsService.getJenkinsProjectName(settings.asMap());
			String[] response = jenkins.generateMultiBranchJob(httpScmProtocol.getCloneUrl(repository, null), repository.getName(), jobGenerator.getProjectName());
			data.put("status", response[0]);
			data.put("message", response[1]);
			return Response.ok(data).build();

		}
		return null;
	}
	
	@GET
	@Path(value = "getJobs")
	public Response getJobs(@Context final Repository repository) {
		if (authenticationContext.isAuthenticated()){
			Settings settings = settingsService.getSettings(repository);
			int count = 0;
			Map<Integer, Object> data = new LinkedHashMap<Integer, Object>();
			if (settings == null){
				return Response.status(404).build();
			}
			for (Job job : settingsService.getJobs(settings.asMap())){
				if (job.getTriggers().contains(Trigger.MANUAL)){
					Map<String, Object> temp = new LinkedHashMap<String, Object>();
					temp.put("id", job.getJobId());
					temp.put("jobName", job.getJobName());
					temp.put("parameters", job.getBuildParameters());
					data.put(count, temp);
					count ++;
				}
			}
			return Response.ok(data).build();
		}
		return null;
	}
	
	protected Job resolveJobConfigFromUriMap(int id, List<Job> settingsList) {
		for (Job job : settingsList){
			if (job.getJobId() == id){
				return job;
			}
		}
		return null;
	}
	
	protected BuildInfo resolveBuildInfoFromMap(MultivaluedMap<String, String> queryParameters) {
		String fromRef = queryParameters.getFirst("fromRef");
		String toRef = queryParameters.getFirst("toRef");
		
		String desc;
		if (toRef != null && !toRef.trim().equals(""))
			desc = "Manual trigger for " + fromRef + " -> " + toRef;
		else 
			desc = "Manual trigger for " + fromRef;
		return new BuildInfo(fromRef, toRef,
				queryParameters.getFirst("fromCommit"), queryParameters.getFirst("toCommit"), desc);
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