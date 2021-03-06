package com.spear.bitbucket.multibranch.helper;

import com.atlassian.bitbucket.event.hook.RepositoryHookEnabledEvent;
import com.atlassian.bitbucket.hook.repository.GetRepositoryHookSettingsRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHook;
import com.atlassian.bitbucket.hook.repository.RepositoryHookService;
import com.atlassian.bitbucket.hook.repository.RepositoryHookSettings;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.scm.http.HttpScmProtocol;
import com.atlassian.bitbucket.scm.ssh.SshScmProtocol;
import com.atlassian.bitbucket.scope.RepositoryScope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.event.api.EventListener;
import com.spear.bitbucket.multibranch.ciserver.Jenkins;
import com.spear.bitbucket.multibranch.ciserver.RepoSettings;

public class SettingsService {
    private static final String KEY = "com.spear.bitbucket.multi-branch:multi-branch-build-hook";
    public static final String BRANCH_REGEX = "branchRegex";
    public static final String JENKINS_PROJECT_NAME = "projectName";
    public static final String SKIP_PR_FROM_BRANCH_BUILD = "skipPRFromBranchBuild";

    private RepositoryHookService hookService;
    private SecurityService securityService;
    private final HttpScmProtocol httpScmProtocol;
    private final SshScmProtocol sshScmProtocol;
    private Jenkins jenkins;

    public SettingsService(RepositoryHookService hookService, SecurityService securityService, HttpScmProtocol httpScmProtocol,
        SshScmProtocol sshScmProtocol,
        Jenkins jenkins) {
        this.hookService = hookService;
        this.securityService = securityService;
        this.httpScmProtocol = httpScmProtocol;
        this.sshScmProtocol = sshScmProtocol;
        this.jenkins = jenkins;
    }

    public RepositoryHookSettings getSettings(final RepositoryScope repositoryScope) {

        RepositoryHookSettings settings = null;
        try {
            settings = securityService.withPermission(Permission.REPO_ADMIN, "Get respository settings")
                .call(new Operation<RepositoryHookSettings, Exception>() {
                    @Override
                    public RepositoryHookSettings perform() throws Exception {
                        return hookService.getSettings(new GetRepositoryHookSettingsRequest.Builder(repositoryScope, KEY).build());
                    }
                });
        } catch (Exception e) {
            return null;
        }

        return settings;
    }

    public RepositoryHook getHook(final RepositoryScope repositoryScope) {
        RepositoryHook hook = null;
        try {
            hook = securityService.withPermission(Permission.REPO_ADMIN, "Get respository settings")
                .call(new Operation<RepositoryHook, Exception>() {
                    @Override
                    public RepositoryHook perform() throws Exception {
                        return hookService.getByKey(repositoryScope, KEY);
                    }
                });
        } catch (Exception e1) {
            return null;
        }
        return hook;
    }

    public RepoSettings getRepoSettings(Settings settings) {
        if (settings == null) {
            return null;
        }
        return new RepoSettings(settings.getString(BRANCH_REGEX), settings.getString(JENKINS_PROJECT_NAME),
            settings.getBoolean(SKIP_PR_FROM_BRANCH_BUILD));
    }

    @EventListener
    public void onRepositoryHookEnabledEvent(RepositoryHookEnabledEvent event) {
        RepositoryScope repositoryScope = (RepositoryScope) event.getScope();
        String jenkinsProjectName = getRepoSettings(getSettings(repositoryScope).getSettings()).getJenkinsProjectName();
        // jenkins.generateMultiBranchJob(httpScmProtocol.getCloneUrl(repository, null), repository.getName(), jenkinsProjectName);
        jenkins.generateMultiBranchJob(sshScmProtocol.getCloneUrl(repositoryScope.getRepository(), null), repositoryScope.getRepository()
            .getName(), jenkinsProjectName);

        // ...
    }

}
