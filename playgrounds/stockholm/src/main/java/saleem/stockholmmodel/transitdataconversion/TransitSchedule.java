package saleem.stockholmmodel.transitdataconversion;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents a Transit Schedule data structure used for writing Excel based transit data 
 * into MatSim form. One could also use the built in MatSim Transit Schedule.
 * 
 * @author Mohammad Saleem
 */

public class TransitSchedule {
	List<Stop> stops = new LinkedList<Stop>();
	List<VehicleType> vehicletypes = new LinkedList<VehicleType>();
	List<Vehicle> vehicles = new LinkedList<Vehicle>();
	List<Line> lines = new LinkedList<Line>();
	public void addStop(Stop stop){
		stops.add(stop);
	}
	public void addVehicleType(VehicleType vehicletype){
		vehicletypes.add(vehicletype);
	}
	public void addVehicle(Vehicle vehicle){
		vehicles.add(vehicle);
	}
	public void addLine(Line line){
		lines.add(line);
	}
	public List<Stop> getStops(){
		return stops;
	}
	public List<VehicleType> getVehicleTypes(){
		return vehicletypes;
	}
	public List<Vehicle> getVehicles(){
		return vehicles;
	}
	public Stop getStop(String id){//Returns a stop based on its ID
		Iterator iter = stops.iterator();
		while(iter.hasNext()){
			Stop stop = (Stop)iter.next();
			if(stop.getId().equals(id)){
				return stop;
			}
			
		}
		return null;
	}
	public void removeStop(Stop stop){//Removes a Stop
		stops.remove(stop);
	}
	public List<Line> getLines(){
		return lines;
	}
}
