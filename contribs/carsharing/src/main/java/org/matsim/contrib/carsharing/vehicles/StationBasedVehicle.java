package org.matsim.contrib.carsharing.vehicles;
/** 
 * @author balac
 */
public class StationBasedVehicle {
	
	private String type;
	private String vehicleId;
	private String stationId;
	
	public StationBasedVehicle(String type, String vehicleId, 
			String stationId) {
		
		this.type = type;
		this.vehicleId = vehicleId;
		this.stationId = stationId;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getVehicleId() {
		return vehicleId;
	}
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}
	public String getStationId() {
		return stationId;
	}
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}
}
