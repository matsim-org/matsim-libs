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
 * An event handling class to help in collecting link data for visualizing congestion benefits of P0
 * 
 * @author Mohammad Saleem
 */
public class VisualisationHandler implements BasicEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler{
	List<String> inlinks;
	double hour = 0;
	double time = 0;
	int numveh = 0;

	Map<Id<Link>, ? extends Link> links;
	
	Map<Id<Link>, Map<Id<Vehicle>, Double>> linksvehtimes = new LinkedHashMap<Id<Link>, Map<Id<Vehicle>, Double>>();//Flow capacities all links, all bins
	Map<Id<Link>, Map<Double, Double>> linkhrrelspeed = new LinkedHashMap<Id<Link>, Map<Double, Double>>();//Flow capacities all links, all bins
	
	Map<Id<Link>, Double> linktimes = new LinkedHashMap<Id<Link>, Double>();
	Map<Id<Link>, Double> linknumvehs = new LinkedHashMap<Id<Link>, Double>();
		
	public VisualisationHandler(Network network) {
		this.links=network.getLinks();
		Iterator<Id<Link>> iterator = this.links.keySet().iterator();
		while(iterator.hasNext()){
			Id<Link> linkid = iterator.next();
			linkhrrelspeed.put(linkid, new LinkedHashMap<Double, Double>());
		}
		initialise();
	}
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(linksvehtimes.get(event.getLinkId()).get(event.getVehicleId())==null){
			return;
		}
		double time = event.getTime()-linksvehtimes.get(event.getLinkId()).remove(event.getVehicleId());
		linktimes.put(event.getLinkId(), linktimes.get(event.getLinkId())+time);
		linknumvehs.put(event.getLinkId(), linknumvehs.get(event.getLinkId())+1);

	}
		// TODO Auto-generated method stub
	@Override
	public void handleEvent(LinkEnterEvent event) {//Record entries against vehicles
			linksvehtimes.get(event.getLinkId()).put(event.getVehicleId(), event.getTime());
		// TODO Auto-generated method stub
		
	}
	@Override
	public void handleEvent(Event event) {
		if(event.getTime()-time>=3600) {
			hour++;
			Iterator<Id<Link>> iter = linktimes.keySet().iterator();
			while(iter.hasNext()){
				Id<Link> linkid = iter.next();
				double time = linktimes.get(linkid);if(time==0)time=1;
				double absspeed = (linknumvehs.get(linkid) * links.get(linkid).getLength()/time);
				double relspeed = absspeed/links.get(linkid).getFreespeed();
				linkhrrelspeed.get(linkid).put(hour, relspeed);
			}
			time=(double)Math.round(event.getTime()/3600)*3600;
			initialise();
		}
		// TODO Auto-generated method stub
		
	}
	public void initialise(){
		Iterator<Id<Link>> iterator = this.links.keySet().iterator();
		while(iterator.hasNext()){
			Id<Link> linkid = iterator.next();
			linksvehtimes.put(linkid, new LinkedHashMap<Id<Vehicle>, Double>());
			linktimes.put(linkid, 0.0);
			linknumvehs.put(linkid, 0.0);
		}
	}
	public Map<Id<Link>, Map<Double, Double>> getRelativeSpeeds(){
		return this.linkhrrelspeed;
	}
}
