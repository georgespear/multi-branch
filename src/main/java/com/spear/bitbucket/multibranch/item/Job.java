package com.spear.bitbucket.multibranch.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Job {
    private int jobId;
    private String jobName;
    private List<Trigger> triggers;
    private String token;
    private Map<String, String> buildParameters;
    private String branchRegex;
    private String pathRegex;

    public Job(int jobId, String jobName, List<Trigger> triggers, String token, Map<String, String> buildParameters, String branchRegex, String pathRegex) {
    	this.jobId = jobId;
    	this.jobName = jobName;
    	this.triggers = triggers;
    	this.token = token;
    	this.buildParameters = buildParameters;
    	this.branchRegex = branchRegex;
    	this.pathRegex = pathRegex;
    }
    
	public int getJobId() {
		return jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

    public String getToken() {
		return token;
	}

	public Map<String, String> getBuildParameters() {
		return buildParameters;
	}

	public String getBranchRegex() {
		return branchRegex;
	}

	public String getPathRegex() {
		return pathRegex;
	}

	public static class JobBuilder
    {
       private int nestedJobId;
       private String nestedJobName;
       private List<Trigger> nestedTriggers;
       private String nestedToken;
       private Map<String, String> nestedBuildParameters;
       private String nestedBranchRegex;
       private String nestedPathRegex;

       public JobBuilder(
          final int jobId) 
       {
    	   this.nestedJobId = jobId;
       }

       public JobBuilder jobId(int jobId)
       {
    	   this.nestedJobId = jobId;
    	   return this;
       }

       public JobBuilder jobName(String jobName)
       {
    	   this.nestedJobName = jobName;
    	   return this;
       }

       public JobBuilder triggers(String[] triggersAry)
       {
    	   List<Trigger> triggers = new ArrayList<Trigger>();
    	   for (String trig : triggersAry){
    		   try {
    			   triggers.add(Trigger.valueOf(trig.toUpperCase()));
			   } catch (IllegalArgumentException e) {
				   triggers.add(Trigger.NULL);
			   }
		   }
    	   this.nestedTriggers = triggers;
    	   return this;
       }
       
       public JobBuilder token(String token)
       {
    	   this.nestedToken = token;
    	   return this;
       }
       
       public JobBuilder buildParameters(String parameterString)
       {
    	   Map<String, String> parameterMap = new LinkedHashMap<String, String>();
    	   if (!parameterString.isEmpty()){
    		   	String lines[] = parameterString.split("\\r?\\n");
		   		for (String line : lines){
	   				String[] pair = line.split("=");
	   				String key = pair[0];
	   				String value = pair.length > 1 ? pair[1] : "";
	   				parameterMap.put(key, value);
		   		}
    	   }
    	   this.nestedBuildParameters = parameterMap;
    	   return this;
       }

       public JobBuilder branchRegex(String branchRegex)
       {
    	   this.nestedBranchRegex = branchRegex;
    	   return this;
       }

       public JobBuilder pathRegex(String pathRegex)
       {
    	   this.nestedPathRegex = pathRegex;
    	   return this;
       }

       public Job createJob()
       {
    	   return new Job(nestedJobId, nestedJobName, nestedTriggers, nestedToken, nestedBuildParameters, nestedBranchRegex, nestedPathRegex);
       }
    }

	public String getQueryString(String branch, String commit, String prDestination) {
		String queryParams = "";
		Iterator<Entry<String, String>> it = buildParameters.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
			queryParams += pair.getKey() + "=" + pair.getValue().split(";")[0] + (it.hasNext() ? "&" : "");
	        it.remove();
	    }
		if (!branch.isEmpty()){queryParams = queryParams.replace("$BRANCH", branch);}
		if (!commit.isEmpty()){queryParams = queryParams.replace("$COMMIT", commit);}
		if (!prDestination.isEmpty()){queryParams = queryParams.replace("$PRDESTINATION", prDestination);}
		return queryParams;
	}
	
	public enum Trigger {
		ADD, PUSH, PULLREQUEST, MANUAL, DELETE, PRMERGED, PRDECLINED, NULL;
	}
}
