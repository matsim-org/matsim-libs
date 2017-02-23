package saleem.gaming.resultanalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.vehicles.Vehicle;

/**
 * A event handling class used to calculate accessibility measure for gaming scenarios, 
 * based on average all day utility of travellers performing an activity in,
 * or travelling through a certain area, with in a certain time window, using a certain mode.
 * 
 * @author Mohammad Saleem
 */
public class SpatialTemporalEventHandler implements BasicEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, 
	PersonDepartureEventHandler, PersonEntersVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler{
	
	private EventsToScore scoring;
	private Map<Id<Vehicle>,Id<Person>> vehicletoperson = new LinkedHashMap<Id<Vehicle>,Id<Person>>();
	private Map<Id<Person>,Double> persontoactstart = new LinkedHashMap<Id<Person>,Double>();
	private Set<Id<Person>> relevantpersons = new HashSet<>();

	private double fromtime=0;//Seconds passed from start of simulation
	private double totime=0;
	private String mode;//Mode can be "car", "pt" or "both"
	
	//Boundingbox around the centre of area of interest
	double xmin=0;
	double xmax=0;
	double ymin=0;
	double ymax=0;
	
	private Map<Id<Link>, ? extends Link> links;
	private Map<Id<Person>, ? extends Person> allpersons;
	
	public SpatialTemporalEventHandler(EventsToScore scoring, Scenario scenario, String mode, Coord origin, double gridsize, double fromtime, double totime){
		
		this.scoring=scoring;
		this.fromtime=fromtime;
		this.totime=totime;
		this.links=scenario.getNetwork().getLinks();
		this.allpersons=scenario.getPopulation().getPersons();
		this.mode = mode;
		
		//Boundingbox for the focussed area, A grid of gridsize X gridsize
		this.xmin=origin.getX()-gridsize;
		this.xmax=origin.getX()+gridsize;
		this.ymin=origin.getY()-gridsize;
		this.ymax=origin.getY()+gridsize;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		//If the event occurs with in the area of focus and with in the considered time
		double time = event.getTime();
		if(time >=fromtime && time<=totime && inBounds(links.get(event.getLinkId()).getCoord())){
			relevantpersons.add(vehicletoperson.get(event.getVehicleId()));
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		//If the event occurs with in the area of focus and with in the considered time
		double time = event.getTime();
		Id<Vehicle> vehid = event.getVehicleId();
		if(time>=fromtime && time<=totime && inBounds(links.get(event.getLinkId()).getCoord())){
			relevantpersons.add(vehicletoperson.get(vehid));
		}
		// TODO Auto-generated method stub
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		//If the event occurs with in the area of focus and with in the considered time
		double time = event.getTime();
		if(time>=fromtime && time<=totime && inBounds(links.get(event.getLinkId()).getCoord())){
			relevantpersons.add(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		//If the event occurs with in the area of focus and with in the considered time
		double time = event.getTime();
		if(time>=fromtime && time<=totime && inBounds(links.get(event.getLinkId()).getCoord())){
			relevantpersons.add(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {//Keep track of who is in which vehicle
		vehicletoperson.put(event.getVehicleId(), event.getPersonId());
		
	}
	
	public Set<Person> getRelevantPersons(){
		//Returns persosn travelling through or performing activity in the  focussed area in the considered time with the given mode.
		
		Set<Person> filteredpersons = new HashSet<>();//Further filtered by mode
		Iterator<Id<Person>> relpersonsiter = this.relevantpersons.iterator();
		while(relpersonsiter.hasNext()){
			Id<Person> personid = relpersonsiter.next();
			Person person = this.allpersons.get(personid);
			if(!personid.toString().startsWith("pt")){//Exclude transit drivers
				if(mode.equalsIgnoreCase("car")){
					if(PopulationUtils.hasCarLeg(person.getSelectedPlan())){
						filteredpersons.add(person);
					}
				}else if (mode.equalsIgnoreCase("pt")){
					if(!PopulationUtils.hasCarLeg(person.getSelectedPlan())){
						filteredpersons.add(person);
					}
				}else if(mode.equalsIgnoreCase("both")){
					filteredpersons.add(person);
				}
			}
		}
		System.out.println("The number of relevant persons is: " + filteredpersons.size());
		return filteredpersons;
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		double time = event.getTime();
		Id<Person> personid = event.getPersonId();
		//If the activity ends with in the area of focus but after the time of focus, check if it started before the considered time,
		//if yes, keep the person as he was performing activity with in the considered time. 
		if(time>totime && inBounds(links.get(event.getLinkId()).getCoord()) && persontoactstart.containsKey(personid)){
			relevantpersons.add(event.getPersonId());
		}
		//If the ends with in the area of focus and with in the time of focus
		else if(time>=fromtime && time<=totime && inBounds(links.get(event.getLinkId()).getCoord())){
			relevantpersons.add(personid);
		}		
		persontoactstart.remove(personid);
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		double time = event.getTime();
		Id<Person> personid = event.getPersonId();
		//If the activity starts with in the area of focus but before the time of focus, keep its track to see when it ends. 
		//If it ends after considered time, it means it was going on with in the considered time.  
		if(time<fromtime && inBounds(links.get(event.getLinkId()).getCoord())){
			persontoactstart.put(personid, time);
		}
		//If the activity starts with in the area of focus and with in the considered time
		else if(time>=fromtime && time<=totime && inBounds(links.get(event.getLinkId()).getCoord())){
			relevantpersons.add(personid);
		}	
	}

	@Override
	public void handleEvent(Event event) {
		scoring.handleEvent(event);

	}
	//Is the activity with the grid of ineterest?
	public Boolean inBounds(Coord coord){
		if(coord.getX()>=xmin && coord.getX()<=xmax && coord.getY()>=ymin && coord.getY()<=ymax){
				return true;
		}
		return false;
	}
}
