package org.matsim.locationchoice.facilityload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.Initializer;
import org.matsim.testcases.MatsimTestCase;

public class FacilityPenaltyTest  extends MatsimTestCase {	
	private FacilityPenalty facilitypenalty = null;
	private Initializer initializer;
			
	public FacilityPenaltyTest() {
	}
	
	protected void setUp() throws Exception {
        super.setUp();
        this.initializer = new Initializer();
        this.initializer.init(this);    
    }
	
	protected void tearDown() throws Exception {
         super.tearDown();
         Gbl.reset();
    }
	
	public void testGetPenalty() {
		facilitypenalty = new FacilityPenalty(0.0, 1);
		assertEquals(facilitypenalty.getCapacityPenaltyFactor(0.0, 1.0), 0.0);
	}
	
	public void testcalculateCapPenaltyFactor() throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
	IllegalAccessException, InvocationTargetException {
		
		facilitypenalty = new FacilityPenalty(0.0, 1);
		
	    Method method = null;
		method = this.facilitypenalty.getClass().getDeclaredMethod("calculateCapPenaltyFactor", new Class[]{int.class, int.class});
		method.setAccessible(true);
		Double val = (Double)method.invoke(this.facilitypenalty, new Object[]{0, 1});
		assertTrue(Math.abs(val.doubleValue()) < 0.000000001);
	}
}