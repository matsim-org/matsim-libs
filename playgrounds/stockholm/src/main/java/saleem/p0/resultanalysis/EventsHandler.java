package saleem.p0.resultanalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;
/**
 * An event handling class to help collect statitics about delays, 
 * number of agents on incoming links of vehicles on junctions, 
 * number of vehicles passing through junctions etc.
 * For comparisons between policies.
 * 
 * @author Mohammad Saleem
 */
public class EventsHandler implements BasicEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler, VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler, PersonStuckEventHandler{
	List<String> inlinks;
	double delay = 0;
	double time = -3600;
	int numveh = 0;
	Network network;
	Map<Id<Vehicle>, Double> entries = new LinkedHashMap<Id<Vehicle>, Double>();//Entry time against vehicle to incoming links for delay calculation
	Map<Id<Person>, Id<Vehicle>> persontoveh = new LinkedHashMap<Id<Person>, Id<Vehicle>>();//Entry time against vehicle to incoming links for delay calculation
	Map<String, Double> freetraveltimes = new LinkedHashMap<String, Double>();//Free speed against link id in String form
	ArrayList<Double> times = new ArrayList<Double>();//Time entries for plotting
	ArrayList<Double> delays = new ArrayList<Double>();//Delays for plotting
	ArrayList<Double> numagents = new ArrayList<Double>();//Number of agents moving through pretimed intersections for plotting
	ArrayList<Double> numonlinks = new ArrayList<Double>();//Number of agents on incoming links
	public EventsHandler(List<String> inlinks, Network network) {
		this.inlinks=inlinks;
		this.network=network;
		getFreeSpeedsOfLinks();
		// TODO Auto-generated constructor stub
	}
	public void getFreeSpeedsOfLinks(){
		Map<Id<Link>, ? extends Link> links = this.network.getLinks();
		Iterator<Id<Link>> iterator = links.keySet().iterator();
		while(iterator.hasNext()){
			Link link = links.get(iterator.next());
			double freetraveltime = Math.ceil(link.getLength()/link.getFreespeed());
			freetraveltimes.put(link.getId().toString(),freetraveltime );
		}
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if(inlinks.contains(event.getLinkId().toString())){
			if(entries.get(event.getVehicleId())!=null){
				double del = event.getTime()-entries.get(event.getVehicleId())
						- freetraveltimes.get(event.getLinkId().toString());
				if(del<10000){
					delay = delay + del;
					numveh++;
				}
				entries.remove(event.getVehicleId());
			}
		}
		
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {//Record entries against vehicles
		if(inlinks.contains(event.getLinkId().toString())){
			entries.put(event.getVehicleId(), event.getTime());
		}
		
	}
	public ArrayList<Double> getDelays(){
		return this.delays;
	}
	public ArrayList<Double> getAgentsOnIncomingLinks(){
		return this.numonlinks;
	}
	public ArrayList<Double> getNumAgentsThroughIntersections(){
		return this.numagents;
	}
	public ArrayList<Double> getTimes(){
		return this.times;
	}
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if(inlinks.contains(event.getLinkId().toString())){
			entries.remove(event.getVehicleId());
		}
	}
	@Override
	public void handleEvent(PersonStuckEvent event) {
		entries.remove(persontoveh.get(event.getPersonId()));
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		persontoveh.put(event.getPersonId(), event.getVehicleId());
		
	}
	@Override
	public void handleEvent(Event event) {
		if(event.getTime()-time>=3600) {
			if(numveh==0)numveh=1;
			delays.add(delay/numveh);
			times.add((double)Math.round(event.getTime()/3600));
			numagents.add((double)numveh);
			numonlinks.add((double)entries.size());
			delay=0;numveh = 0;
			time=(double)Math.round(event.getTime()/3600)*3600;
		}
		
	}
}
