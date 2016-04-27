package com.spear.bitbucket.multibranch.conditions;

import static org.junit.Assert.*;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.spear.bitbucket.multibranch.conditions.ManualButtonCondition;
import com.spear.bitbucket.multibranch.helper.SettingsService;

public class ManualButtonConditionTest {
	private Map<String, Object> context;
	private SettingsService settingsService;
	private PullRequestService pullRequestService;
	private ManualButtonCondition condition;
	private Repository repository;

	@Before
	public void setup() throws Exception {
		settingsService = mock(SettingsService.class);
		pullRequestService = mock(PullRequestService.class);
		
		repository = mock(Repository.class);

		context = new HashMap<String, Object>();
		context.put("repository", repository);

		condition = new ManualButtonCondition(settingsService, pullRequestService);
	}

	@Test
	public void testShouldNotDisplayIfRepositoryNullOrNotRepository() {
		context.put("repository", null);
		assertFalse(condition.shouldDisplay(context));

		context.put("repository", "notARepository");
		assertFalse(condition.shouldDisplay(context));
	}
}
