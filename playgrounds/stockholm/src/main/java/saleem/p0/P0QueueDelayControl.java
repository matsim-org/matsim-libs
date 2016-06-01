package saleem.p0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
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
	ArrayList<Double> absolutepressuredifference = new ArrayList<Double>();//To check the convergence quality
	double satcapacity2, satcapacity4, satcapacity5, capacity2, capacity4, capacity5, timelastupated=0;//calccaplink2 and calccaplink4 used to apply limit
	double totaldelaylink2=0, totaldelaylink4=0, averagedelaylink2=0, averagedelaylink4=0;
	int countvehlink2=0, countvehlink4=0;
	int count=0;
	int ctevents = 0;
	double factor2=0, factor4=0;
	String str="";
	public static List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>() ;
	int iter;
	P0QueueDelayControl(NetworkImpl network, int iter){
		this.iter=iter;
		this.network=network;
		 link2 = network.getLinks().get(Id.create("2", Link.class));
		 link4 = network.getLinks().get(Id.create("4", Link.class));
		 link5 = network.getLinks().get(Id.create("5", Link.class));
		 satcapacity2 = link2.getCapacity();
		 satcapacity4 = link4.getCapacity();
		 satcapacity5 = link5.getCapacity();
		 events = new ArrayList<NetworkChangeEvent>() ;
		 
		 NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(21600 + Math.random()/10000);//Assuming the simulations start at 06:00
		 change.addLink(link2);
		 change.setFlowCapacityChange((new ChangeValue(ChangeType.ABSOLUTE, 500.0/3600.0)));
		 addNetworkChangeEvent(change);
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
		//plot.PlotCapacities(times, capacitieslink2, capacitieslink4);
		//plot.PlotDelays(times, avgdelayslink2, avgdelayslink4);
		//plot.PlotDelaysandCapacities(times, capacitieslink2, capacitieslink4, avgdelayslink2, avgdelayslink4);
	}
	public void plotAbsoultePressureDifference(ArrayList<Double> iters, ArrayList<Double> avgabsolutepressuredifference){
		PlotStatistics plot = new PlotStatistics();
		//plot.PlotAbsolutePressureDiff(iters, avgabsolutepressuredifference);
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
	public ArrayList<Double> getCapacitiesLink2(){
		return capacitieslink2;
	}
	public ArrayList<Double> getCapacitiesLink4(){
		return capacitieslink4;
	}
	public void handleEvent (LinkLeaveEvent event){
		if(event.getLinkId().toString().equals("2") || event.getLinkId().toString().equals("4")){
			deptimes.put(event.getVehicleId().toString(), event.getTime());
			vehiclesandlinks.put(event.getVehicleId().toString(), event.getLinkId().toString());//Which vehicle uses which link
			if(count==0){//Change the capacities to the ones imposed by the outgoing junction link as the process starts, done only in first iteration
				count++;
				 //network.setNetworkChangeEvents(new ArrayList<NetworkChangeEvent>());
				 double combinedsatcapacity = satcapacity2 + satcapacity4;//So that the combined capacities are no more than the merged link capacity
				 capacity2 = link2.getCapacity();
				 capacity4 = link4.getCapacity();
				 if(capacity2 == satcapacity2){
					 capacity2=satcapacity2*satcapacity5/combinedsatcapacity;
				 }
				 if(capacity4 == satcapacity4){
					 capacity4=satcapacity4*satcapacity5/combinedsatcapacity;
				 }
				 NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(21600 + Math.random()/10000);//Assuming the simulations start at 06:00
				 change.addLink(link2);
				 //change.setFlowCapacityChange((new ChangeValue(ChangeType.FACTOR, capacity2/satcapacity5)));
				 change.setFlowCapacityChange((new ChangeValue(ChangeType.FACTOR, capacity2/satcapacity5)));
				 factor2=capacity2/satcapacity5;
				 //network.addNetworkChangeEvent(change);
				 //addNetworkChangeEvent(change);
				 str = "Time: " + change.getStartTime() + " Link: " + ((Link)change.getLinks().toArray()[0]).getId() + " Factor: " + change.getFlowCapacityChange().getValue() + " Iter: " + iter + "\n";
				 NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(21600 + Math.random()/10000);
				 change1.addLink(link4);
				 change1.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, capacity4/satcapacity5));//Allowing due share
				 //addNetworkChangeEvent(change1);
				 factor4=capacity4/satcapacity5;
				 str += "Time: " + change1.getStartTime() + " Link: " + ((Link)change1.getLinks().toArray()[0]).getId() + " Factor: " + change1.getFlowCapacityChange().getValue() + " Iter: " + iter + "\n";
				 // network.addNetworkChangeEvent(change1);
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
	public void addNetworkChangeEvent(NetworkChangeEvent event){
		int i = 0;
		while (i<events.size() &&  event.getStartTime() > events.get(i).getStartTime()) {
		    i++;
		}
		events.add(event);
	}
	public void printEvents(){
		/*int i = 0;
		while (i<events.size()) {
			NetworkChangeEvent event = events.get(i);
			System.out.println("Time: " + event.getStartTime() + " Link: " + ((Link)event.getLinks().toArray()[0]).getId() + " Factor: " + event.getFlowCapacityChange().getValue());
		    i++;
		}*/
		System.out.println(str);
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
			if((event.getTime()-timelastupated)>=500 && timelastupated>0){
				//timelastupated=roundTime(event.getTime()-200) +  (iter+1.0)/1000 + Math.random()/10000;
				timelastupated=event.getTime() +  Math.random()/10000;
				if(countvehlink2!=0)averagedelaylink2 = totaldelaylink2/countvehlink2;
				if(countvehlink4!=0)averagedelaylink4 = totaldelaylink4/countvehlink4;
				times.add(timelastupated);
				if(averagedelaylink2!=0)avgdelayslink2.add(averagedelaylink2);
				if(averagedelaylink4!=0)avgdelayslink4.add(averagedelaylink4);
				totaldelaylink2=0;totaldelaylink4=0;countvehlink2=0;countvehlink4=0;
				adjustCapacityP0(timelastupated);
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
	public double roundTime(double time){
		time = time - time%300;
		return time;
	}
	public void adjustCapacityP0(double time) {
			
	   double p2 = averagedelaylink2 * satcapacity2;//where satcapacity2 and satcapacity4 refer to saturation capacity, and capacity2 and capacity4 refer to flow capacities
	   double p4 = averagedelaylink4 * satcapacity4;
	   capacity2 = link2.getCapacity();
	   capacity4 = link4.getCapacity();
	   //double factor = 200/(iter+1);//To make the capacity change dependent on number of day/iteration
	   double factor = 200;
	   ((LinkImpl)link4).getFlowCapacityPerSec();
	   double abs = Math.abs(p2-p4);
	   //-100 for starting from start of time bin,  (iter+1)/1000 + Math.random()/10000 for ordering the events based on iteration number as well as limiting it from exceptions due to two events on same time
	   if (p2>p4 && factor2<0.8 && ctevents<iter){
		   		ctevents++;
			   	NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(time);//To ensure the change takes effect at the start of the time bin
				change.addLink(link2);
				change.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 1+factor/capacity2));
				 //addNetworkChangeEvent(change);
				//network.addNetworkChangeEvent(change);
				//addNetworkChangeEvent(change);
				 str += "Time: " + change.getStartTime() + " Link: " + ((Link)change.getLinks().toArray()[0]).getId() + " Factor: " + change.getFlowCapacityChange().getValue() + " Iter: " + iter + "\n";
				 factor2=factor2*(1+factor/capacity2);
				
				NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(time);
				change1.addLink(link4);
				change1.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 1-factor/capacity4));
				//addNetworkChangeEvent(change1);
				//network.addNetworkChangeEvent(change1);
				//addNetworkChangeEvent(change1);
				 str += "Time: " + change1.getStartTime() + " Link: " + ((Link)change1.getLinks().toArray()[0]).getId() + " Factor: " + change1.getFlowCapacityChange().getValue() + " Iter: " + iter + "\n";
				 factor4=factor4*(1-factor/capacity4);
	   }
	   else if (p4>p2 && factor4<0.8 && ctevents<iter){
		   		ctevents++;
			   	NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(time);
				change.addLink(link2);
				change.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(ChangeType.FACTOR, 1-factor/capacity2));
				//network.addNetworkChangeEvent(change);
				//addNetworkChangeEvent(change);
				//addNetworkChangeEvent(change);
				 str += "Time: " + change.getStartTime() + " Link: " + ((Link)change.getLinks().toArray()[0]).getId() + " Factor: " + change.getFlowCapacityChange().getValue() + " Iter: " + iter + "\n";
				 factor2=factor2*(1-factor/capacity2);
				
				NetworkChangeEvent change1 = network.getFactory().createNetworkChangeEvent(time);
				change1.addLink(link4);
				change1.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(ChangeType.FACTOR, 1+factor/capacity4));
				//network.addNetworkChangeEvent(change1);
				//addNetworkChangeEvent(change1);
				// addNetworkChangeEvent(change1);
				 str += "Time: " + change1.getStartTime() + " Link: " + ((Link)change1.getLinks().toArray()[0]).getId() + " Factor: " + change1.getFlowCapacityChange().getValue() + " Iter: " + iter + "\n";
				 factor4=factor4*(1+factor/capacity4);
	   }
		capacitieslink2.add(capacity2);
		capacitieslink4.add(capacity4);
		absolutepressuredifference.add(abs);

	}
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		lastarrivaltime= event.getTime();
		//System.out.println("Last Arrival Time: " + claculateTime(lastarrivaltime));
		
	}
}
