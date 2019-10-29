package playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;



public class RouteStopInfo {
	private String transportMode;
	private Id<Vehicle> vehicleId;
	private double depatureTime;
	private double arrivalTime;
	
	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public void setDepatureTime(double depatureTime) {
		this.depatureTime = depatureTime;
	}
	public void setTransportMode(String transportMode) {
		this.transportMode = transportMode;
	}
	public void setVehicleId(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}
	public double getArrivalTime() {
		return arrivalTime;
	}
	public double getDepatureTime() {
		return depatureTime;
	}
	public String getTransportMode() {
		return transportMode;
	}public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}
	@Override
	public String toString() {
		return "  ArrivalTime = "+this.getArrivalTime()+" DepatureTime = "+this.getDepatureTime()+" VehicleId = "+this.getVehicleId()+" TransportMode = "+this.getTransportMode();
	}
}
