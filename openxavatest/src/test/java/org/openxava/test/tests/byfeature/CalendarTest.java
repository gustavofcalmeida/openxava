package org.openxava.test.tests.byfeature;

import java.text.*;
import java.time.*;
import java.util.*;

import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.*;
import org.openqa.selenium.support.ui.*;

/**
 * To test calendar view with Selenium.
 * 
 * @author Chungyen Tsai
 */
public class CalendarTest extends WebDriverTestBase {
	
    public void testCalendar() throws Exception {
        assertCreateEventPrevCurrentNextMonth_conditionsAndFilter(); 
        assertMultipleDatesPropertiesAndFirstDateAsEventStart();
        assertFilterPerformance();
        assertCreateDateWithTimeInWeekAndDailyView_tooltip();
        assertAnyNameAsDateProperty();
        assertNavigationInDateCalendarAndDateTimeCalendar();
    }    

	private void nextOnCalendar() throws Exception {
		String s = getDriver().findElement(By.cssSelector(".fc-toolbar-title")).getText();
		WebElement next = getDriver().findElement(By.cssSelector(".fc-icon.fc-icon-chevron-right"));
		next.click();
		waitCalendarEvent(getDriver());
		if (s.equals(getDriver().findElement(By.cssSelector(".fc-toolbar-title")).getText())) {
			nextOnCalendar();
		}
	}

	private void prevOnCalendar() throws Exception {
		String s = getDriver().findElement(By.cssSelector(".fc-toolbar-title")).getText();
		WebElement prev = getDriver().findElement(By.cssSelector(".fc-icon.fc-icon-chevron-left"));
		prev.click();
		waitCalendarEvent(getDriver());
		if (s.equals(getDriver().findElement(By.cssSelector(".fc-toolbar-title")).getText())) {
			prevOnCalendar();
		}
	}

	private void assertNavigationInDateCalendarAndDateTimeCalendar() throws Exception {
		goModule("Appointment");
		moveToCalendarView(getDriver());
		moveToTimeGridWeek(getDriver());
		goModule("Appointment2");
		moveToCalendarView(getDriver());
		moveToTimeGridWeek(getDriver());
		moveToListView();
		goModule("Event");
		moveToCalendarView(getDriver());
		boolean weekButton = getDriver().findElements(By.cssSelector("button.fc-timeGridWeek-button")).isEmpty();
		assertTrue(weekButton);
		boolean dayButton = getDriver().findElements(By.cssSelector("button.fc-timeGridDay-button")).isEmpty();
		assertTrue(dayButton);
		moveToListView();
	}

	private void assertFilterPerformance() throws Exception {
		// testing default filter defined by user and month filter defined by calendar
		goModule("EventWithFilter");
		moveToListView();
		long ini = System.currentTimeMillis();
		moveToCalendarView(getDriver());
		long takes = System.currentTimeMillis() - ini;
		// with CompositeFilter it takes no more than 1500, without it takes more than
		// 4000
		assertTrue(takes < 3000);
		moveToListView();
	}

	private void assertCreateEventPrevCurrentNextMonth_conditionsAndFilter() throws Exception {
		goModule("Invoice");
		moveToCalendarView(getDriver());
		prevOnCalendar();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		List<Date> dates = setDates();
		for (int i = 0; i < dates.size(); i++) {
			if (i == 2) {
				nextOnCalendar();
				wait(getDriver());
			}
			String dateString = dateFormat.format(dates.get(i));
			WebElement day = getDriver()
					.findElement(By.xpath("//div[contains(@class,'fc-daygrid-day-frame') and ancestor::td[@data-date='"
							+ dateString + "']]"));
			day.click();
			wait(getDriver());
			createInvoice(i);
		}
		prevOnCalendar();
		verifyPrevInvoiceEvent();
		moveToListView();
		
		//conditions and filter
		goModule("InvoiceCalendar");
		setConditionValue("12", 1);
		setConditionComparator("=", 1);
		wait(getDriver());
		moveToCalendarView(getDriver());
		prevOnCalendar();
		verifyConditionEvents("past", false);
		nextOnCalendar();
		nextOnCalendar();
		verifyConditionEvents("future", true);
		moveToListView();
		clearListCondition();
		for (int i = 0; i < 3; i++) {
			execute("CRUD.deleteRow", "row=0");
		}
		Thread.sleep(300);
	}

	private void assertAnyNameAsDateProperty() throws Exception {
		goModule("UserWithBirthday");
		try {
			execute("Mode.list");
		} catch (NoSuchElementException e) {
		}
		moveToListView();
		moveToCalendarView(getDriver());
		moveToListView();
	}

	private void assertMultipleDatesPropertiesAndFirstDateAsEventStart() throws Exception {
		goModule("Event");
		moveToCalendarView(getDriver());
		for (int i = 0; i < 6; i++) {
			createEvents(i);
		}
		moveToListView();
		setConditionValue("TEST", 3);
		setConditionComparator("=", 3);
		for (int i = 0; i < 6; i++) {
			execute("CRUD.deleteRow", "row=0");
		}
		clearListCondition();
	}

	private void assertCreateDateWithTimeInWeekAndDailyView_tooltip() throws Exception {
		goModule("Appointment");
		moveToCalendarView(getDriver());
		moveToTimeGridWeek(getDriver());

	    while (true) {
	        String prevUrl = getDriver().getCurrentUrl();
	        WebElement dayTimeCell = getDriver().findElement(By.cssSelector("tr:nth-child(6) > .fc-timegrid-slot-lane"));
	        dayTimeCell.click();
	        wait(getDriver());

	        if (!prevUrl.equals(getDriver().getCurrentUrl())) {
	            break;
	        } else {
	            refreshCalendarView(getDriver());
	            moveToTimeGridWeek(getDriver());
	        }
	    }
		
		WebElement dateTime = getDriver().findElement(By.id("ox_openxavatest_Appointment__time"));
		String dateTimeInput = dateTime.getAttribute("value");
		assertTrue(dateTimeInput.contains("2:30"));
		setValue("description", "A");
		execute("CRUD.save");
		execute("Mode.list");
		waitCalendarEvent(getDriver());

		// tooltip
		refreshCalendarView(getDriver());
		WebElement event = getDriver().findElement(By.cssSelector(".fc-event-time"));
		Actions builder = new Actions(getDriver());
		builder.moveToElement(event).perform();
		Thread.sleep(500);
		WebElement tooltip = getDriver().findElement(By.cssSelector(".fc-event-tooltip"));
		assertEquals("A", tooltip.getText());

		moveToTimeGridWeek(getDriver());
		event = getDriver().findElement(By.cssSelector(".fc-event-time"));
		event.click();
		wait(getDriver());
		dateTime = getDriver().findElement(By.id("ox_openxavatest_Appointment__time"));
		dateTimeInput = dateTime.getAttribute("value");
		assertTrue(dateTimeInput.contains("2:30"));
		execute("CRUD.delete");
		execute("Mode.list");
		waitCalendarEvent(getDriver());
		moveToListView();
		acceptInDialogJS(getDriver());
	}

	private List<Date> setDates() {
		List<Date> dates = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date currentMonth = calendar.getTime();
		calendar.add(Calendar.MONTH, -1);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date previousMonth = calendar.getTime();
		calendar.add(Calendar.MONTH, 2);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date nextMonth = calendar.getTime();
		dates.add(previousMonth);
		dates.add(currentMonth);
		dates.add(nextMonth);

		return dates;
	}

	private void createInvoice(int invoiceNumber) throws Exception {
		setValue("number", String.valueOf(10 + invoiceNumber));
		setValue("customer.number", "1");
		execute("Sections.change", "activeSection=2");
		setValue("vatPercentage", "3");
		execute("CRUD.save");
		execute("Mode.list");
		waitCalendarEvent(getDriver());
	}

	private void verifyPrevInvoiceEvent() throws Exception {
		WebElement currentMonthEvent = getDriver().findElement(By.cssSelector(
				"a.fc-event.fc-event-draggable.fc-event-resizable.fc-event-start.fc-event-end.fc-event-past.fc-daygrid-event.fc-daygrid-dot-event"));
		currentMonthEvent.click();
		wait(getDriver());
		assertEquals("10", getValue("number"));
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		assertEquals(String.valueOf(year), getValue("year"));
		execute("Mode.list");
		waitCalendarEvent(getDriver());
	}

	private void verifyConditionEvents(String time, boolean isExist) {
		WebElement currentMonthEvent = null;
		try {
			currentMonthEvent = getDriver().findElement(By.cssSelector(
					"a.fc-event.fc-event-draggable.fc-event-resizable.fc-event-start.fc-event-end.fc-event-" + time
							+ ".fc-daygrid-event.fc-daygrid-dot-event"));
		} catch (NoSuchElementException e) {
		}
		assert isExist ? currentMonthEvent != null : currentMonthEvent == null;

	}

	private void createEvents(int i) throws Exception {
		List<Date> dates = setDates();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = dateFormat.format(dates.get(1));
		WebElement day;
		day = getDriver().findElement(By.xpath("//td[@data-date='" + dateString + "']//a[@class='fc-daygrid-day-number']"));
		day.click();
		wait(getDriver());
		Thread.sleep(500);
		List<WebElement> days = getDriver().findElements(By.xpath("//td[@data-date='" + dateString + "']//a[@class='fc-daygrid-day-number']"));
		if (days.size() > 0) {
			//sometimes test not work here
			refreshCalendarView(getDriver());
			day = getDriver().findElement(By.xpath("//td[@data-date='" + dateString + "']//a[@class='fc-daygrid-day-number']"));
			day.click();
			wait(getDriver());
		}
		
		WebElement startDate = getDriver().findElement(By.id("ox_openxavatest_Event__startDate"));
		blur(startDate);
		Thread.sleep(500); // needed after blur
		// selecting with pop up calendar in case that browser is not in english
		List<WebElement> iconElements = getDriver().findElements(By.cssSelector("i.mdi.mdi-calendar"));
		if (!iconElements.isEmpty()) {
			WebElement firstIconElement = iconElements.get(1);
			firstIconElement.click();
		}
		List<WebElement> spanElements = getDriver().findElements(By.xpath("//div[@class='dayContainer']//span[@class='flatpickr-day ' and text()='2']"));
		if (!spanElements.isEmpty()) {
			WebElement spanElement = spanElements.get(1);
			spanElement.click();
		}
		wait(getDriver());
		setValue("name", "TEST");
		execute("CRUD.save");
		execute("Mode.list");
		waitCalendarEvent(getDriver());
		if (i == 0) {
			refreshCalendarView(getDriver());
			WebElement aElement = getDriver().findElement(By.xpath("//td[@data-date='" + dateString + "']//div[contains(@class,'fc-event-title')]"));
			assertEquals("TEST", aElement.getText());
		} else if (i == 5) {
			WebElement linkElement = getDriver().findElement(By.cssSelector("a.fc-daygrid-more-link.fc-more-link"));
			assertNotNull(linkElement);
		}
	}

	private void waitCalendarEvent(WebDriver driver) throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(10000));
		try {
			wait.until(ExpectedConditions.jsReturnsValue("return calendarEditor.requesting === false;"));
			Thread.sleep(500);
		} catch (Exception ex) {
		}
	}

	private void moveToCalendarView(WebDriver driver) throws Exception {
		WebElement tabCalendar = driver.findElement(By.cssSelector(".mdi.mdi-calendar"));
		WebElement tabCalendarParent = tabCalendar.findElement(By.xpath(".."));
		String title = tabCalendarParent.getAttribute("class");
		if (!(title != null && title.contains("ox-selected-list-format"))) {
			tabCalendar.click();
		}
		
		//sometimes errors occur in selenium when changing views, 
		//so we handle the alert and load the view again
		boolean b = false;
		try {
			Alert alert = driver.switchTo().alert();
			alert.accept();
			alert.dismiss();
			wait(driver);
			b = true;
		} catch(NoAlertPresentException e) {
		}
		if (b) {
			moveToListView();
			moveToCalendarView(driver);
		}
		waitCalendarEvent(driver);
		wait(driver);
	}

	private void moveToTimeGridWeek(WebDriver driver) throws Exception {
		WebElement weekButton = driver.findElement(By.cssSelector("button.fc-timeGridWeek-button"));
		weekButton.click();
		waitCalendarEvent(driver);
	}

	private void refreshCalendarView(WebDriver driver) throws Exception {
		//sometimes need when changing view so fast
		driver.navigate().refresh();
		acceptInDialogJS(driver);
		waitCalendarEvent(driver);
	}
}