# multi-branch
Ths plugin has quite simple purpose - build your code in Jenkins as you push, create new branches, open or modify merge requests.
The builds are organized in folders, job per branch. Jobs are created automatically upon build creation and removed upon build deletion.

# Required Jenkins Plugins
1) https://wiki.jenkins-ci.org/display/JENKINS/Multi-Branch+Project+Plugin
2) Optional, but recommended https://wiki.jenkins-ci.org/display/JENKINS/StashNotifier+Plugin

#Setting up Jenkins template job.

For multi-repository builds, it's more convenient to setup a template job, which will be used for creating per-repository Jenkins folder jobs.
1) Create a new 'Freestyle multi-branch project'. You can as well use 'Maven multi-branch project' for Maven projects. Name it as you prefer, e.g. 'Template.Bitbucket'
2) In 'Source Code Management' block, choose Git, specifying $PROJECT_URL as 'Project Repository'. This placeholder will be replaced by actualy repository ssh url, when job is created out of template. Specify valid credentials.
3) In 'Per Branch Configuration' -> 'Project Options and Properties' tick 'This build is parameterized' and add 5 parameters of type String.
FROM_REF with default value ${GIT_BRANCH}
TO_REF with default value ${FROM_REF}
FROM_COMMIT with default value ${GIT_COMMIT}
TO_COMMIT with default value ${FROM_COMMIT}
DESCRIPTION. Default value if specified, will be used for the first build when a job per branch is created.
4) There is no need to modify 'Build Triggers' block, unless you want extra builds, e.g. based on schedule.
5) (Optional) In 'Pre Steps' add 'Set build description' providing ${DESCRIPTION} value in 'Description' field.
6) In 'Pre Steps' add 'Exexute shell' with the following script:
  %PATH_TO_GIT% checkout -f ${TO_REF}
  %PATH_TO_GIT% merge --ff ${FROM_COMMIT}
  
7) (Optional) In 'Post-build Actions' add 'Notify Stash Instance' (only if you installed the 'StashNotifier plugin'.
Provide correct base url to your bitbucket server, credentials and put the ${FROM_COMMIT} in the 'Commit SHA-1'. 

#Setting up Bitbucket Server
On the administration page of Bitbucket server, use the 'Jenkins Settings' link to provide the base URL of Jenkins and name of the template job, e.g. in our example - Template.Bitbucket
For each repository, that needs to be configured to use this plugin, go to repository settings, then Hooks, and enable the 'Jenkins multibranch build integration' plugin providing name of the Jenkins job and optionally regexp for filtering the branches for which the builds will be triggered. When you click 'Enable' button, the Jenkins job will be created, based on the template, specified above. NOTE: Every time you disable+enable the plugin for repository, the job creation attempt will be done.


That's it. Once the plugin has been configured, the following events in SCM will trigger Jenkins.
1) Branch created - creates a new job per branch.
2) Branch deleted - deletes the corresponding job.
3) Push to branch - triggers a build on a job, corresponding to this branch.
4) Pull request created, re-opened, source or target branch changed - triggers a build or a merged source on a job, configured for a source branch.
4) A buttons for manual triggering the build are added to branches and merge requests pages.

P.S.
The plugin's source code is heavily based on the https://github.com/KyleLNicholls/parameterized-builds. I used it as a template, iteratively removing the features i do not need, and adding new. 


