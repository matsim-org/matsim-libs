package org.matsim.contrib.carsharing.vehicles;
/** 
 * @author balac
 */
public class FFVehicleImpl implements FFVehicle{
	
	private String type;
	private String vehicleId;
	
	public FFVehicleImpl(String type, String vehicleId) {
		
		this.type = type;
		this.vehicleId = vehicleId;
	}
	public String getType() {
		return type;
	}
	
	@Override
	public String getVehicleId() {
		return vehicleId;
	}
	

	

}
