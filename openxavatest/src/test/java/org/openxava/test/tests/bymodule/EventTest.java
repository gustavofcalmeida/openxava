package org.openxava.test.tests.bymodule;

import org.openxava.tests.*;

/**
 * 
 * @author Chungyen Tsai
 */
public class EventTest extends ModuleTestBase {

	public EventTest(String testName) {
		super(testName, "Event");
	}
	
	public void testComboShowValueWhenHasCondition() throws Exception {
		assertValueInList(0, 6, "DEBT"); 
		execute("List.viewDetail", "row=0");
		assertValue("debtAtDate.id", "4028e4ab8a8966d7018a896a5bf80000");
	}
	
}
