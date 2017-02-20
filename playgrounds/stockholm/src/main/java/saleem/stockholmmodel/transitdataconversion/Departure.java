package saleem.stockholmmodel.transitdataconversion;

/** This class is used in converting Excel based data into MatSim based transit schedule 
 * data structure, consisting of neccessary attributes for stops and lines.
 * Departure class contain Departure attributes for route departures**/

public class Departure {
	private String id;
	private String departuretime;
	private String vehiclerefid;
	Vehicle vehicle;
	public void setDepartureTime(String departuretime){
		this.departuretime = departuretime;
	}
	public void setVehicleRefId(String vehiclerefid){
		this.vehiclerefid = vehiclerefid;
	}
	public void setVehicle(Vehicle vehicle){
		this.vehicle=vehicle;
	}
	public void setId(String id){
		this.id=id;
	}
	public String getDepartureTime(){
		return this.departuretime;
	}
	public String getId(){
		return this.id;
	}
	public Vehicle getVehicle(){
		return this.vehicle;
	}
	public String getVehicleRefId(){
		return this.vehiclerefid;
	}
}
