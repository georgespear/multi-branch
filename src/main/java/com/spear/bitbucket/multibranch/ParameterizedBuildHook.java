package com.spear.bitbucket.multibranch;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsBetweenRequest;
import com.atlassian.bitbucket.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookContext;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.RepositorySettingsValidator;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.spear.bitbucket.multibranch.ciserver.BuildInfo;
import com.spear.bitbucket.multibranch.ciserver.GlobalSettings;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.ciserver.RepoSettings;
import com.spear.bitbucket.multibranch.helper.SettingsService;
import com.spear.bitbucket.multibranch.helper.Trigger;

public class ParameterizedBuildHook implements AsyncPostReceiveRepositoryHook, RepositorySettingsValidator {

	private static final String REFS_HEADS = "refs/heads/";

	private final SettingsService settingsService;
	private final CommitService commitService;
	private final Jenkins jenkins;
	private static final PageRequestImpl PAGE_REQUEST = new PageRequestImpl(0, 100);
	private static final String MAVEN_RELEASE_IGNORE_PATTERN = "[maven-release-plugin]";
	

	public ParameterizedBuildHook(SettingsService settingsService, CommitService commitService, Jenkins jenkins) {
		this.settingsService = settingsService;
		this.commitService = commitService;
		this.jenkins = jenkins;
	}

	@Override
	public void postReceive(RepositoryHookContext context, Collection<RefChange> refChanges) {
		RepoSettings repoSettings = settingsService.getRepoSettings(context.getSettings());
		for (RefChange refChange : refChanges) {
			String branch = refChange.getRef().getId().replace(REFS_HEADS, "");
			String commit = refChange.getToHash();
			
			String jenkinsProjectName = repoSettings.getJenkinsProjectName();
			final CommitsBetweenRequest changesetsBetweenRequest = new CommitsBetweenRequest.Builder(context.getRepository())
                    .include(refChange.getToHash())
                    .exclude(refChange.getFromHash())
                    .build();
			Trigger trigger = buildBranchCheck(refChange, branch, repoSettings.getBranchRegex());
			if (refChange.getType() == RefChangeType.UPDATE && !checkCommitMessageValid(commitService.getCommitsBetween(changesetsBetweenRequest, PAGE_REQUEST)))
				continue;

			
			BuildInfo buildInfo = new BuildInfo(branch, null, commit, null, "Auto-triggered for branch " + branch, trigger);

			jenkins.triggerJob(buildInfo, jenkinsProjectName);
		}
	}

	private boolean checkCommitMessageValid(Page<Commit> commits) {
		for (Commit changeset : commits.getValues()) {
			// System.err.println("Commit message : " + changeset.getme);
			if (changeset.getMessage() != null && changeset.getMessage().startsWith(MAVEN_RELEASE_IGNORE_PATTERN)) {
				return false;
			}
		}
		return true;
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