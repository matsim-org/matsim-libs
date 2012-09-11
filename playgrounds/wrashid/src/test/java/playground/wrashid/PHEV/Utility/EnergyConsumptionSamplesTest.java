package playground.wrashid.PHEV.Utility;

import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumption;

import junit.framework.TestCase;

public class EnergyConsumptionSamplesTest extends TestCase {

	public void testGetInterpolatedEnergyConsumption() {
		EnergyConsumptionSamples ecs = new EnergyConsumptionSamples();
		ecs.add(new EnergyConsumption(20, 100));

		assertEquals(200.0, ecs.getInterpolatedEnergyConsumption(40, 1));

		ecs.add(new EnergyConsumption(40, 200));

		assertEquals(300.0, ecs.getInterpolatedEnergyConsumption(30, 2));

	}

	public void testGetInterpolatedValue() {

		assertEquals(125.0, EnergyConsumptionSamples.getInterpolatedValue(new EnergyConsumption(20, 100),
				new EnergyConsumption(30, 150), 25));
		assertEquals(60.0, EnergyConsumptionSamples.getInterpolatedValue(new EnergyConsumption(20, 100),
				new EnergyConsumption(30, 20), 25));
		assertEquals(91.0, EnergyConsumptionSamples.getInterpolatedValue(new EnergyConsumption(20, 100),
				new EnergyConsumption(30, 10), 21));

	} 
} 
