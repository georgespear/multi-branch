define('jenkins/multi-branch-pullrequest', [
  'aui',
  'jquery',
  'bitbucket/internal/model/page-state',
  'bitbucket/internal/util/ajax',
  'aui/flag'
], function(
   _aui,
   $,
   pageState,
   ajax,
   flag
) {
	var branchName;
	var commit;
	var jobs;
	
	function getResourceUrl(resourceType){
		return _aui.contextPath() + '/rest/multi-branch/latest/projects/' + pageState.getProject().getKey() + '/repos/'
        + pageState.getRepository().getSlug() + '/' + resourceType;
	}
    
	$(".multi-branch-pullrequest").click(function() {
		var prJSON = require('bitbucket/internal/model/page-state').getPullRequest().toJSON();
   		var buildUrl = getResourceUrl("triggerBuild");
		var buildInfo = {
				fromRef :  prJSON.fromRef.displayId,
				toRef :  prJSON.toRef.displayId,
				fromCommit :  prJSON.fromRef.latestCommit,
				toCommit : prJSON.toRef.latestCommit
		};
    	var successFlag = flag({
            type: 'success',
            body: 'Build started',
            close: 'auto'
        });
		ajax.rest({
		  type: "POST",
		  url: buildUrl,
		  dataType: 'json',
		  data : buildInfo,
		  async: true
		}).success(function (data) {
			var buildStatus = data.status;
    		var buildMessage = data.message;
    		if (buildStatus !== "201"){
    			successFlag.close();
    			flag({
                    type: 'warning',
                    body: buildMessage,
                    close: 'auto'
                });
    		}
		});
	});
	return false;

});

AJS.$(document).ready(function() {
    require('jenkins/multi-branch-pullrequest');
});