package playground.wrashid.sschieffer.DecentralizedSmartCharger.ClassTests;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.sschieffer.SetUp.DomainFinder;

public class DomainFinderTest extends MatsimTestCase{

	
	public DomainFinderTest(){
		
	}
	
	
	public void testLinear(){
		PolynomialFunction func = new PolynomialFunction(new double[]{0, 1});
		
		DomainFinder finder= new DomainFinder(0, 10, func);
		
		double maxDomain= finder.getDomainMax();
		double minDomain= finder.getDomainMin();
		
		assertEquals(0.0, minDomain);
		assertEquals(10.0, maxDomain);
	}
	
	
	public void testQuadratic(){
		PolynomialFunction func = new PolynomialFunction(new double[]{0, 0, 1});
		
		DomainFinder finder= new DomainFinder(-5, 5, func);
		
		double maxDomain= finder.getDomainMax();
		double minDomain= finder.getDomainMin();
		
		assertEquals(0.0, minDomain);
		assertEquals(25.0, maxDomain);
	}
	
}
