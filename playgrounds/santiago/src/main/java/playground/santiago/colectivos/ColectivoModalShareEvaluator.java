package playground.santiago.colectivos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class ColectivoModalShareEvaluator implements PersonEntersVehicleEventHandler, ActivityStartEventHandler{

	private final List<Id<Person>> transitDriverPersons = new ArrayList<>();
	private final ArrayList<Id<Vehicle>> Vehicles = new ArrayList<>();
	private final Map<String,Integer> numberOfRides = new HashMap<>();
	private final List<Id<Person>> usersInTransit = new ArrayList<>();
	private int colectivo = 0;
	private int pt = 0;
//	private boolean inTransit = true;
//	private int bike = 0;
//	private int other = 0;
//	private int car = 0;
//	private int train = 0;
//	private int walk = 0;
//	private int taxi = 0;
//	private int transit_walk = 0;

	
//	public ColectivoModalShareEvaluator() {
//	}
//	public void handleEvent(TransitDriverStartsEvent event) {
//		transitDriverPersons.add(event.getDriverId());
//	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if( !usersInTransit.contains(event.getPersonId())) {
			
			if (event.getVehicleId().toString().startsWith("co")){
				if(!event.getPersonId().toString().startsWith("pt")){
					colectivo ++;
					Vehicles.add(event.getVehicleId());
					usersInTransit.add(event.getPersonId());
				}
			}
		}
	}
	
	@Override
	public void handleEvent (ActivityStartEvent event){
		if (!event.getActType().equals("pt interaction")){
			usersInTransit.remove(event.getPersonId());
		}
	}
//	public void handleEvent (PersonDepartureEvent event){
//		if(event.getLegMode().toString().equals("bike")){
//			bike++;
//		}
//		if(event.getLegMode().toString().equals("other")){
//			other++;
//		}
//		if(event.getLegMode().toString().equals("train")){
//			train++;
//		}
//		if(event.getLegMode().toString().equals("walk")){
//			walk++;
//		}
//		if(event.getLegMode().toString().equals("taxi")){
//			taxi++;
//		}
//		if(event.getLegMode().toString().equals("taxi")){
//			car++;
//		}
//		if(event.getLegMode().toString().equals("transit_walk")){
//			transit_walk++;
//		}
//		
//		numberOfRides.put("Bike", bike);
//		numberOfRides.put("Other", other);
//		numberOfRides.put("Train", train);
//		numberOfRides.put("Walk", walk);
//		numberOfRides.put("Taxi", taxi);
//		numberOfRides.put("Car über departures", car);
//		numberOfRides.put("Transit_walk", transit_walk);
//	}
	

	
//	public Map<String,Integer> getNumberofRides(){
//		return numberOfRides;
//	}
	
	public ArrayList<Id<Vehicle>> getVehicles(){
		return Vehicles;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public int getColectivo() {
		System.out.println("Number of colectivo trips: " + colectivo);
		return colectivo;
	}

	
}