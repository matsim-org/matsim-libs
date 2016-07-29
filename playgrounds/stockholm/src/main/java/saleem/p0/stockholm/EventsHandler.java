package saleem.p0.stockholm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

public class EventsHandler implements LinkLeaveEventHandler, LinkEnterEventHandler, VehicleLeavesTrafficEventHandler{
	List<String> inlinks;
	double delay = 0;
	double time = -3600;
	int numveh = 0;
	Network network;
	Map<Id<Vehicle>, Double> entries = new LinkedHashMap<Id<Vehicle>, Double>();//Entry time against vehicle to incoming links for delay calculation
	Map<String, Double> freetraveltimes = new LinkedHashMap<String, Double>();//Free speed against link id in String form
	List<Double> times = new ArrayList<Double>();//Time entries for plotting
	List<Double> delays = new ArrayList<Double>();//Delays for plotting
	List<Double> numagents = new ArrayList<Double>();//Number of agents moving through pretimed intersections for plotting
	List<Double> numonlinks = new ArrayList<Double>();//Number of agents on incoming links
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
		
		if(event.getTime()-time>=3190) {
			if(numveh==0)numveh=1;
			delays.add(delay/numveh);
			times.add(event.getTime()/3600);
			numagents.add((double)numveh);
			numonlinks.add((double)entries.size());
			delay=0;numveh = 0;
			time=event.getTime();
		}
		if(inlinks.contains(event.getLinkId().toString())){
			numveh++;
			if(entries.get(event.getVehicleId())!=null){
				double del = event.getTime()-entries.get(event.getVehicleId())
						- freetraveltimes.get(event.getLinkId().toString());
				delay = delay + del;
//				if(del>3600){
//					System.out.println(del + "..." + event.getLinkId().toString());
//				}
				entries.remove(event.getVehicleId());
			}
		}
		// TODO Auto-generated method stub
		
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {//Record entries against vehicles
		if(inlinks.contains(event.getLinkId().toString())){
			entries.put(event.getVehicleId(), event.getTime());
		}
		// TODO Auto-generated method stub
		
	}
	public List<Double> getDelays(){
		return this.delays;
	}
	public List<Double> getAgentsOnIncomingLinks(){
		return this.numonlinks;
	}
	public List<Double> getNumAgentsThroughIntersections(){
		return this.numagents;
	}
	public List<Double> getTimes(){
		return this.times;
	}
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		// TODO Auto-generated method stub
		if(inlinks.contains(event.getLinkId().toString())){
			entries.remove(event.getVehicleId());
		}
	}
}
