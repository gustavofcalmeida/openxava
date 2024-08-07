package org.openxava.test.tests.bymodule;

import org.openxava.tests.*;

/**
 * 
 * @author Javier Paniza
 */

public class ProductDefinitionTest extends ModuleTestBase {
	
	public ProductDefinitionTest(String testName) {
		super(testName, "ProductDefinition");		
	}
	
	public void testCompositeWithCascadeRemoveInsideCollectionWithCascadeRemove() throws Exception {
		execute("List.viewDetail", "row=0");
		assertValue("name", "LEGO CITY");
		assertCollectionRowCount("parts", 1);
		execute("Collection.edit", "row=0,viewObject=xava_view_parts");
		assertValue("name", "CAR");
		assertCollectionRowCount("subparts", 0);
		execute("Collection.new", "viewObject=xava_view_subparts");
		setValue("name", "WHEELS");
		execute("Collection.save");
		assertNoErrors();
		assertCollectionRowCount("subparts", 1);
		
		execute("Collection.deleteSelected", "row=0,viewObject=xava_view_subparts");
		assertCollectionRowCount("subparts", 0);
	}
	
		
}
