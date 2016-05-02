package com.spear.bitbucket.multibranch.helper;

import com.atlassian.bitbucket.event.hook.RepositoryHookEnabledEvent;
import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.http.HttpScmProtocol;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.event.api.EventListener;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.ciserver.RepoSettings;

public class SettingsService {
	private static final String KEY = "com.spear.bitbucket.multi-branch:multi-branch-build-hook";
	public static final String BRANCH_REGEX = "branchRegex-";
	public static final String JENKINS_PROJECT_NAME = "projectName";

	private RepositoryHookService hookService;
	private SecurityService securityService;
	private final HttpScmProtocol httpScmProtocol;
	private Jenkins jenkins;

	public SettingsService(RepositoryHookService hookService, SecurityService securityService, HttpScmProtocol httpScmProtocol,
			Jenkins jenkins) {
		this.hookService = hookService;
		this.securityService = securityService;
		this.httpScmProtocol = httpScmProtocol;
		this.jenkins = jenkins;
	}

	public Settings getSettings(final Repository repository) {

		Settings settings = null;
		try {
			settings = securityService.withPermission(Permission.REPO_ADMIN, "Get respository settings")
					.call(new Operation<Settings, Exception>() {
						@Override
						public Settings perform() throws Exception {
							return hookService.getSettings(repository, KEY);
						}
					});
		} catch (Exception e) {
			return null;
		}

		return settings;
	}

	public RepositoryHook getHook(final Repository repository) {
		RepositoryHook hook = null;
		try {
			hook = securityService.withPermission(Permission.REPO_ADMIN, "Get respository settings")
					.call(new Operation<RepositoryHook, Exception>() {
						@Override
						public RepositoryHook perform() throws Exception {
							return hookService.getByKey(repository, KEY);
						}
					});
		} catch (Exception e1) {
			return null;
		}
		return hook;
	}

	public RepoSettings getRepoSettings(Settings settings) {
		if (settings == null) {
			return null;
		}
		return new RepoSettings(settings.getString(BRANCH_REGEX), settings.getString(JENKINS_PROJECT_NAME));
	}

	@EventListener
	public void onRepositoryHookEnabledEvent(RepositoryHookEnabledEvent event) {
		Repository repository = event.getRepository();
		String jenkinsProjectName = getRepoSettings(getSettings(repository)).getJenkinsProjectName();
		jenkins.generateMultiBranchJob(httpScmProtocol.getCloneUrl(repository, null), repository.getName(), jenkinsProjectName);

		// ...
	}

}
