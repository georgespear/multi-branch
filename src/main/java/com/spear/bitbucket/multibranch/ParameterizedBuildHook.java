package com.spear.bitbucket.multibranch;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.atlassian.bitbucket.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.RepositorySettingsValidator;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.spear.bitbucket.multibranch.ciserver.BuildInfo;
import com.spear.bitbucket.multibranch.ciserver.GlobalSettings;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.ciserver.RepoSettings;
import com.spear.bitbucket.multibranch.helper.SettingsService;
import com.spear.bitbucket.multibranch.helper.Trigger;

public class ParameterizedBuildHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

	private static final String REFS_HEADS = "refs/heads/";

	private final SettingsService settingsService;
	private final Jenkins jenkins;

	public ParameterizedBuildHook(SettingsService settingsService, Jenkins jenkins) {
		this.settingsService = settingsService;
		this.jenkins = jenkins;
	}

	@Override
	public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
		for (RefChange refChange : refChanges) {
			String branch = refChange.getRef().getId().replace(REFS_HEADS, "");
			String commit = refChange.getToHash();
			RepoSettings repoSettings = settingsService.getRepoSettings(context.getSettings());
			String jenkinsProjectName = repoSettings.getJenkinsProjectName();
			Trigger trigger = buildBranchCheck(refChange, branch, repoSettings.getBranchRegex());
			BuildInfo buildInfo = new BuildInfo(branch, null, commit, null, "Auto-triggered for branch " + branch, trigger);

			jenkins.triggerJob(buildInfo, jenkinsProjectName);
		}
	}

	public Trigger buildBranchCheck(RefChange refChange, String branch, String branchCheck) {
		if (refChange.getType() == RefChangeType.UPDATE) {
			if ((branchCheck.isEmpty() || branch.toLowerCase().matches(branchCheck.toLowerCase()))) {
				return Trigger.PUSH;
			}
		} else if (refChange.getType() == RefChangeType.ADD) {
			if (branchCheck.isEmpty() || branch.toLowerCase().matches(branchCheck.toLowerCase())) {
				return Trigger.ADD;
			}
		} else if (refChange.getType() == RefChangeType.DELETE) {
			if (branchCheck.isEmpty() || branch.toLowerCase().matches(branchCheck.toLowerCase())) {
				return Trigger.DELETE;
			}
		}
		return Trigger.NULL;
	}

	@Override
	public void validate(Settings settings, SettingsValidationErrors errors, Repository repository) {

		GlobalSettings server = jenkins.getSettings();
		if (server == null || server.getBaseUrl().isEmpty()) {
			errors.addFieldError("jenkins-admin-error", "Jenkins is not setup in Bitbucket");
			return;
		}
		RepoSettings repoSettings = settingsService.getRepoSettings(settings);

		PatternSyntaxException branchExecption = null;
		try {
			Pattern.compile(repoSettings.getBranchRegex());
		} catch (PatternSyntaxException e) {
			branchExecption = e;
		}
		if (branchExecption != null) {
			errors.addFieldError(SettingsService.BRANCH_REGEX, branchExecption.getDescription());
		}

	}
}