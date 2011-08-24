package playground.wrashid.artemis.lav;

import playground.wrashid.lib.DebugLib;

public class VehicleTypeLAV {

	public int powerTrainClass;
	public int fuelClass;
	public int powerClass;
	public int massClass;
	
	public boolean equals(VehicleTypeLAV otherVehicle){
		if (powerTrainClass!=otherVehicle.powerTrainClass){
			return false;
		}
		if (fuelClass!=otherVehicle.fuelClass){
			return false;
		}
		if (powerClass!=otherVehicle.powerClass){
			return false;
		}
		if (massClass!=otherVehicle.massClass){
			return false;
		}
		
		return true;
	}
	
	public void print(){
		System.out.println("pt:"+powerTrainClass+", fl:"+fuelClass+", pw:"+powerClass+", wt:"+massClass);
	}
	
	public void ifPHEVSwitchToElectricity(){
		if (powerTrainClass==LAVLib.getPHEVPowerTrainClass()){
			fuelClass=LAVLib.getElectricityFuelClass();
		} else {
			DebugLib.stopSystemAndReportInconsistency("powerTrainClass: " + powerTrainClass);
		}
	}
	
	public void ifPHEVSwitchToGasolineMode(){
		if (powerTrainClass==LAVLib.getPHEVPowerTrainClass()){
			fuelClass=LAVLib.getGasolineFuelClass();
		} else {
			DebugLib.stopSystemAndReportInconsistency("powerTrainClass: " + powerTrainClass);
		}
	}
}
