package playground.wrashid.PSF.data.energyConsumption;

import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumption;

import playground.wrashid.PHEV.Utility.EnergyConsumptionSamples;


public class AverageEnergyConsumptionGalus implements AverageEnergyConsumptionBins {

	private EnergyConsumptionSamples ecs=new EnergyConsumptionSamples();
	
	public AverageEnergyConsumptionGalus() {
		// energy consumption for different speeds for driving one meter in joule
		ecs.add(new EnergyConsumption(5.555555556,3.173684E+02));
		ecs.add(new EnergyConsumption(8.333333333,4.231656E+02));
		ecs.add(new EnergyConsumption(11.11111111,5.549931E+02));
		ecs.add(new EnergyConsumption(13.88888889,1.039878E+03));
		ecs.add(new EnergyConsumption(16.66666667,4.056338E+02));
		ecs.add(new EnergyConsumption(19.44444444,4.784535E+02));
		ecs.add(new EnergyConsumption(22.22222222,5.580053E+02));
		ecs.add(new EnergyConsumption(25,6.490326E+02));
		ecs.add(new EnergyConsumption(27.77777778,7.502112E+02));
		ecs.add(new EnergyConsumption(30.55555556,8.614505E+02));
		ecs.add(new EnergyConsumption(33.33333333,1.179291E+03));
		ecs.add(new EnergyConsumption(36.11111111,1.825931E+03));
		ecs.add(new EnergyConsumption(38.88888889,2.418100E+03));
		ecs.add(new EnergyConsumption(41.66666667,2.905639E+03));				
	}
	
	
	public double getEnergyConsumption(double speedInMetersPerSecond, double distance) {
		return ecs.getInterpolatedEnergyConsumption(speedInMetersPerSecond, distance);
	}

}
