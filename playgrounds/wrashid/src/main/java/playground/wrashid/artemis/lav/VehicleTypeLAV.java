package playground.wrashid.artemis.lav;

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
	
}
