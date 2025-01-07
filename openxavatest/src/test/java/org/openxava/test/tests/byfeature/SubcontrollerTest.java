package org.openxava.test.tests.byfeature;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * To test subcontroller related issues with Selenium.
 * 
 * @author Javier Paniza
 */
public class SubcontrollerTest extends WebDriverTestBase {
	
	public SubcontrollerTest(String testName) {
		super(testName);
	}
	
	public void testCharts() throws Exception {
		goModule("Invoice");
		assertInCorrectPlaceInButtonBar();
		
		goModule("Team");
		assertInCorrectPlaceInCollection();
	}
	
	private void assertInCorrectPlace(String openIconId, String buttonId, String popupId) throws Exception {
		execute("List.viewDetail", "row=0");
		WebElement openIcon = getDriver().findElement(By.id(openIconId));
		openIcon.click();
		WebElement button = getDriver().findElement(By.id(buttonId));
		WebElement popup = getDriver().findElement(By.id(popupId));
		
		// With some pixels of margin, not to test the exact position but that the popup is not in another part of the screen
		assertTrue(popup.getLocation().getX() > button.getLocation().getX() - 5 && popup.getLocation().getX() < button.getLocation().getX() + 5);
		assertTrue(popup.getLocation().getY() > button.getLocation().getY() + 20 && popup.getLocation().getY() < button.getLocation().getY() + 30);
	}
	
	private void assertInCorrectPlaceInButtonBar() throws Exception {
		assertInCorrectPlace(
			"ox_openxavatest_Invoice__sc-image-InvoicePrint_detail", 
			"ox_openxavatest_Invoice__sc-button-InvoicePrint_detail", 
			"ox_openxavatest_Invoice__sc-InvoicePrint_detail");
	}

	private void assertInCorrectPlaceInCollection() throws Exception {
		assertInCorrectPlace(
			"ox_openxavatest_Team__sc-image-TeamMemberSub_detail",
			"ox_openxavatest_Team__sc-button-TeamMemberSub_detail",
			"ox_openxavatest_Team__sc-TeamMemberSub_detail");
	}
	
}
