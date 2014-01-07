$(function() {
	$("form").submit(function(event) {
		event.preventDefault();
		$(".help-inline").empty();
		var request = $.post($(this).attr("action"), $(this).serialize());
		request.done(function(json) {
			alert("Success: " + json.token);
		});
		request.fail(function(response) {
			var json = response.responseJSON
			if (json.hasOwnProperty("errors")) {
				$.each(json.errors, function(i, error) {
					$("#" + error.field).next().text(error.message);
				});
			}
		});
	});
});
