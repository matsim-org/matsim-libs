package playground.dhosse.prt.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Vehicle;

import com.vividsolutions.jts.geom.Geometry;

public class PrtEventsHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	LinkEnterEventHandler {
	
	int counter = 0;
	int diff = 0;
	double dur = 0;
	double max = 0;
	double min = Double.MAX_VALUE;
	
	double durPt = 0;
	int cntPt = 0;
	double maxPt = 0;
	double minPt = Double.MAX_VALUE;
	
	int cntIv = 0;
	double durIv = 0;
	double maxIv = 0;
	double minIv = Double.MAX_VALUE;
	
	Map<Id<Person>, Double> persons = new HashMap<Id<Person>, Double>();
	Map<Id<Person>, Double[]> pt = new HashMap<Id<Person>, Double[]>();
	Map<Id<Person>, Double[]> iv = new HashMap<Id<Person>, Double[]>();
	
	Map<Id<Person>,List<Double>> travelTimesIV = new HashMap<Id<Person>,List<Double>>();
	Map<Id<Person>,List<Double>> travelTimesPt = new HashMap<Id<Person>,List<Double>>();
	List<Double> waitingTimesPt = new ArrayList<Double>();
	
	List<Id<Person>> personIds = new ArrayList<Id<Person>>();
	
	int passengerCounts = 0;
	
	List<Id<Vehicle>> vehicleIds;
	Geometry cottbus;
	Id<Person> maxId = null;
	
	public PrtEventsHandler(List<Id<Vehicle>> vehicles, Geometry bound){
		this.vehicleIds = vehicles;
		this.cottbus = bound;
	}
	
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!this.vehicleIds.contains(event.getPersonId())){
			if(event.getActType().equals("work")){
				if(event.getPersonId().toString().contains("prt")){
					this.pt.get(event.getPersonId())[1] = event.getTime();
				} else{
					this.iv.get(event.getPersonId())[1] = event.getTime();
				}
			} else if(event.getActType().equals("home")){
				if(event.getPersonId().toString().contains("prt")){
					this.pt.get(event.getPersonId())[3] = event.getTime();
				} else{
					this.iv.get(event.getPersonId())[3] = event.getTime();
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!this.vehicleIds.contains(event.getPersonId())){
			if(event.getActType().equals("home")){
				if(event.getPersonId().toString().contains("prt")){
					this.pt.put(event.getPersonId(), new Double[4]);
					this.pt.get(event.getPersonId())[0] = event.getTime();
				} else{
					this.iv.put(event.getPersonId(), new Double[4]);
					this.iv.get(event.getPersonId())[0] = event.getTime();
				}
			} else if(event.getActType().equals("work")){
				if(event.getPersonId().toString().contains("prt")){
					this.pt.get(event.getPersonId())[2] = event.getTime();
				} else{
					this.iv.get(event.getPersonId())[2] = event.getTime();
				}
			}
		}
	}

}
