package com.spear.bitbucket.multibranch;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsBetweenRequest;
import com.atlassian.bitbucket.hook.repository.AbstractRepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.PostRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PostRepositoryHookContext;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestSearchRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.pull.PullRequestState;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.setting.SettingsValidator;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.spear.bitbucket.multibranch.ciserver.BuildInfo;
import com.spear.bitbucket.multibranch.ciserver.GlobalSettings;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.ciserver.RepoSettings;
import com.spear.bitbucket.multibranch.helper.SettingsService;
import com.spear.bitbucket.multibranch.helper.Trigger;

public class ParameterizedBuildHook implements PostRepositoryHook<AbstractRepositoryHookRequest>, SettingsValidator {

    private static final String REFS_HEADS = "refs/heads/";
    private static final String REFS_TAGS = "refs/tags/";

    private final SettingsService settingsService;
    private final CommitService commitService;
    private final PullRequestService pullRequestService;
    private final Jenkins jenkins;
    private static final PageRequestImpl PAGE_REQUEST = new PageRequestImpl(0, 100);
    private static final String MAVEN_RELEASE_IGNORE_PATTERN = "[maven-release-plugin]";

    public ParameterizedBuildHook(SettingsService settingsService, CommitService commitService, Jenkins jenkins,
        PullRequestService pullRequestService) {
        this.settingsService = settingsService;
        this.commitService = commitService;
        this.jenkins = jenkins;
        this.pullRequestService = pullRequestService;
    }

    @Override
    public void postUpdate(PostRepositoryHookContext context, AbstractRepositoryHookRequest hookRequest) {
        Collection<RefChange> refChanges = hookRequest.getRefChanges();
        RepoSettings repoSettings = settingsService.getRepoSettings(context.getSettings());

        System.err.println("--------------------");
        System.err.println("Found " + refChanges.size() + " commits");
        for (RefChange refChange : refChanges) {
            System.err.println("Ref : " + refChange.getRef().getId());
            if (refChange.getRef().getId().startsWith(REFS_TAGS)) {
                System.err.println("Found tag... Ignoring");
                continue;
            }
            String branch = refChange.getRef().getId().replace(REFS_HEADS, "");
            String commit = refChange.getToHash();

            String jenkinsProjectName = repoSettings.getJenkinsProjectName();
            final CommitsBetweenRequest changesetsBetweenRequest = new CommitsBetweenRequest.Builder(hookRequest.getRepository())
                .include(refChange.getToHash())
                .exclude(refChange.getFromHash())
                .build();
            System.err.println("ToHash : " + refChange.getToHash());
            System.err.println("FromHash : " + refChange.getFromHash());
            switch (refChange.getType()) {
                case ADD:
                    System.err.println("Adding branch");
                    break;
                case DELETE:
                    System.err.println("Deleting branch");
                    break;
                case UPDATE:
                    System.err.println("Commits between: " + commitService.getCommitsBetween(changesetsBetweenRequest, PAGE_REQUEST).getSize());
                    break;
                default:
                    break;
            }

            System.err.println("Branch : " + branch);
            if (checkSkipPRFromBranchBuild(branch, repoSettings)) {
                System.err.println("Skipping branch, because pull request exists");
                continue;
            }
            Trigger trigger = buildBranchCheck(refChange, branch, repoSettings.getBranchRegex());
            System.err.println(trigger.name());
            if (refChange.getType() == RefChangeType.UPDATE
                && !checkCommitMessageValid(commitService.getCommitsBetween(changesetsBetweenRequest, PAGE_REQUEST)))
                continue;

            BuildInfo buildInfo = new BuildInfo(branch, null, commit, null, "Auto-triggered for branch " + branch, trigger);

            jenkins.triggerJob(buildInfo, jenkinsProjectName);
        }
        System.err.println("--------------------");
    }

    private boolean checkCommitMessageValid(Page<Commit> commits) {
        for (Commit changeset : commits.getValues()) {
            // System.err.println("Commit message : " + changeset.getme);
            if (changeset.getMessage() != null && changeset.getMessage().startsWith(MAVEN_RELEASE_IGNORE_PATTERN)) {
                return false;
            }
        }
        return true;
    }

    public Trigger buildBranchCheck(RefChange refChange, String branch, String branchCheck) {
        if (refChange.getType() == RefChangeType.UPDATE) {
            if ((branchCheck.isEmpty() || branch.toLowerCase().matches(branchCheck.toLowerCase()))) {
                return Trigger.PUSH;
            }
        } else if (refChange.getType() == RefChangeType.ADD) {
            if (branchCheck.isEmpty() || branch.toLowerCase().matches(branchCheck.toLowerCase())) {
                return Trigger.ADD;
            }
        } else if (refChange.getType() == RefChangeType.DELETE) {
            if (branchCheck.isEmpty() || branch.toLowerCase().matches(branchCheck.toLowerCase())) {
                return Trigger.DELETE;
            }
        }
        return Trigger.NULL;
    }

    @Override
    public void validate(Settings settings, SettingsValidationErrors errors, Scope scope) {

        GlobalSettings server = jenkins.getSettings();
        if (server == null || server.getBaseUrl().isEmpty()) {
            errors.addFieldError("jenkins-admin-error", "Jenkins is not setup in Bitbucket");
            return;
        }
        RepoSettings repoSettings = settingsService.getRepoSettings(settings);

        PatternSyntaxException branchExecption = null;
        try {
            Pattern.compile(repoSettings.getBranchRegex());
        } catch (PatternSyntaxException e) {
            branchExecption = e;
        }
        if (branchExecption != null) {
            errors.addFieldError(SettingsService.BRANCH_REGEX, branchExecption.getDescription());
        }

    }

    private boolean checkSkipPRFromBranchBuild(String branch, RepoSettings repoSettings) {

        Page<PullRequest> searchFrom =
            pullRequestService.search(new PullRequestSearchRequest.Builder().fromRefId(REFS_HEADS + branch).state(PullRequestState.OPEN)
                .build(), PAGE_REQUEST);

        if (repoSettings.isSkipPRFromBranchBuild() && searchFrom.getSize() > 0)
            return true;
        else
            return false;
    }

}
