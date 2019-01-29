package org.matsim.contrib.carsharing.vehicles;
/** 
 * @author balac
 */
public class StationBasedVehicle implements CSVehicle{
	
	private String type;
	private String vehicleId;
	private String stationId;
	private String csType;
	private String companyId;

	public StationBasedVehicle(String vehicleType, String vehicleId, 
			String stationId, String csType, String companyId) {
		
		this.type = vehicleType;
		this.vehicleId = vehicleId;
		this.stationId = stationId;
		this.csType = csType;
		this.companyId = companyId;

	}	
	@Override
	public String getVehicleId() {
		return vehicleId;
	}
	
	public String getStationId() {
		return stationId;
	}
	@Override
	public String getType() {
		return type;
	}
	@Override
	public String getCsType() {
		return csType;
	}
	@Override
	public String getCompanyId() {
		return companyId;
	}	
	
}
