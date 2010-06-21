package playground.wrashid.lib;

import junit.framework.TestCase;
import playground.wrashid.lib.obj.Coord;
import playground.wrashid.lib.obj.CoordXComparable;

public class LinearFunctionTest extends TestCase{

	LinearFunction linFunc;
	
	public void testLinearFunctionPart(){
		LinearFunctionPart lfp=new LinearFunctionPart(new Coord(1, 1), new Coord(10,10));
		
		assertEquals(5.0, lfp.getYValue(5));	
		
		// provoke exception
		boolean exceptionOccured=false;
		try{
			// y-value is undefined for this x-value
			lfp.getYValue(11);	
		} catch (Exception e) {
			exceptionOccured=true;
		}
		assertEquals(true, exceptionOccured);	
	}
	
	private void setup(){
		linFunc=new LinearFunction();
		linFunc.defineFunctionPart(new LinearFunctionPart(new Coord(1, 1), new Coord(10,10)));
		linFunc.defineFunctionPart(new LinearFunctionPart(new Coord(10, 20), new Coord(20,20)));
	}
	
	
	public void testLinearFunction0(){
		setup();
		
		assertEquals(5.0, linFunc.getYValue(5));
		assertEquals(20.0, linFunc.getYValue(10));
		assertEquals(20.0, linFunc.getYValue(15));
	}
	
	public void testLinearFunction1(){
		setup();
		
		// provoke exception, by fetching undefined value (too low)
		boolean exceptionOccured=false;
		try{
			// y-value is undefined for this x-value
			linFunc.getYValue(0);	
		} catch (Exception e) {
			exceptionOccured=true;
		}
		assertEquals(true, exceptionOccured);
		
		// attention: don't add tests after the exception (internal data structure damaged)!!!!
	}
	
	public void testLinearFunction2(){
		setup();
		
		// provoke exception, by fetching undefined value (too high)
		boolean exceptionOccured=false;
		try{
			// y-value is undefined for this x-value
			linFunc.getYValue(21);	
		} catch (Exception e) {
			exceptionOccured=true;
		}
		assertEquals(true, exceptionOccured);
		
		// attention: don't add tests after the exception (internal data structure damaged)!!!!
	}
	
	public void testLinearFunction3(){
		setup();
		
		// provoke exception, by addition function part, which is overlapping
		boolean exceptionOccured=false;
		try{
			linFunc.defineFunctionPart(new LinearFunctionPart(new Coord(19, 11), new Coord(30,10)));	
		} catch (Exception e) {
			exceptionOccured=true;
		}
		assertEquals(true, exceptionOccured);
		
		// attention: don't add tests after the exception (internal data structure damaged)!!!!
	}
		
}
