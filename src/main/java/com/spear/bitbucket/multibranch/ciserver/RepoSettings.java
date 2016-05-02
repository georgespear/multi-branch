package com.spear.bitbucket.multibranch.ciserver;

public class RepoSettings {

	private String branchRegex;
	private String jenkinsProjectName;

	public RepoSettings(String branchRegex, String jenkinsProjectName) {
		super();
		this.branchRegex = branchRegex != null ? branchRegex : "";
		this.jenkinsProjectName = jenkinsProjectName;
	}

	public String getBranchRegex() {
		return branchRegex;
	}

	public String getJenkinsProjectName() {
		return jenkinsProjectName;
	}

}
