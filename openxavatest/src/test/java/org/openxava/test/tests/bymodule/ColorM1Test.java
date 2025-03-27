package org.openxava.test.tests.bymodule;

import org.apache.commons.logging.*;
import org.openxava.tests.*;

/**
 * Create on 21/01/2009 (17:18:32)
 * @autor Ana Andrés
 */
public class ColorM1Test extends ModuleTestBase { 
	private static Log log = LogFactory.getLog(ColorM1Test.class);
	
	public ColorM1Test(String testName) {
		super(testName, "ColorM1");		
	}
	
	/*
	 * - module1
	 * 	. module2
	 * 		' module2Sub1
	 * 		' module2Sub2
	 */
	public void testReturnPreviousModule() throws Exception {		
		assertExists("property1");		
		execute("ColorM1.changeModule2");		
		assertExists("property2");		
		execute("ReturnPreviousModule.return");		
		assertExists("property1");		
		execute("ColorM1.changeModule2");		
		assertExists("property2");		
		execute("ColorM2.changeModule2Sub1");		
		assertExists("property2Sub1");		
		execute("ReturnPreviousModule.return");		
		assertExists("property2");		
		execute("ColorM2.changeModule2Sub1");	// the second time fails
		assertExists("property2Sub1");
		execute("ReturnPreviousModule.return");		
		assertExists("property2");	
		execute("ReturnPreviousModule.return");		
		assertExists("property1");
	}
	
	public void testReloadFromASubmodule() throws Exception { 		
		assertExists("property1");		
		execute("ColorM1.changeModule2");		
		assertExists("property2");		
		execute("ColorM2.changeModule2Sub1");		
		assertExists("property2Sub1");
		reload();				
		execute("ReturnPreviousModule.return");		
		assertExists("property2"); 		
		execute("ReturnPreviousModule.return");		
		assertExists("property1");		
	}
	
	
}
