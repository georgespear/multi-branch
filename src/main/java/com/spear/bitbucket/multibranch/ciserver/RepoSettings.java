package com.spear.bitbucket.multibranch.ciserver;

public class RepoSettings {

	private String branchRegex;
	private String jenkinsProjectName;
	private boolean skipPRFromBranchBuild = false;

	public RepoSettings(String branchRegex, String jenkinsProjectName, Boolean skipPRFromBranchBuild) {
		super();
		this.branchRegex = branchRegex != null ? branchRegex : "";
		this.jenkinsProjectName = jenkinsProjectName;
		this.skipPRFromBranchBuild = (skipPRFromBranchBuild == Boolean.TRUE);
	}

	public String getBranchRegex() {
		return branchRegex;
	}

	public String getJenkinsProjectName() {
		return jenkinsProjectName;
	}

	public boolean isSkipPRFromBranchBuild() {
		return skipPRFromBranchBuild;
	}

}
