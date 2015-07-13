package saleem.p0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkImpl;

public class P0QueueDelayControl implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonArrivalEventHandler{
	NetworkImpl network;
	double lastarrivaltime;
	Link link2, link4, link5;
	double delaylink2, delaylink4;
	Map<String, Double> arrtimes = new HashMap<String, Double>();
	Map<String, Double> deptimes = new HashMap<String, Double>();
	Map<String, String> vehiclesandlinks = new HashMap<String, String>();
	ArrayList<Double> times = new ArrayList<Double>();
	ArrayList<Double> capacitieslink2 = new ArrayList<Double>();//keep track of capacities for link 2 for plotting purposes
	ArrayList<Double> capacitieslink4 = new ArrayList<Double>();
	ArrayList<Double> avgdelayslink2 = new ArrayList<Double>();//keep track of average delays for link 2 for plotting purposes
	ArrayList<Double> avgdelayslink4 = new ArrayList<Double>();
	double satcapacity2, satcapacity4, satcapacity5, capacity2, capacity4, capacity5, timelastupated=0, factor2, factor4;
	double totaldelaylink2=0, totaldelaylink4=0, averagedelaylink2=0, averagedelaylink4=0;
	int countvehlink2=0, countvehlink4=0, count=0;
	List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>() ;
	P0QueueDelayControl(NetworkImpl network){
		this.network=network;
		 Map map = network.getLinks();
		 link2 = network.getLinks().get(Id.create("2", Link.class));
		 link4 = network.getLinks().get(Id.create("4", Link.class));
		 link5 = network.getLinks().get(Id.create("5", Link.class));
		 satcapacity2 = link2.getCapacity();
		 satcapacity4 = link4.getCapacity();
		 satcapacity5 = link5.getCapacity();
		 
	}
	public void printDelayStats(){
		double delaylink2=0;
		double delaylink4=0;
		Iterator<Double> iter = avgdelayslink2.iterator();
		Iterator<Double> iter1 = avgdelayslink4.iterator();
		while (iter.hasNext()){ 
			delaylink2+=iter.next();
		}
		while (iter1.hasNext()){ 
			delaylink4+=iter1.next();
		}
		System.out.println("Average Delay Link 2: " + delaylink2/avgdelayslink2.size());
		System.out.println("Average Delay Link 4: " + delaylink4/avgdelayslink4.size());
	}
	public void plotStats(){
		PlotStatistics plot = new PlotStatistics();
		plot.PlotCapacities(times, capacitieslink2, capacitieslink4);
		plot.PlotDelays(times, avgdelayslink2, avgdelayslink4);
		plot.PlotDelaysandCapacities(times, capacitieslink2, capacitieslink4, avgdelayslink2, avgdelayslink4);
	}
	public void handleEvent (LinkLeaveEvent event){
		if(event.getLinkId().toString().equals("2") || event.getLinkId().toString().equals("4")){
			deptimes.put(event.getVehicleId().toString(), event.getTime());
			vehiclesandlinks.put(event.getVehicleId().toString(), event.getLinkId().toString());//Which vehicle uses which link
			if(count==0){//Change the capacities to the ones imposed by the outgoing junction link as the process starts
				count++;
				 network.setNetworkChangeEvents(new ArrayList<NetworkChangeEvent>());
				 double combinedsatcapacity = satcapacity2 + satcapacity4;//So that the combined capacities are no more than the merged link capacity
				 capacity2=satcapacity2*satcapacity5/combinedsatcapacity;
				 NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(event.getTime() + Math.random());//Assuming the simulations start at 06:00
				 change.addLink(link2);
				 //change.setFlowCapacityChange((new ChangeValue(ChangeType.FACTOR, capacity2/satcapacity5)));
				 change.setFlowCapacityChange((new ChangeValue(ChangeType.FACTOR, capacity2/satcapacity5)));
				 network.addNetworkChangeEvent(change);
				 capacity4=satcapacity4*satcapacity5/combinedsatcapacity;
				 NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(event.getTime() + Math.random());
				 change1.addLink(link4);
				 change1.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, capacity4/satcapacity5));//Allowing due share
				 network.addNetworkChangeEvent(change1);
				 //network.getNetworkChangeEvents();
			}
		}
		
	}
	public Link getLink(String id){
		Map map = network.getLinks();
		Collection<Link> links = map.values();
		Iterator iterator = links.iterator();
		while (iterator.hasNext()){
			Link link = (Link)iterator.next();
			if(link.getId().toString().equals(id)){
				return link;
			}
		}
		return null;
	}
	public void handleEvent (LinkEnterEvent event){
		
		if(event.getLinkId().toString().equals("2") || event.getLinkId().toString().equals("4")){
			arrtimes.put(event.getVehicleId().toString(), event.getTime());
		}
		else if(event.getLinkId().toString().equals("5")){
			if (timelastupated==0){
				timelastupated=event.getTime();
			}
			Link link = getLink(vehiclesandlinks.get(event.getVehicleId().toString()));
			double timeonlink = deptimes.get(event.getVehicleId().toString())-arrtimes.get(event.getVehicleId().toString());
			double timeonlinkfreespeed = link.getLength()/link.getFreespeed();
			if(vehiclesandlinks.get(event.getVehicleId().toString()).equals("2")){//If the link is link 2
				totaldelaylink2 += timeonlink - timeonlinkfreespeed;
				//averagedelaylink2 = timeonlink - timeonlinkfreespeed;
				countvehlink2++;
			}
			else if (vehiclesandlinks.get(event.getVehicleId().toString()).equals("4")){//If the link is link 4
				totaldelaylink4 += timeonlink - timeonlinkfreespeed;
				//averagedelaylink4 = timeonlink - timeonlinkfreespeed;
				countvehlink4++;
			}
			if((event.getTime()-timelastupated)>300 && timelastupated>0){
				timelastupated=event.getTime();
				if(countvehlink2!=0)averagedelaylink2 = totaldelaylink2/countvehlink2;
				if(countvehlink4!=0)averagedelaylink4 = totaldelaylink4/countvehlink4;
				times.add(timelastupated);
				avgdelayslink2.add(averagedelaylink2);
				avgdelayslink4.add(averagedelaylink4);
				totaldelaylink2=0;totaldelaylink4=0;countvehlink2=0;countvehlink4=0;
				adjustCapacityP0(event.getTime());
			}
			if(event.getVehicleId().toString().equals("699") || event.getVehicleId().toString().equals("1299")){
				System.out.println("Entry: " + event.getVehicleId().toString() + "..." + claculateTime(event.getTime()));
			}
		}
	}
	@Override
	  public void reset(int iteration) {
	  }
	public String claculateTime(double timeInSeconds){
		int hours = (int) timeInSeconds / 3600;
	    int remainder = (int) timeInSeconds - hours * 3600;
	    int mins = remainder / 60;
	    remainder = remainder - mins * 60;
	    int secs = remainder;
	    String departureoffset = (hours<10)?"0"+hours+":":""+hours+":";//Hours, Mins and Secs in format "00:00:00"
	    departureoffset = (mins<10)?departureoffset+"0"+mins+":":departureoffset+mins+":";
	    departureoffset = (secs<10)?departureoffset+"0"+secs:departureoffset+secs;

		return departureoffset;
	}
	public void adjustCapacityP0(double time) {
			
	   double p2 = averagedelaylink2 * satcapacity2;//where satcapacity2 and satcapacity4 refer to saturation capacity, and capacity2 and capacity4 refer to flow capacities
	   double p4 = averagedelaylink4 * satcapacity4;
	   if (p2>p4){
		   if(capacity2 < 500 && capacity4>100 ){
			   
			   
			   	NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(time + Math.random());
				change.addLink(link2);
				change.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 1+100/capacity2));
				network.addNetworkChangeEvent(change);
				capacity2=capacity2*(1+100/capacity2);
				
				NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(time + Math.random());
				change1.addLink(link4);
				change1.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 1-100/capacity4));
				network.addNetworkChangeEvent(change1);
				capacity4=capacity4*(1-100/capacity4);
		   }
	   }
	   else if (p4>p2){
		   if(capacity4 < 500 && capacity2>100 ){
			   	NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(time + Math.random());
				change.addLink(link2);
				change.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(ChangeType.FACTOR, 1-100/capacity2));
				network.addNetworkChangeEvent(change);
				capacity2=capacity2*(1-100/capacity2);
				
				NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(time + Math.random());
				change1.addLink(link4);
				change1.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(ChangeType.FACTOR, 1+100/capacity4));
				network.addNetworkChangeEvent(change1);
				capacity4=capacity4*(1+100/capacity4);
		   }
	   }
		capacitieslink2.add(capacity2);
		capacitieslink4.add(capacity4);

	}
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		lastarrivaltime= event.getTime();
		//System.out.println("Last Arrival Time: " + claculateTime(lastarrivaltime));
		
	}
}
