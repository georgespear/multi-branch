define('jenkins/multi-branch-layout', [ 'bitbucket/internal/model/page-state',
		'trigger/build-dialog', 'exports' ], function(pageState, branchBuild,
		exports) {
	exports.onReady = function() {
		branchBuild.bindToDropdownLink('.multi-branch-layout',
				'#branch-actions-menu', function() {
					return {
						fromRef : pageState.getRevisionRef().getDisplayId(),
						fromCommit : pageState.getRevisionRef()
								.getLatestCommit()
					};
				});
	};
});

AJS.$(document).ready(function() {
	require('jenkins/multi-branch-layout').onReady();
});