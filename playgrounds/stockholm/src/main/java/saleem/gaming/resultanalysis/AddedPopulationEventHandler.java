package saleem.gaming.resultanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * A event handling class to help in doing basic plausibility check over the executed gaming simulation scenarios, 
 * before the results were provided to ProtoWorld to be used in interactive gaming.
 * 
 * @author Mohammad Saleem
 */
public class AddedPopulationEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, TransitDriverStartsEventHandler, PersonStuckEventHandler{
	private Set<Id<Person>> transitDrivers = new HashSet<>();
	private Map<Id<Person>, Double> times = new HashMap<>();
	private Map<Id<Person>, Double> timescar = new HashMap<>();
	private Map<Id<Person>, Double> timespt = new HashMap<>();
	double stuck = 0, totaltrips = 0, totaltime=0;
	double stuckcar = 0, tripscar = 0; double totaltimecar=0;
	double stuckpt = 0, tripspt = 0; double totaltimept=0;
	
	public AddedPopulationEventHandler(double totaltrips, double tripscar, double tripspt){//These values come from calculated modal split
		this.tripscar= tripscar;
		this.tripspt=tripspt;
		this.totaltrips=totaltrips;
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDrivers.add(event.getDriverId());
		// TODO Auto-generated method stub
		
	}
	/* event.getPersonId().toString().contains("a") is to ensure considering added population only, 
	 * has to be removed if want to consider everyone.(non-Javadoc)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(!transitDrivers.contains(event.getPersonId())){//Transit drivers are ignored as they are not passengers
			if(event.getPersonId().toString().contains("a")){
				totaltime += (event.getTime()-times.get(event.getPersonId()));
				times.remove(event.getPersonId());
				
			}
			if(event.getPersonId().toString().contains("a") && event.getLegMode() == "car"){
				totaltimecar += (event.getTime()-timescar.get(event.getPersonId()));
				timescar.remove(event.getPersonId());
			}
			if(event.getPersonId().toString().contains("a") && (event.getLegMode() == "pt" || event.getLegMode() == "transit_walk")){
				totaltimept += (event.getTime()-timespt.get(event.getPersonId()));
				timespt.remove(event.getPersonId());
			}
		}
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(!transitDrivers.contains(event.getPersonId())){
			if(event.getPersonId().toString().contains("a")){
				if(!times.containsKey(event.getPersonId())){
					times.put(event.getPersonId(), event.getTime());
				}
				
			}
			if(event.getPersonId().toString().contains("a") && event.getLegMode() == "car"){
				if(!timescar.containsKey(event.getPersonId())){
					timescar.put(event.getPersonId(), event.getTime());
				}
			}
			if(event.getPersonId().toString().contains("a") && (event.getLegMode() == "pt" || event.getLegMode() == "transit_walk")){
				if(!timespt.containsKey(event.getPersonId())){
					timespt.put(event.getPersonId(), event.getTime());
				}
			}
		
		}
		// TODO Auto-generated method stub
		
	}
	@Override
	public void handleEvent(PersonStuckEvent event) {
		if(!transitDrivers.contains(event.getPersonId())){
			if(event.getPersonId().toString().contains("a")){
				stuck++;
			}
			if(event.getPersonId().toString().contains("a") && (event.getLegMode() == "car")){
				stuckcar++;
			}
			if(event.getPersonId().toString().contains("a") && (event.getLegMode() == "pt" || event.getLegMode() == "transit_walk")){
				stuckpt++;
			}
		}
		// TODO Auto-generated method stub
		
	}
	public void printTripDurations(){
		
		double avgtripduration = totaltime/totaltrips;
		double avgtripdurationcar = totaltimecar/tripscar;
		double avgtripdurationpt = totaltimept/tripspt;

		System.out.println("Trips: " + totaltrips);
		System.out.println("Car trips: " + tripscar);
		System.out.println("PT trips: " + tripspt);
		
		System.out.println("Average trip duration of the added agents is: " + avgtripduration);
		System.out.println("Average car trip duration of the added agents is: " + avgtripdurationcar);
		System.out.println("Average pt trip duration of the added agents is: " + avgtripdurationpt);
	}
	public void printStuckAgentsInfo(){
		
		System.out.println("Average stuck agents: " + stuck);
		System.out.println("Average stuck car agents: " + stuckcar);
		System.out.println("Average pt stcuk agents: " + stuckpt);
	}

}
