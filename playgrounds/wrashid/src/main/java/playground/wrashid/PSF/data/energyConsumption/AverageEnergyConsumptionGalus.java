package playground.wrashid.PSF.data.energyConsumption;

import playground.wrashid.PHEV.Utility.AverageSpeedEnergyConsumption;
import playground.wrashid.PHEV.Utility.EnergyConsumptionSamples;

public class AverageEnergyConsumptionGalus implements AverageEnergyConsumptionBins {

	private EnergyConsumptionSamples ecs=new EnergyConsumptionSamples();
	
	public AverageEnergyConsumptionGalus() {
		// energy consumption for different speeds for driving one meter
		ecs.add(new AverageSpeedEnergyConsumption(5.555555556,3.173684E+02));
		ecs.add(new AverageSpeedEnergyConsumption(8.333333333,4.231656E+02));
		ecs.add(new AverageSpeedEnergyConsumption(11.11111111,5.549931E+02));
		ecs.add(new AverageSpeedEnergyConsumption(13.88888889,1.039878E+03));
		ecs.add(new AverageSpeedEnergyConsumption(16.66666667,4.056338E+02));
		ecs.add(new AverageSpeedEnergyConsumption(19.44444444,4.784535E+02));
		ecs.add(new AverageSpeedEnergyConsumption(22.22222222,5.580053E+02));
		ecs.add(new AverageSpeedEnergyConsumption(25,6.490326E+02));
		ecs.add(new AverageSpeedEnergyConsumption(27.77777778,7.502112E+02));
		ecs.add(new AverageSpeedEnergyConsumption(30.55555556,8.614505E+02));
		ecs.add(new AverageSpeedEnergyConsumption(33.33333333,1.179291E+03));
		ecs.add(new AverageSpeedEnergyConsumption(36.11111111,1.825931E+03));
		ecs.add(new AverageSpeedEnergyConsumption(38.88888889,2.418100E+03));
		ecs.add(new AverageSpeedEnergyConsumption(41.66666667,2.905639E+03));				
	}
	
	
	public double getEnergyConsumption(double speed, double distance) {
		return ecs.getInterpolatedEnergyConsumption(speed, distance);
	}

}
