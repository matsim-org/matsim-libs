package playground.santiago.colectivos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

public class ColectivoModalShareEvaluator implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler{

	private final List<Integer> startTimes = new ArrayList<>();
	private final Map<String,Integer> numberOfRides = new HashMap<>();
	private int colectivo = 0;
//	private int pt = 0;
//	private int bike = 0;
//	private int other = 0;
//	private int car = 0;
//	private int train = 0;
//	private int walk = 0;
//	private int taxi = 0;
//	private int transit_walk = 0;
	
	public ColectivoModalShareEvaluator() {
	}

	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getVehicleId().toString().startsWith("co")){
			if(!event.getPersonId().toString().startsWith("pt")){
				colectivo ++;						
		}}
//		if (!event.getVehicleId().toString().startsWith("co")){
//			if(!event.getPersonId().toString().startsWith("pt")){
//				pt++;
//		}	}
//		numberOfRides.put("pt", pt);
		numberOfRides.put("colectivos", colectivo);
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
		

	
	public Map<String,Integer> getNumberofRides(){
		return numberOfRides;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		
	}

	
}