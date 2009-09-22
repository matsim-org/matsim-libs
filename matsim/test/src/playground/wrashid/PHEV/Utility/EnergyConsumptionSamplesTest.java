package playground.wrashid.PHEV.Utility;

import playground.wrashid.PHEV.Utility.AverageSpeedEnergyConsumption;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamples;
import junit.framework.TestCase;

public class EnergyConsumptionSamplesTest extends TestCase {

	public void testGetInterpolatedEnergyConsumption() {
		EnergyConsumptionSamples ecs = new EnergyConsumptionSamples();
		ecs.add(new AverageSpeedEnergyConsumption(20, 100));

		assertEquals(200.0, ecs.getInterpolatedEnergyConsumption(40, 1));

		ecs.add(new AverageSpeedEnergyConsumption(40, 200));

		assertEquals(300.0, ecs.getInterpolatedEnergyConsumption(30, 2));

	}

	public void testGetInterpolatedValue() {

		assertEquals(125.0, EnergyConsumptionSamples.getInterpolatedValue(new AverageSpeedEnergyConsumption(20, 100),
				new AverageSpeedEnergyConsumption(30, 150), 25));
		assertEquals(60.0, EnergyConsumptionSamples.getInterpolatedValue(new AverageSpeedEnergyConsumption(20, 100),
				new AverageSpeedEnergyConsumption(30, 20), 25));
		assertEquals(91.0, EnergyConsumptionSamples.getInterpolatedValue(new AverageSpeedEnergyConsumption(20, 100),
				new AverageSpeedEnergyConsumption(30, 10), 21));

	} 
} 
