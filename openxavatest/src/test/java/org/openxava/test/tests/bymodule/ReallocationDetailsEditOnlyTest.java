package org.openxava.test.tests.bymodule;

import org.htmlunit.html.*;
import org.openxava.tests.*;


/**
 * 
 * @author Javier Paniza
 */

public class ReallocationDetailsEditOnlyTest extends ModuleTestBase {
	
	public ReallocationDetailsEditOnlyTest(String testName) {
		super(testName, "ReallocationDetailsEditOnly");		
	}	
	
	public void testEditOnlyElementCollections_booleanWithEditOnlyElementCollection() throws Exception {
		execute("List.viewDetail", "row=0");
		assertValue("description", "THE BIG REALLOCATION");
		assertNoErrors();
		
		assertCollectionRowCount("details", 3);
		execute("CRUD.save");
		execute("Mode.list");
		execute("List.viewDetail", "row=0");
		assertCollectionRowCount("details", 3);

		assertValueInCollection("details", 0, "place", "Valencia Office"); // In lowercase because of @Editor
		assertValueInCollection("details", 0, "product.number", "1");
		assertValueInCollection("details", 0, "product.description", "MULTAS DE TRAFICO");
		assertValueInCollection("details", 0, "product.unitPrice", "11.00");
		
		assertValueInCollection("details", 2, "place", "Casa En Michigan"); 
		assertValueInCollection("details", 2, "product.number", "3");
		assertValueInCollection("details", 2, "product.description", "XAVA"); 
		assertValueInCollection("details", 2, "product.unitPrice", "0.00");
		
		assertEditableInCollection("details", 0, "place");
		assertEditableInCollection("details", 0, "product.number");
		assertNoEditableInCollection("details", 0, "product.description");
		assertNoEditableInCollection("details", 0, "product.unitPrice");
		 
		assertEditableInCollection("details", 2, "place");
		assertEditableInCollection("details", 2, "product.number");
		assertNoEditableInCollection("details", 2, "product.description");
		assertNoEditableInCollection("details", 2, "product.unitPrice");
		
		HtmlTable table = (HtmlTable) getHtmlPage().getElementById("ox_openxavatest_ReallocationDetailsEditOnly__details");
		assertEquals(5, table.getRowCount());  
		String html = getHtml(); 
		assertFalse(html.contains("elementCollectionEditor.removeRow("));
		assertTrue(html.contains("Reference.search"));
	}
	
}
