package com.spear.bitbucket.multibranch.conditions;

import java.util.Map;

import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

/**
 * A Condition that passes when the webhook is enabled for the provided
 * repository.
 */
public class ManualButtonCondition implements Condition {

	private static final String REPOSITORY = "repository";

	private PullRequestService pullRequestService;

	public ManualButtonCondition(PullRequestService pullRequestService) {
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
		
		return true;
	}
}
