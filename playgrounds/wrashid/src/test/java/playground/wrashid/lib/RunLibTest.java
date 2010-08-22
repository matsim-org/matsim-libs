package playground.wrashid.lib;

import junit.framework.TestCase;

public class RunLibTest extends TestCase {

	public void testGetRunNumber(){
		assertEquals(2501,RunLib.getRunNumber(new Test2501()));
	}
	
	public void testRemoveNonNumericChars(){
		assertEquals("2501",RunLib.removeNonNumericChars("Test2501"));
	}
	
	private class Test2501{
		
	}
	
}
