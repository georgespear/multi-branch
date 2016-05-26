package com.spear.bitbucket.multibranch;

import java.io.IOException;

import com.atlassian.bitbucket.event.pull.PullRequestDeclinedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.event.api.EventListener;
import com.spear.bitbucket.multibranch.ciserver.BuildInfo;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.ciserver.RepoSettings;
import com.spear.bitbucket.multibranch.helper.SettingsService;
import com.spear.bitbucket.multibranch.helper.Trigger;

public class PullRequestHook {
	private final SettingsService settingsService;
	private final PullRequestService pullRequestService;
	private final Jenkins jenkins;

	public PullRequestHook(SettingsService settingsService, PullRequestService pullRequestService, Jenkins jenkins) {
		this.settingsService = settingsService;
		this.pullRequestService = pullRequestService;
		this.jenkins = jenkins;
	}

	@EventListener
	public void onPullRequestOpened(PullRequestOpenedEvent event) throws IOException {
		PullRequest pullRequest = event.getPullRequest();
		triggerFromPR(pullRequest, Trigger.PULLREQUEST);
	}

	@EventListener
	public void onPullRequestReOpened(PullRequestReopenedEvent event) throws IOException {
		PullRequest pullRequest = event.getPullRequest();
		triggerFromPR(pullRequest, Trigger.PULLREQUEST);
	}

	@EventListener
	public void onPullRequestRescoped(PullRequestRescopedEvent event) throws IOException {
		final PullRequest pullRequest = event.getPullRequest();
		if (!event.getPreviousFromHash().equals(pullRequest.getFromRef().getLatestCommit())) {
			triggerFromPR(pullRequest, Trigger.PULLREQUEST);
		}
	}

	@EventListener
	public void onPullRequestMerged(PullRequestMergedEvent event) throws IOException {
		//PullRequest pullRequest = event.getPullRequest();
		//triggerFromPR(pullRequest, Trigger.PRMERGED);
	}

	@EventListener
	public void onPullRequestDeclined(PullRequestDeclinedEvent event) throws IOException {
		final PullRequest pullRequest = event.getPullRequest();
		triggerFromPR(pullRequest, Trigger.PRDECLINED);
	}

	public void triggerFromPR(PullRequest pullRequest, Trigger trigger) throws IOException {
		final Repository repository = pullRequest.getFromRef().getRepository();
		if (pullRequest.isClosed() || pullRequestService.canMerge(repository.getId(), pullRequest.getId()).isConflicted())
			return;
		String branch = pullRequest.getFromRef().getDisplayId();
		String commit = pullRequest.getFromRef().getLatestCommit();
		String prDest = pullRequest.getToRef().getDisplayId();
		String destCommit = pullRequest.getToRef().getLatestCommit();
		Settings settings = settingsService.getSettings(repository);

		if (settings == null) {
			return;
		}
		RepoSettings repoSettings = settingsService.getRepoSettings(settings);
		String jenkinsProjectName = repoSettings.getJenkinsProjectName();

		BuildInfo buildInfo = new BuildInfo(branch, prDest, commit, destCommit, "Pull request rebuild for " + branch + " -> " + prDest,
				trigger);
		buildInfo.setPrId(pullRequest.getId());
		buildInfo.setPrTitle(pullRequest.getTitle());
		jenkins.triggerJob(buildInfo, jenkinsProjectName);
	}
}
