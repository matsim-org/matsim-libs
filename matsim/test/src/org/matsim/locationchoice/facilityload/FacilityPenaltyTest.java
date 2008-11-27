package org.matsim.locationchoice.facilityload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

public class FacilityPenaltyTest  extends MatsimTestCase {	
	private FacilityPenalty facilitypenalty = null;
	private Controler controler = null;
			
	public FacilityPenaltyTest() {
		this.initialize();
	}
	
	private void initialize() {
		Gbl.reset();
		String path = "test/input/org/matsim/locationchoice/config.xml";		
		String configpath[] = {path};
		controler = new Controler(configpath);
		controler.setOverwriteFiles(true);
		controler.run();		
	}
	
	public void testGetPenalty() {
		this.initialize();
		facilitypenalty = new FacilityPenalty(0.0, 1);
		assertEquals(facilitypenalty.getCapacityPenaltyFactor(0.0, 1.0), 0.0);
	}
	
	public void testcalculateCapPenaltyFactor() throws SecurityException, NoSuchMethodException, IllegalArgumentException, 
	IllegalAccessException, InvocationTargetException {
		this.initialize();
		facilitypenalty = new FacilityPenalty(0.0, 1);
		
	    Method method = null;
		method = this.facilitypenalty.getClass().getDeclaredMethod("calculateCapPenaltyFactor", new Class[]{int.class, int.class});
		method.setAccessible(true);
		Double val = (Double)method.invoke(this.facilitypenalty, new Object[]{0, 1});
		assertTrue(Math.abs(val.doubleValue()) < 0.000000001);
	}
}