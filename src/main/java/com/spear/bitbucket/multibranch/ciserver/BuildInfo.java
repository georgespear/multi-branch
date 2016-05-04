package com.spear.bitbucket.multibranch.ciserver;

import com.spear.bitbucket.multibranch.helper.Trigger;

public class BuildInfo {

	public BuildInfo(String fromRef, String toRef, String fromCommit, String toCommit, String description, Trigger trigger) {
		this.fromRef = fromRef;
		this.toRef = toRef;
		this.fromCommit = fromCommit;
		this.toCommit = toCommit;
		this.description = description;
		this.trigger = trigger;
	}

	public BuildInfo() {
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

	private Trigger trigger;

	private String prTitle;
	private Long prId;

	public String getPrTitle() {
		return prTitle;
	}

	public void setPrTitle(String prTitle) {
		this.prTitle = prTitle;
	}

	public Long getPrId() {
		return prId;
	}

	public void setPrId(Long prId) {
		this.prId = prId;
	}

	public Trigger getTrigger() {
		return trigger;
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = trigger;
	}

	public String getQueryParams() {
		StringBuilder sb = new StringBuilder();
		sb.append("FROM_REF=").append(fromRef);
		sb.append("&FROM_COMMIT=").append(fromCommit);
		sb.append("&DESCRIPTION=").append(description);
		sb.append("&TRIGGER=").append(trigger.name());
		if (toRef != null) {
			sb.append("&TO_REF=").append(toRef);
			sb.append("&TO_COMMIT=").append(toCommit);

		}
		if (prId != null) {
			sb.append("&PR_ID=").append(prId);
			sb.append("&PR_TITLE=").append(prTitle);

		}

		return sb.toString();
	}

	public void setFromRef(String fromRef) {
		this.fromRef = fromRef;
	}

	public String getFromRef() {
		return fromRef;
	}

	public void setToRef(String toRef) {
		this.toRef = toRef;
	}

	public String getToRef() {
		return toRef;
	}

	public void setFromCommit(String fromCommit) {
		this.fromCommit = fromCommit;
	}

	public String getFromCommit() {
		return fromCommit;
	}

	public void setToCommit(String toCommit) {
		this.toCommit = toCommit;
	}

	public String getToCommit() {
		return toCommit;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
