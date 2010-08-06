package playground.wrashid.lib.obj;

import junit.framework.TestCase;


public class GenericResultTest extends TestCase {

	public void testBasic(){
		GenericResult gr=new GenericResult(0,1);
		
		assertEquals(2, gr.getResult().length);
		assertEquals(0, gr.getResult()[0]);
		assertEquals(1, gr.getResult()[1]);
	}
	
}
