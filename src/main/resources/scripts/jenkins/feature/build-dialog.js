define('trigger/build-dialog', [
    'aui',
    'jquery',
    'exports',
    'bitbucket/internal/model/page-state',
    'bitbucket/internal/util/ajax',
    'aui/flag'
], function(
    _aui,
    $,
    exports,
    pageState,
    ajax,
    flag
) {

	function getResourceUrl(resourceType){
		return _aui.contextPath() + '/rest/multi-branch/latest/projects/' + pageState.getProject().getKey() + '/repos/'
        + pageState.getRepository().getSlug() + '/' + resourceType;
	}
    
 	function bindToDropdownLink(linkSelector, dropDownSelector, getBuildInfoFunction) {
        //selecting on the document as the drop down is absolutely positioned and may not have a parent other than the document
        $(document).on('aui-dropdown2-show', dropDownSelector, function () {
        	var $dropdownMenu = $(this);
        	var $buildTriggerButton = $(linkSelector);
            var buildInfo = getBuildInfoFunction($dropdownMenu);
            var buildUrl = getResourceUrl("triggerBuild");

            var triggerBuildSetup = function() {
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
            };
            
            $buildTriggerButton.on('click', triggerBuildSetup);
            $dropdownMenu.on('aui-dropdown2-hide', function() {
            	$buildTriggerButton.off('click', triggerBuildSetup);
            });
            return false;
        });
    };
	
    exports.bindToDropdownLink = bindToDropdownLink;
});