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
    
	function getJobs(resourceUrl){
		var results;
		$.ajax({
		  type: "GET",
		  url: resourceUrl,
		  dataType: 'json',
		  async: false,
		  success: function (data){
			  results = data;
		  }
		});
		return results;
	}
	
	function triggerBuild(buildUrl){
		var successFlag = flag({
            type: 'success',
            body: 'Build started',
            close: 'auto'
        });
		ajax.rest({
		  type: "POST",
		  url: buildUrl,
		  dataType: 'json',
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
	};
	
    function getParameterArray(jobId){
    	var parameterArray = [];
		var params = jobs[jobId].parameters;
		for (var param in params){
			var val = params[param];
			parameterArray.push([param, val.split(";")]);
		}
		return parameterArray;
    };
    
	function SetupJobForm(element, jobId) {
		//Remove previous elements
		var $container = $('.job-params');
		$container.remove();
		var parameterArray = getParameterArray(jobId);
		var html = com.spear.bitbucket.multibranch.jenkins.branchBuild.createParams({
            'count' : parameterArray.length,
            'parameters' : parameterArray
        });
        $(html).insertAfter(element);
	};
	
	$(document).on('change', '#job', function(e) {
    	e.preventDefault();
    	SetupJobForm(this, this.value);
    });
	
    function showManualBuildDialog(buildUrl, jobIdArray) {
    	
        var dialog = _aui.dialog2(aui.dialog.dialog2({
            titleText: AJS.I18n.getText('Build with Parameters'),
            content: com.spear.bitbucket.multibranch.jenkins.branchBuild.buildDialog({
                jobs: jobIdArray
            }),
            footerActionContent: com.spear.bitbucket.multibranch.jenkins.branchBuild.buildButton(),
            removeOnHide: true
        })).show();
        
        var selects = document.getElementById("job");
        var selectedValue = selects.options[selects.selectedIndex].value;
        SetupJobForm(selects, selectedValue);
        
        dialog.$el.find('form').on('submit', function(e) { e.preventDefault(); });
        dialog.$el.find('#start-build').on('click', function() {
            _.defer(function() {
            	var $jobParameters = dialog.$el.find('.web-post-hook');
            	var jobSelect = document.getElementById("job");
                var id = selects.options[selects.selectedIndex].value;
            	buildUrl += "/" + jobs[id].id + "?";
            	$jobParameters.each(function(index, jobParam) {
            		var $curJobParam = $(jobParam);
            		var key = $curJobParam.find('label').text();
            		var value = dialog.$el.find('#build-param-value-' + index).val();
            		var type = $(dialog.$el.find('#build-param-value-' + index)[0]).attr('class');
            		if (type.indexOf("checkbox") > -1) {
            			value = dialog.$el.find('#build-param-value-' + index)[0].checked;
            		}
            		buildUrl += key + "=" + value + "&";
            	});
            	triggerBuild(buildUrl.slice(0,-1));
                dialog.hide();
            });
        }).focus().select();
    }
	
	$(".multi-branch-pullrequest").click(function() {
		var prJSON = require('bitbucket/internal/model/page-state').getPullRequest().toJSON();
		branchName = prJSON.fromRef.displayId;
		commit = prJSON.fromRef.latestCommit;
		var fromRefName = prJSON.fromRef.displayId;
		var toRefName = prJSON.toRef.displayId;
		var fromCommit = prJSON.fromRef.latestCommit;
		var toCommit = prJSON.toRef.latestCommit;

		var resourceUrl = getResourceUrl("getJobs");
    	
		jobs = getJobs(resourceUrl);
    	var jobArray = [];
    	for (var i in jobs){
    		jobs[i].parameters = [];
    		jobs[i].parameters["fromRef"] = fromRefName;
    		jobs[i].parameters["toRef"] = toRefName;
    		jobs[i].parameters["fromCommit"] = fromCommit;
    		jobs[i].parameters["toCommit"] = toCommit;
    	
    		jobArray.push(jobs[i]);
 	    }
    	

   		var buildUrl = getResourceUrl("triggerBuild");
       	showManualBuildDialog(buildUrl, jobArray);
    	
		return false;
	});

});

AJS.$(document).ready(function() {
    require('jenkins/multi-branch-pullrequest');
});