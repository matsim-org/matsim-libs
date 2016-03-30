package saleem.p0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkImpl;

import saleem.stockholmscenario.utils.CollectionUtil;

public class P0ControlHandler implements BasicEventHandler{
	Link link2, link4, link5;
	NetworkImpl network;
	int iter;
	double capacity2, capacity4, satcapacity2, satcapacity4, satcapacity5, bintime=0;
	public Map<Double, Double> capacitiesLink2 = new HashMap<Double, Double>();
	public Map<Double, Double> capacitiesLink4 = new HashMap<Double, Double>();
	public static Map<Double, Double> lastcapacitiesLink2 = new HashMap<Double, Double>();
	public static Map<Double, Double> lastcapacitiesLink4 = new HashMap<Double, Double>();
	ArrayList<Double> initialcapacitiesLink2 = new ArrayList<Double>();
	ArrayList<Double> initialcapacitiesLink4 = new ArrayList<Double>();
	Map<String, Double> arrtimes = new HashMap<String, Double>();
	Map<String, Double> deptimes = new HashMap<String, Double>();
	Map<String, String> vehiclesandlinks = new HashMap<String, String>();
	public Map<Double, Double> delaysLink2 = new HashMap<Double, Double>();
	public Map<Double, Double> delaysLink4 = new HashMap<Double, Double>();
	public static List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>() ;
	public static List<Double>delayOverIterLink2 = new ArrayList<Double>() ;//Average delay on link2 over each iteration
	public static List<Double> delayOverIterLink4 = new ArrayList<Double>() ;
	ArrayList<Double> absolutepressuredifference = new ArrayList<Double>();//To check the convergence quality
	ArrayList<Double> absolutepressurelink2 = new ArrayList<Double>();//To check the convergence quality
	ArrayList<Double> absolutepressurelink4 = new ArrayList<Double>();//To check the convergence quality
	double totaldelaylink2=0, totaldelaylink4=0, averagedelaylink2=0, averagedelaylink4=0;
	int countvehlink2=0, countvehlink4=0;
	int totalcountvehlink2=0, totalcountvehlink4=0;
	public P0ControlHandler(NetworkImpl network) {
		this.network=network;
	}
	public void initialise(int iter){
		this.capacitiesLink2 = new HashMap<Double, Double>();
		this.capacitiesLink4 = new HashMap<Double, Double>();
		this.initialcapacitiesLink2 = new ArrayList<Double>();
		this.initialcapacitiesLink4 = new ArrayList<Double>();
		this.arrtimes = new HashMap<String, Double>();
		this.deptimes = new HashMap<String, Double>();
		this.vehiclesandlinks = new HashMap<String, String>();
		this.delaysLink2 = new HashMap<Double, Double>();
		this.delaysLink4 = new HashMap<Double, Double>();
		this.iter=iter;
		this.bintime=0;
		this.absolutepressuredifference = new ArrayList<Double>();//To check the convergence quality
		this.absolutepressurelink2 = new ArrayList<Double>();//To check the convergence quality
		this.absolutepressurelink4 = new ArrayList<Double>();//To check the convergence quality
		this.totaldelaylink2=0; this.totaldelaylink4=0; this.averagedelaylink2=0; this.averagedelaylink4=0;
		this.countvehlink2=0; this.countvehlink4=0;totalcountvehlink2=0; totalcountvehlink4=0;
		this.link2 = network.getLinks().get(Id.create("2", Link.class));
		this.link4 = network.getLinks().get(Id.create("4", Link.class));
		this.link5 = network.getLinks().get(Id.create("5", Link.class));
		this.satcapacity2 = link2.getCapacity();
		this.satcapacity4 = link4.getCapacity();
		this.satcapacity5 = link5.getCapacity();
		this.capacity2=0.5*this.satcapacity2;//The value of green time is 0.5 initially. In Veh Per Hour
		this.capacity4=0.5*this.satcapacity4;
		this.initialcapacitiesLink2.add(this.capacity2);
		this.initialcapacitiesLink4.add(this.capacity4);
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
				totalcountvehlink2+=countvehlink2; totalcountvehlink4+=countvehlink4;
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
				delaysLink2.put(bintime, averagedelaylink2);
				delaysLink4.put(bintime, averagedelaylink4);
				adjustCapacityP0(satcapacity2, satcapacity4,bintime-500);
				double abspreslink2 = averagedelaylink2*satcapacity2;
				double abspres1ink4 = averagedelaylink4*satcapacity4;	
				double abspresdiff = Math.abs(abspreslink2 - abspres1ink4);
				absolutepressuredifference.add(abspresdiff);
				absolutepressurelink2.add(abspreslink2);
				absolutepressurelink4.add(abspres1ink4);
				capacitiesLink2.put(bintime-500, capacity2);
				capacitiesLink4.put(bintime-500, capacity4);
				//Capacities on day one
				initialcapacitiesLink2.add(initialcapacitiesLink2.get(0));
				initialcapacitiesLink4.add(initialcapacitiesLink4.get(0));
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
	public void populatelastCapacities(){
		lastcapacitiesLink2 = new HashMap<Double, Double>();
		lastcapacitiesLink4 = new HashMap<Double, Double>();
		Iterator<Double> iter = capacitiesLink2.keySet().iterator();
		while (iter.hasNext()){ 
			double key = iter.next();
			lastcapacitiesLink2.put(key, capacitiesLink2.get(key));
		}
		Iterator<Double> iter1 = capacitiesLink4.keySet().iterator();
		while (iter1.hasNext()){ 
			double key = iter1.next();
			lastcapacitiesLink4.put(key, capacitiesLink4.get(key));
		}
	}
	public void adjustCapacityP0(double w2, double w4, double time) {
		
		   double p2 = averagedelaylink2 * w2;//where w2 and w4 are equal to saturationflow2 and saturationflow4 for unblocked junctions, and 1 each for blocked junctions
		   double p4 = averagedelaylink4 * w4;
		   if(iter==0){
			   return;
		   }
		   System.out.println("Average Delays: "  + time + "..." + averagedelaylink2 + "..." + averagedelaylink4);
		   double m = 0.1/iter;//To make the capacity change dependent on number of day/iteration
		   if(lastcapacitiesLink2.get(time)!=null){
			  capacity2=lastcapacitiesLink2.get(time);
			  capacity4=lastcapacitiesLink4.get(time);
		   }
		   else{
			   return;
		   }
		   double factor2 = m*satcapacity2;
		   double factor4 = m*satcapacity4;//To make the capacity change dependent on number of day/iteration

		   //p2=p2/capacity2;
		   //p4=p4/capacity4;
		   if (p2>=p4){
			   		if(capacity2+factor2<satcapacity2  && capacity4-factor4>50){
			   			capacity2=capacity2+factor2;
				   		capacity4=capacity4-factor4;
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
		   	   	if(capacity4+factor4<satcapacity4 && capacity2-factor2>50){
		   	   		capacity2=capacity2-factor2;
		   	   		capacity4=capacity4+factor4;
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
		this.delayOverIterLink2.add(this.iter,delaylink2/delaysLink2.values().size());
		this.delayOverIterLink4.add(this.iter,delaylink4/delaysLink4.values().size());
		System.out.println("Average Delay Link 2: " + delaylink2/delaysLink2.values().size());
		System.out.println("Average Delay Link 4: " + delaylink4/delaysLink4.values().size());
		System.out.println("Total Vehicles on Link 2: " + totalcountvehlink2);
		System.out.println("Total Vehicles on Link 4: " + totalcountvehlink4);
		
	}
	public void AverageDelayOverLast20Iters(){
		double delaylink2=0;
		double delaylink4=0;
		if(iter<481){
			return;//return if too early
		}
		for (int i=481;i<=500;i++){ 
			delaylink2+=this.delayOverIterLink2.get(i);
		}
		for (int i=481;i<=500;i++){ 
			delaylink4+=this.delayOverIterLink4.get(i);
		}
		System.out.println("Average Delay Link 2 for Last 20 Iterations: " + delaylink2/20);
		System.out.println("Average Delay Link 4  for Last 20 Iterations: " + delaylink4/20);
		
	}
	public void printCapacityStats(){
		
		Iterator<Double> iter = capacitiesLink2.keySet().iterator();
		while (iter.hasNext()){ 
			double key = iter.next();
			System.out.println("Time Bin: " + key + " Capacity Link 2: " + capacitiesLink2.get(key) + " Capacity Link 4: " + capacitiesLink4.get(key));
		}
		System.out.println();
	}
	//For plotting without P0
	public void writeInitiaDelaystoFile(){
		TextReaderWriter rw = new TextReaderWriter();
		rw.writeToTextFile(toArrayList(delaysLink2.values().iterator()), "H:\\Mike Work\\delayslink2.txt");
		rw.writeToTextFile(toArrayList(delaysLink2.keySet().iterator()), "H:\\Mike Work\\timelink2.txt");
		rw.writeToTextFile(toArrayList(delaysLink4.values().iterator()), "H:\\Mike Work\\delayslink4.txt");
		rw.writeToTextFile(toArrayList(delaysLink4.keySet().iterator()), "H:\\Mike Work\\timelink4.txt");
	}
	//Sort delays according to time
	public ArrayList<Double> sortDelaysPerTime(Map<Double, Double> delays){
		ArrayList<Double> sorteddelays = new ArrayList<Double>();
		CollectionUtil cutil = new CollectionUtil();
		List<Double> times = cutil.toArrayList(delays.keySet().iterator());
		Collections.sort(times, new Comparator<Double>() {

	        public int compare(Double a, Double b) {
	            return (int)(a - b);
	        }
	    });
		for(Double d:times){
			sorteddelays.add(delays.get(d));
		}
		return sorteddelays;
	}
	//Sort times in increasing order
	public ArrayList<Double> sortTimes(ArrayList<Double> times){
		CollectionUtil cutil = new CollectionUtil();
		Collections.sort(times, new Comparator<Double>() {

	        public int compare(Double a, Double b) {
	            return (int)(a - b);
	        }
	    });
		return times;
	}
	public void plotStats(){
		TextReaderWriter rw = new TextReaderWriter();
		ArrayList<Double> initialdelaysLink2 = rw.readFromTextFile("H:\\Mike Work\\delayslink2.txt");
		ArrayList<Double> initialdelaysLink4 = rw.readFromTextFile("H:\\Mike Work\\delayslink4.txt");
		ArrayList<Double> initialtimeslink2 = rw.readFromTextFile("H:\\Mike Work\\timelink2.txt");
		ArrayList<Double> initialtimeslink4 = rw.readFromTextFile("H:\\Mike Work\\timelink4.txt");
//		if(iter==500)writeInitiaDelaystoFile();//For dashed line without P0, writing to text file
		ArrayList<Double> capacitieslink2 = toArrayList(capacitiesLink2.values().iterator());
		ArrayList<Double> capacitieslink4 = toArrayList(capacitiesLink4.values().iterator());
		ArrayList<Double> times = toArrayList(capacitiesLink2.keySet().iterator());
		ArrayList<Double> delayslink2 = sortDelaysPerTime(delaysLink2);
		ArrayList<Double> delayslink4 = sortDelaysPerTime(delaysLink4);
		PlotStatistics plot = new PlotStatistics();
		plot.PlotCapacities(iter, times, capacitieslink2, capacitieslink4, initialcapacitiesLink2, initialcapacitiesLink4);
		plot.PlotDelays(iter, sortTimes(toArrayList(delaysLink2.keySet().iterator())), sortTimes(toArrayList(delaysLink4.keySet().iterator())), delayslink2, delayslink4, initialtimeslink2, initialtimeslink4, initialdelaysLink2, initialdelaysLink4);
		plot.PlotDelaysandCapacities(iter, times, capacitieslink2, capacitieslink4, delayslink2, delayslink4);
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
	public void plotAbsolutePressures(){
		ArrayList<Double> abspreslink2 = toArrayList(absolutepressurelink2.iterator());
		ArrayList<Double> abspreslink4 = toArrayList(absolutepressurelink4.iterator());
		ArrayList<Double> times = toArrayList(capacitiesLink2.keySet().iterator());
		PlotStatistics plot = new PlotStatistics();
		plot.plotAbsolutePressures(iter, times, abspreslink2, abspreslink4);
	}
	public void plotAbsoultePressureDifference(ArrayList<Double> iters, ArrayList<Double> initialabsolutepressuredifference, ArrayList<Double> avgabsolutepressuredifference){
		PlotStatistics plot = new PlotStatistics();
		plot.PlotAbsolutePressureDiff(iter, iters, initialabsolutepressuredifference, avgabsolutepressuredifference);
	}
}
