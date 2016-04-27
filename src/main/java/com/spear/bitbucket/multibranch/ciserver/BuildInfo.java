package com.spear.bitbucket.multibranch.ciserver;

public class BuildInfo {

	public BuildInfo(String fromRef, String toRef, String fromCommit, String toCommit, String description) {
		this.fromRef = fromRef;
		this.toRef = toRef;
		this.fromCommit = fromCommit;
		this.toCommit = toCommit;
		this.description = description;
	}

	/**
	 * Not null, in case of pull request, indicates the FROM branch, otherwise the branch the build was triggered on.
	 */
	private String fromRef;

	/**
	 * Can be null, in case of pull request, indicates the TO branch.
	 */
	private String toRef;

	/**
	 * Not null, in case of pull request, indicates the FROM commit, otherwise the commit the build was triggered on.
	 */
	private String fromCommit;

	/**
	 * Can be null, in case of pull request, indicates the TO commit.
	 */
	private String toCommit;

	/**
	 * Description of the build
	 */
	private String description;

	public String getQueryParams() {
		StringBuilder sb = new StringBuilder();
		sb.append("FROM_REF=").append(fromRef);
		sb.append("&FROM_COMMIT=").append(fromCommit);
		sb.append("&DESCRIPTION=").append(description);
		if (toRef != null) {
			sb.append("&TO_REF=").append(toRef);
			sb.append("&TO_COMMIT=").append(toCommit);

		}
		return sb.toString();
	}

	public String getFromRef() {
		return fromRef;
	}

	public String getToRef() {
		return toRef;
	}

	public String getFromCommit() {
		return fromCommit;
	}

	public String getToCommit() {
		return toCommit;
	}

	public String getDescription() {
		return description;
	}


}
