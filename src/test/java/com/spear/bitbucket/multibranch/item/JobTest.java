package com.spear.bitbucket.multibranch.item;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import org.junit.Before;
import org.junit.Test;

import com.spear.bitbucket.multibranch.item.Job;
import com.spear.bitbucket.multibranch.item.Job.Trigger;

public class JobTest {
	
	@Before
	public void setup() throws Exception {
		
	}
	
	@Test
	public void testCreateNewJob() {
		int jobId = 0;
		String jobName = "test_job";
		List<Trigger> triggers = new ArrayList<Trigger>();
		triggers.add(Trigger.ADD);
		triggers.add(Trigger.MANUAL);
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put("param1", "value1");
		parameters.put("param2", "value2");
		String branch = "branch";
		String path = "path";
		
		Job job = new Job
				.JobBuilder(jobId)
				.jobName(jobName)
				.triggers(new String[]{"add", "manual"})
				.buildParameters("param1=value1\r\nparam2=value2")
				.branchRegex(branch)
				.pathRegex(path)
				.createJob();
		
		assertEquals(jobId, job.getJobId());
		assertEquals(jobName, job.getJobName());
		assertEquals(triggers, job.getTriggers());
		assertEquals(parameters, job.getBuildParameters());
		assertEquals(branch, job.getBranchRegex());
		assertEquals(path, job.getPathRegex());
	}
}
