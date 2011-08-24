package playground.wrashid.artemis.lav;

import playground.wrashid.artemis.lav.EnergyConsumptionRegressionModel.EnergyConsumptionModelRow;
import playground.wrashid.lib.DebugLib;

public class VehicleSOC {

	public VehicleSOC(double socInJoule) {
		super();
		this.socInJoule = socInJoule;
	}

	private double socInJoule;
	private boolean didRunOutOfBattery=false;
	
	public void chargeVehicle(EnergyConsumptionModelRow energyConsumptionRegressionModel,double chargeInJoule){
		VehicleTypeLAV vehicleType = energyConsumptionRegressionModel.vehicleType;
		if (LAVLib.getBatteryElectricPowerTrainClass()!=vehicleType.powerTrainClass && LAVLib.getPHEVPowerTrainClass()!=vehicleType.powerTrainClass){
			DebugLib.stopSystemAndReportInconsistency("vehicleType.powerTrainClass:" + vehicleType.powerTrainClass);
		}
	
		socInJoule+=chargeInJoule;
		
		double batteryCapacity = energyConsumptionRegressionModel.getBatteryCapacityInJoule();
		if (socInJoule>batteryCapacity){
			DebugLib.stopSystemAndReportInconsistency("overcharging:" + socInJoule+ "/"+batteryCapacity);
		}
	}
	
	public void useBattery(double energyConsumptionInJoule){
		socInJoule-=energyConsumptionInJoule;
		
		if (socInJoule<0){
			didRunOutOfBattery=true;
		}
	}
	
	public boolean didRunOutOfBattery(){
		return didRunOutOfBattery;
	}
	
	public double getSocInJoule(){
		return socInJoule;
	}
	
	
	
}
