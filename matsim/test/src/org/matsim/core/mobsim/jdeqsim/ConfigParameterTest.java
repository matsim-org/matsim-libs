package org.matsim.core.mobsim.jdeqsim;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

public class ConfigParameterTest extends MatsimTestCase {

	public void testParametersSetCorrectly() {
		
		
		Config config = super.loadConfig(this.getPackageInputDirectory() + "config.xml");
//		config.controler().setLastIteration(-1); // we don't really want to run the simulation
		Controler controler = new Controler(config);
		
		controler.run();
		/* make sure, all simulation parameters are set properly from
		 * config xml file */

		assertEquals(360.0, SimulationParameters.getSimulationEndTime(), EPSILON);
		assertEquals(2.0, SimulationParameters.getFlowCapacityFactor(), EPSILON);
		assertEquals(3.0, SimulationParameters.getStorageCapacityFactor(), EPSILON);
		assertEquals(3600.0, SimulationParameters.getMinimumInFlowCapacity(), EPSILON);
		assertEquals(10.0, SimulationParameters.getCarSize(), EPSILON);
		assertEquals(20.0, SimulationParameters.getGapTravelSpeed(), EPSILON);
		assertEquals(9000.0, SimulationParameters.getSqueezeTime(), EPSILON);
	}
}
