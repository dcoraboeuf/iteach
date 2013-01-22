var Lessons = function () {
	
	function deleteLesson (id) {
		application.confirmAndCall(
			loc('lesson.delete.prompt'),
			function () {
				$.ajax({
					type: 'DELETE',
					url: 'ui/teacher/lesson/{0}'.format(id),
					contentType: 'application/json',
					dataType: 'json',
					success: function (data) {
						if (data.success) {
							// Going back to the planning
							location = "gui/home";
						} else {
							application.displayError(loc('lesson.delete.error'));
						}
					},
					error: function (jqXHR, textStatus, errorThrown) {
					  	application.displayAjaxError (loc('lesson.delete.error'), jqXHR, textStatus, errorThrown);
					}
				});
			}
		);
	}
	
	function readDate () {
		var raw = $( "#lessonDate" ).datepicker("getDate");
		return application.formatDate(raw);
	}
	
	function lessonDialogInit () {
		$( "#lessonDate" ).attr("placeholder", i18n.dateCalendarFormat);
		$( "#lessonDate" ).datepicker( "destroy" );
		$( "#lessonDate" ).datepicker({
			showOtherMonths: true,
		    selectOtherMonths: true,
		    dateFormat: i18n.dateCalendarFormat
		});
		$( "#lessonDate" ).datepicker ("setDate", new Date ($( "#lessonDate" ).val()));
	}
	
	function createLesson (date, startTime, endTime, cancelFn, successFn) {
		application.dialog({
			id: 'lesson-dialog',
			title: loc('lesson.new'),
			width: 500,
			data: {
				lessonDate: date,
				lessonFrom: startTime,
				lessonTo: endTime,
				lessonStudent: '',
				lessonLocation: ''
			},
			submit: {
				name: loc('general.create'),
				action: function () {
					return submitCreateLesson(successFn);
				}
			},
			open: lessonDialogInit,
			cancel: cancelFn
		});		
	}
	
	function submitCreateLesson (successFn) {
		$.ajax({
			type: 'POST',
			url: 'ui/teacher/lesson',
			contentType: 'application/json',
			data: JSON.stringify({
				date: readDate($('#lessonDate').val()),
				from: $('#lessonFrom').val(),
				to: $('#lessonTo').val(),
				student: $('#lessonStudent').val(),
				location: $('#lessonLocation').val()
			}),
			dataType: 'json',
			success: function (data) {
				if (data.success) {
					successFn();
					$('#lesson-dialog').dialog('close');
				} else {
					application.displayError(loc('lesson.new.error'));
				}
			},
			error: function (jqXHR, textStatus, errorThrown) {
			  	if (jqXHR.responseText && jqXHR.responseText != '') {
			  		$('#lesson-dialog-error').html(jqXHR.responseText.htmlWithLines());
			  		$('#lesson-dialog-error').show();
			  	} else {
			  		application.displayAjaxError (loc('lesson.new.error'), jqXHR, textStatus, errorThrown);
			  	}
			}
		});
		return false;
	}
	
	function editLesson () {
		var id = $('#lesson-id').val();
		var student = $('#lesson-student').val();
		var date = $('#lesson-date').val();
		var startTime = $('#lesson-from').val();
		var endTime = $('#lesson-to').val();
		var location = $('#lesson-location').val();
		application.dialog({
			id: 'lesson-dialog',
			title: loc('lesson.edit'),
			width: 500,
			data: {
				lessonDate: date,
				lessonFrom: startTime,
				lessonTo: endTime,
				lessonStudent: student,
				lessonLocation: location
			},
			submit: {
				name: loc('general.update'),
				action: function () {
					return submitEditLesson (id);
				}
			},
			open: lessonDialogInit
		});		
	}
	
	function submitEditLesson (id) {
		$.ajax({
			type: 'PUT',
			url: 'ui/teacher/lesson/{0}'.format(id),
			contentType: 'application/json',
			data: JSON.stringify({
				date: readDate($('#lessonDate').val()),
				from: $('#lessonFrom').val(),
				to: $('#lessonTo').val(),
				student: $('#lessonStudent').val(),
				location: $('#lessonLocation').val()
			}),
			dataType: 'json',
			success: function (data) {
				if (data.success) {
					location.reload();
				} else {
					application.displayError(loc('lesson.edit.error'));
				}
			},
			error: function (jqXHR, textStatus, errorThrown) {
			  	if (jqXHR.responseText && jqXHR.responseText != '') {
			  		$('#lesson-dialog-error').html(jqXHR.responseText.htmlWithLines());
			  		$('#lesson-dialog-error').show();
			  	} else {
			  		application.displayAjaxError (loc('lesson.edit.error'), jqXHR, textStatus, errorThrown);
			  	}
			}
		});
		return false;
	}

	return {
		lessonDialogInit: lessonDialogInit,
		deleteLesson: deleteLesson,
		editLesson: editLesson,
		createLesson: createLesson,
		readDate: readDate
	};

} ();
