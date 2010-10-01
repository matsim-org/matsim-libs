package playground.wrashid.lib.obj.math;


import java.util.Collection;
import java.util.LinkedList;

import junit.framework.TestCase;


public class PolynomialTest extends TestCase{

	public void testBasic(){
		Polynomial polynomial=new Polynomial(1.0,1.0,5.0);
		
		assertEquals(131.0, polynomial.evaluate(5.0));
	}
	
}
