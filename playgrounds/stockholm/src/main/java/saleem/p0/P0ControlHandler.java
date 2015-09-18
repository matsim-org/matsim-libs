package saleem.p0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

public class P0ControlHandler implements BasicEventHandler{
	Link link2, link4, link5;
	NetworkImpl network;
	int iter;
	double capacity2, capacity4, satcapacity2, satcapacity4, satcapacity5, bintime=0;
	public Map<Double, Double> capacitiesLink2 = new HashMap<Double, Double>();
	public Map<Double, Double> capacitiesLink4 = new HashMap<Double, Double>();
	Map<String, Double> arrtimes = new HashMap<String, Double>();
	Map<String, Double> deptimes = new HashMap<String, Double>();
	Map<String, String> vehiclesandlinks = new HashMap<String, String>();
	public Map<Double, Double> delaysLink2 = new HashMap<Double, Double>();
	public Map<Double, Double> delaysLink4 = new HashMap<Double, Double>();
	public static List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>() ;
	ArrayList<Double> absolutepressuredifference = new ArrayList<Double>();//To check the convergence quality
	double totaldelaylink2=0, totaldelaylink4=0, averagedelaylink2=0, averagedelaylink4=0;
	int countvehlink2=0, countvehlink4=0;
	public P0ControlHandler(NetworkImpl network, int iter) {
		this.iter=iter;
		this.network=network;
		 link2 = network.getLinks().get(Id.create("2", Link.class));
		 link4 = network.getLinks().get(Id.create("4", Link.class));
		 link5 = network.getLinks().get(Id.create("5", Link.class));
		 satcapacity2 = link2.getCapacity();
		 satcapacity4 = link4.getCapacity();
		 satcapacity5 = link5.getCapacity();
		 capacity2=satcapacity2*satcapacity5/(satcapacity2+satcapacity4);//In Veh Per Hour
		 capacity4=satcapacity4*satcapacity5/(satcapacity2+satcapacity4);
		 if(iter==0){
			 NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(21600 + Math.random()/10000);//Assuming the simulations start at 06:00
			 change.addLink(link2);
			 change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, capacity2/3600));
			 events.add(change);
			 
			 NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(21600 + Math.random()/10000);//Assuming the simulations start at 06:00
			 change1.addLink(link4);
			 change1.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, capacity4/3600));
			 events.add(change1);
			}
	}
	@Override
	public void reset(int iteration) {
	// TODO Auto-generated method stub
	
	}
	public void handleLinkLeaveEvent (LinkLeaveEvent event){
		if(event.getLinkId().toString().equals("2") || event.getLinkId().toString().equals("4")){
			deptimes.put(event.getVehicleId().toString(), event.getTime());
			vehiclesandlinks.put(event.getVehicleId().toString(), event.getLinkId().toString());//Which vehicle uses which link
		}
	}
	public void handleLinkEnterEvent (LinkEnterEvent event){
		if(event.getLinkId().toString().equals("2") || event.getLinkId().toString().equals("4")){
			arrtimes.put(event.getVehicleId().toString(), event.getTime());
		}
		else if(event.getLinkId().toString().equals("5")){
			Link link = getLink(vehiclesandlinks.get(event.getVehicleId().toString()));
			double timeonlink = deptimes.get(event.getVehicleId().toString())-arrtimes.get(event.getVehicleId().toString());
			double timeonlinkfreespeed = link.getLength()/link.getFreespeed();
			if(vehiclesandlinks.get(event.getVehicleId().toString()).equals("2")){//If the link is link 2
				totaldelaylink2 += timeonlink - timeonlinkfreespeed;
				countvehlink2++;
			}
			else if (vehiclesandlinks.get(event.getVehicleId().toString()).equals("4")){//If the link is link 4
				totaldelaylink4 += timeonlink - timeonlinkfreespeed;
				countvehlink4++;
			}
		}

	}
	public void updateDelays(LinkEnterEvent event){
				if(countvehlink2!=0){
					averagedelaylink2 = totaldelaylink2/countvehlink2;
				}else{
					averagedelaylink2 = 0;
				}
				if(countvehlink4!=0){
					averagedelaylink4 = totaldelaylink4/countvehlink4;
				}else{
					averagedelaylink4 = 0;
				}
				totaldelaylink2=0;totaldelaylink4=0;countvehlink2=0;countvehlink4=0;
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
	public void handleEvent(Event event) {
		if (event.getEventType().equals("left link")){
		    // TODO Auto-generated method stub
		    handleLinkLeaveEvent((LinkLeaveEvent)event);
	    }
		else if (event.getEventType().equals("entered link")){
		    // TODO Auto-generated method stub
			handleLinkEnterEvent((LinkEnterEvent)event);
			if(event.getTime()-bintime>500){
				updateDelays((LinkEnterEvent)event);
				bintime = event.getTime() - event.getTime()%500;
				double abspreslink2 = averagedelaylink2*satcapacity2;
				double abspres1ink4 = averagedelaylink4*satcapacity4;	
				double abspresdiff = Math.abs(abspreslink2 - abspres1ink4);
				delaysLink2.put(bintime, averagedelaylink2);
				delaysLink4.put(bintime, averagedelaylink4);
				absolutepressuredifference.add(abspresdiff);
				adjustCapacityP0(bintime-500);
				capacitiesLink2.put(bintime-500, capacity2);
				capacitiesLink4.put(bintime-500, capacity4);
			}
	    }
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
		   double factor = 200/(iter+1);//To make the capacity change dependent on number of day/iteration
		   double abs = Math.abs(p2-p4);
		   if (p2>p4){
			   		if(capacity2+factor<satcapacity2 && capacity4-factor>0){
			   			capacity2=capacity2+factor;
				   		capacity4=capacity4-factor;
			   			NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(time+Math.random()/10000);//To ensure the change takes effect at the start of the time bin
						change.addLink(link2);
						change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, capacity2/3600));
						events.add(change);
						
						NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(time+Math.random()/10000);
						change1.addLink(link4);
						change1.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, capacity4/3600));
						events.add(change1);
			   		}
		   }
		   else if (p4>p2){
		   	   	if(capacity4+factor<satcapacity4 && capacity2-factor>0){
		   	   		capacity2=capacity2-factor;
		   	   		capacity4=capacity4+factor;
		   			NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(time+Math.random()/10000);//To ensure the change takes effect at the start of the time bin
					change.addLink(link2);
					change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, capacity2/3600));
					events.add(change);
					
					NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(time+Math.random()/10000);
					change1.addLink(link4);
					change1.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, capacity4/3600));
					events.add(change1);
		   		}
		   }
	}
	public void printDelayStats(){
		double delaylink2=0;
		double delaylink4=0;
		Iterator<Double> iter = delaysLink2.values().iterator();
		Iterator<Double> iter1 = delaysLink4.values().iterator();
		while (iter.hasNext()){ 
			delaylink2+=iter.next();
		}
		while (iter1.hasNext()){ 
			delaylink4+=iter1.next();
		}
		System.out.println("Average Delay Link 2: " + delaylink2/delaysLink2.values().size());
		System.out.println("Average Delay Link 4: " + delaylink4/delaysLink4.values().size());
	}
	public void plotStats(){
		ArrayList<Double> capacitieslink2 = toArrayList(capacitiesLink2.values().iterator());
		ArrayList<Double> capacitieslink4 = toArrayList(capacitiesLink4.values().iterator());
		ArrayList<Double> times = toArrayList(capacitiesLink2.keySet().iterator());
		ArrayList<Double> delayslink2 = toArrayList(delaysLink2.values().iterator());
		ArrayList<Double> delayslink4 = toArrayList(delaysLink4.values().iterator());
		PlotStatistics plot = new PlotStatistics();
		plot.PlotCapacities(times, capacitieslink2, capacitieslink4);
		plot.PlotDelays(times, delayslink2, delayslink4);
		plot.PlotDelaysandCapacities(times, capacitieslink2, capacitieslink4, delayslink2, delayslink4);
	}
	public ArrayList<Double> toArrayList(Iterator<Double> iter){
		ArrayList<Double> arraylist = new ArrayList<Double>();
		while(iter.hasNext()){
			arraylist.add(iter.next());
		}
		return arraylist;
	}
	public double getAvgPressDiffOverIter(){
		double totalpressdiff=0;
		int count = 0;
		Iterator<Double> iter = absolutepressuredifference.iterator();
		while (iter.hasNext()){ 
			totalpressdiff+=iter.next();
			count++;
		}
		return totalpressdiff/count;
	}
	public void plotAbsoultePressureDifference(ArrayList<Double> iters, ArrayList<Double> avgabsolutepressuredifference){
		PlotStatistics plot = new PlotStatistics();
		plot.PlotAbsolutePressureDiff(iters, avgabsolutepressuredifference);
	}
}
