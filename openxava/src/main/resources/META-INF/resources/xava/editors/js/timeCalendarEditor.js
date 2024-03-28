// WARNING: IF YOU CHANGE THIS PASS DateCalendarTest.txt
if (timeCalendarEditor == null) var timeCalendarEditor = {};

openxava.addEditorInitFunction(function() {
    openxava.getScript(openxava.contextPath + "/xava/editors/flatpickr/" + openxava.language + ".js");
    if (openxava.browser.htmlUnit) return;

    timeCalendarEditor.onOpenDateTime;
    timeCalendarEditor.onChangeChecked = false;
    timeCalendarEditor.calendarOpen = false;
    timeCalendarEditor.focusTimeValue;

    //for fix chinese pop up AM/PM issue
    timeCalendarEditor.inputElementList = $('.xava_time > input').toArray();
    timeCalendarEditor.inputValueList = [];
    $('.xava_time > input').each(function() {
        var inputValue = $(this).val();
        timeCalendarEditor.inputValueList.push(inputValue);
    });
    timeCalendarEditor.isZh = false;

    $('.xava_time > input').on('focus', function() {
        timeCalendarEditor.focusTimeValue = $(this).val();
    });

    $('.xava_time').flatpickr({
        allowInput: true,
        clickOpens: false,
        wrap: true,
        locale: openxava.language,
        enableTime: true,
        noCalendar: true,
        onOpen: function(selectedDates, dateStr, instance) {
            timeCalendarEditor.calendarOpen = true;
            timeCalendarEditor.onOpenDateTime = dateStr;
            if (timeCalendarEditor.isZh && dateStr.includes('PM') && instance.amPM.innerHTML === 'AM') {
                instance.amPM.innerHTML = 'PM';
            }
        },
        onChange: function(selectedDates, dateStr, instance) {
            if (timeCalendarEditor.calendarOpen === true) {
                $(instance.input).data("changedCancelled", true);
            } else {
                if (timeCalendarEditor.focusTimeValue != dateStr) {
                    $(instance.input).removeData("changedCancelled");
                    $(instance.input).change();
                } else {
                    $(instance.input).data("changedCancelled", true);
                }
            }
        },
        onClose: function(selectedDates, dateStr, instance) {
            if (timeCalendarEditor.onOpenDateTime != dateStr) {
                $(instance.input).removeData("changedCancelled");
                $(instance.input).change();
            } else {
                $(instance.input).data("changedCancelled", true);
            }
            timeCalendarEditor.calendarOpen = false;
        },
        onReady: function(selectedDates, dateStr, instance) {
            if (openxava.language === 'zh') {
                timeCalendarEditor.isZh = true;
                for (var i = 0; i < timeCalendarEditor.inputElementList.length; i++) {
                    if (instance.input === timeCalendarEditor.inputElementList[i]) {
                        instance.input.value = timeCalendarEditor.inputValueList[i];
                    }
                }
            }
        }
    });

});