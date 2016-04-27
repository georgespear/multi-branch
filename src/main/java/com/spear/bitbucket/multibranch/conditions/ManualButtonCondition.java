package com.spear.bitbucket.multibranch.conditions;

import java.util.Map;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.spear.bitbucket.multibranch.helper.SettingsService;
import com.spear.bitbucket.multibranch.item.Job;
import com.spear.bitbucket.multibranch.item.Job.Trigger;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;

/**
 * A Condition that passes when the webhook is enabled for the provided
 * repository.
 */
public class ManualButtonCondition implements Condition {

	private static final String REPOSITORY = "repository";

	private SettingsService settingsService;
	private PullRequestService pullRequestService;

	public ManualButtonCondition(SettingsService settingsService, PullRequestService pullRequestService) {
		this.settingsService = settingsService;
		this.pullRequestService = pullRequestService;
	}
	
	@Override
	public void init(Map<String, String> context) throws PluginParseException {
		// Nothing to do here
	}
	
	@Override
	public boolean shouldDisplay(Map<String, Object> context) {
		final Object obj = context.get(REPOSITORY);
		// Get current repo, if failure disable button
		if (obj == null || !(obj instanceof Repository))
			return false;

		final Repository repository = (Repository) obj;
		final PullRequest pullRequest = (PullRequest)context.get("pullRequest");
		if (pullRequest != null && pullRequestService.canMerge(repository.getId(), pullRequest.getId()).isConflicted())
			return false;
		
		Settings settings = settingsService.getSettings(repository);

		for (Job job : settingsService.getJobs(settings.asMap())){
			if (job.getTriggers().contains(Trigger.MANUAL)){
				return true;
			}
		}
		return false;
	}
}
