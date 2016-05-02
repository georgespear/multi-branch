define('jenkins/multi-branch-branchlist', [ 'jquery', 'trigger/build-dialog',
		'exports' ], function($, branchBuild, exports) {
	exports.onReady = function() {
		branchBuild.bindToDropdownLink('.multi-branch-branchlist',
				'.branch-list-action-dropdown', function(element) {
					return {
						fromRef : $(element).closest('[data-display-id]').attr(
								'data-display-id'),
						fromCommit : $(element).closest('[data-latest-commit]')
								.attr('data-latest-commit')
					};
				});
	};
});

AJS.$(document).ready(function() {
	require('jenkins/multi-branch-branchlist').onReady();
});