package playground.wrashid.lib.obj.math;


import java.util.Collection;
import java.util.LinkedList;

import junit.framework.TestCase;


public class PolynomialTest extends TestCase{

	public void testBasic(){
		double[] coefficients=new double[3];
		
		coefficients[0]=1.0;
		coefficients[1]=1.0;
		coefficients[2]=5.0;
		
		Polynomial polynomial=new Polynomial(coefficients);
		
		assertEquals(131.0, polynomial.evaluate(5.0));
	}
	
}
