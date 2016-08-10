package org.matsim.contrib.carsharing.vehicles;
/** 
 * @author balac
 */
public class FFVehicleImpl implements FFVehicle{
	
	private String type;
	private String vehicleId;
	
	public FFVehicleImpl(String type, String vehicleId) {
		
		this.setType(type);
		this.setVehicleId(vehicleId);
	}
	@Override
	public String getType() {
		return type;
	}
	@Override
	public void setType(String type) {
		this.type = type;
	}
	@Override
	public String getVehicleId() {
		return vehicleId;
	}
	@Override
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	

}
