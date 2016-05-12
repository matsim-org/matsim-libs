package saleem.p0.stockholm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vehicles.Vehicle;

import saleem.p0.PlotStatistics;
import saleem.p0.TextReaderWriter;
import saleem.stockholmscenario.utils.CollectionUtil;

//For Generic Junctions

public class StockholmP0Handler implements BasicEventHandler{
	NetworkImpl network;int iteration;double bintime=0;
	List<Link> inLinks = new ArrayList<Link>();//Incoming links
	List<Link> outLinks = new ArrayList<Link>();//Outgoing links
	Map<Id<Link>, Link> allLinks = new HashMap<Id<Link>, Link>();//Both incoming and outgoing links in the junction
	Map<Id<Link>, Double> satCapacities = new LinkedHashMap<Id<Link>, Double>();//Saturation Capacities for Links
	Map<Id<Link>, Double> capacitiesLinks = new LinkedHashMap<Id<Link>, Double>();//Flow capacities in current bin 
	Map<Id<Link>, Map<Double, Double>> capacities = new LinkedHashMap<Id<Link>, Map<Double, Double>>();//Flow capacities all links, all bins
	Map<Id<Link>, Map<Double, Double>> lastcapacities = new LinkedHashMap<Id<Link>, Map<Double, Double>>();//Flow capacities all links, all bins, on previous day
	Map<Id<Link>, List<Double>> withoutP0capacities = new LinkedHashMap<Id<Link>, List<Double>>();//Representing Non P0 capacities for plotting purposes
	Map<Id<Link>, List<Double>> withoutP0delayLinks = new LinkedHashMap<Id<Link>, List<Double>>();//Representing Non P0 delays for plotting purposes
	Map<Id<Vehicle>, Double> arrtimes = new HashMap<Id<Vehicle>, Double>();
	Map<Id<Vehicle>, Double> deptimes = new HashMap<Id<Vehicle>, Double>();
	Map<Id<Vehicle>, Id<Link>> vehiclesandlinks = new HashMap<Id<Vehicle>, Id<Link>>();
	Map<Id<Link>, Map<Double, Double>> delayLinks = new LinkedHashMap<Id<Link>, Map<Double, Double>>();//Delays all links, all bins
	List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>() ;
	Map<Id<Link>, Map<Integer, Double>> delayOverIterLinks = new LinkedHashMap<Id<Link>, Map<Integer, Double>>();//Daily delays all links, each iteration
	Map<Double, Double> avgabsolutepressures = new LinkedHashMap<Double, Double>();//Average pressures over all links over each bin
	Map<Double, Double> dailyavgabspres = new LinkedHashMap<Double, Double>();//Average absolute pressure per day
	Map<Id<Link>, Map<Double, Double>> abspreslinks = new LinkedHashMap<Id<Link>, Map<Double, Double>>();//Absolute pressure per link, per time bin
	Map<Double, Map<Id<Link>, Double>> dailyabspreslinks = new LinkedHashMap<Double, Map<Id<Link>, Double>>();//Absolute pressure per iteration, per link
	Map<Id<Link>, Double> totalDelayLinks = new LinkedHashMap<Id<Link>, Double>();//Total delay Per current time bin
	Map<Id<Link>, Double> avgDelayLinks = new LinkedHashMap<Id<Link>, Double>();//Average delay Per current time bin
	Map<Id<Link>, Double> vehCountsLinks = new LinkedHashMap<Id<Link>, Double>();//Per Current Bin
	Map<Id<Link>, Double> totalVehCountsLinks = new LinkedHashMap<Id<Link>, Double>();//Per day
	Scenario scenario;
	public StockholmP0Handler(Scenario scenario, List<Link> inLinks, List<Link> outLinks, NetworkImpl network) {
		this.network=network;
		this.inLinks=inLinks;
		this.outLinks=outLinks;
		this.allLinks=this.network.getLinks();
		this.scenario=scenario;
	}
	//Initialise all variable used except events, lastcapacities, dailyavgabspres etc. where we have to keep track of values from older days
	public void initialise(int iteration){
		bintime=0;
		Iterator<Link> iterator = this.inLinks.iterator();
		totalDelayLinks = new LinkedHashMap<Id<Link>, Double>();
		avgDelayLinks = new LinkedHashMap<Id<Link>, Double>();
		totalVehCountsLinks = new LinkedHashMap<Id<Link>, Double>();
		vehCountsLinks = new LinkedHashMap<Id<Link>, Double>();
		satCapacities = new LinkedHashMap<Id<Link>, Double>();
		capacitiesLinks = new LinkedHashMap<Id<Link>, Double>();
		avgabsolutepressures = new LinkedHashMap<Double, Double>();
		Map<Id<Link>, Double> dailyabspres = new LinkedHashMap<Id<Link>, Double>();
		while(iterator.hasNext()){
			Link link = (Link)iterator.next();
			Map<Double, Double> capac = new LinkedHashMap<Double, Double>();
//			Map<Double, Double> lastcapac = new LinkedHashMap<Double, Double>();
			Map<Double, Double> delays = new LinkedHashMap<Double, Double>();
			Map<Double, Double> abspres = new LinkedHashMap<Double, Double>();
			Map<Integer, Double> delayoveriter = new LinkedHashMap<Integer, Double>();
			this.totalDelayLinks.put(link.getId(), 0.0);
			this.avgDelayLinks.put(link.getId(), 0.0);
			this.vehCountsLinks.put(link.getId(), 0.0);
			this.totalVehCountsLinks.put(link.getId(), 0.0);
			this.satCapacities.put(link.getId(), link.getCapacity());
			this.capacitiesLinks.put(link.getId(), 0.5*link.getCapacity());//0.5 share at the start
			this.arrtimes = new LinkedHashMap<Id<Vehicle>, Double>();
			this.deptimes = new LinkedHashMap<Id<Vehicle>, Double>();
			this.vehiclesandlinks = new LinkedHashMap<Id<Vehicle>, Id<Link>>();
			this.capacities.put(link.getId(), capac);
//			this.lastcapacities.put(link.getId(), lastcapac);
			this.delayLinks.put(link.getId(), delays);
			this.abspreslinks.put(link.getId(), abspres);
			dailyabspres.put(link.getId(), 0.0);
			if(delayOverIterLinks.get(link.getId())==null){//Initialise only first time
				delayOverIterLinks.put(link.getId(), delayoveriter);
			}
			this.iteration=iteration;
			if(iteration==0){
				 NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(21600 + Math.random()/10000+Math.random()/100000+Math.random()/1000000);//Assuming the simulations start at 06:00
				 change.addLink(link);
				 // The statement 1/inLinks.size() to give equal percentage initially to all links
				 change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, 1.0/inLinks.size()*link.getCapacity()/3600));
				 events.add(change);
			}
			this.dailyabspreslinks.put((double)this.iteration, dailyabspres);
		}
	}
	@Override
	public void reset(int iteration) {
	// TODO Auto-generated method stub
	
	}
	public boolean contains(List<Link> inLinks, Id<Link> id) {//Whether the InLinks conatin the link with id "id"
		Iterator<Link> iterator = inLinks.iterator();
		while(iterator.hasNext()){
			Link link = iterator.next();
			if(link.getId().toString().equals(id.toString())){
				return true;
			}
		}
		return false;
	}
	public void handleLinkLeaveEvent (LinkLeaveEvent event){
		if(contains(this.inLinks, event.getLinkId())){
			deptimes.put(event.getVehicleId(), event.getTime());//What time it left the inlink, to calculate delay
			vehiclesandlinks.put(event.getVehicleId(), event.getLinkId());//Which vehicle uses which link
		}
	}
	public void handleLinkEnterEvent (LinkEnterEvent event){
		if(contains(this.inLinks, event.getLinkId())){
			arrtimes.put(event.getVehicleId(), event.getTime());//What time it arrived on the inlink, to calculate delay
		}
		else if(contains(this.outLinks, event.getLinkId())){
			Link link = this.allLinks.get(vehiclesandlinks.get(event.getVehicleId()));
			double timeonlink = deptimes.get(event.getVehicleId())-arrtimes.get(event.getVehicleId());
			double timeonlinkfreespeed = link.getLength()/link.getFreespeed();
			double totaldelaylink = totalDelayLinks.get(link.getId()) + ((timeonlink - timeonlinkfreespeed)/3600);//Convert to per hour
			totalDelayLinks.put(link.getId(), totaldelaylink) ;
			double vehcountlink = vehCountsLinks.get(link.getId()) + 1;
			vehCountsLinks.put(link.getId(), vehcountlink) ;
		}

	}
	//Records delay and vehicle count from current bin, and re-initialise the current bin values
	public void updateDelays(LinkEnterEvent event){
		Iterator<Link> iterator = inLinks.iterator();
		while(iterator.hasNext()){
			Id<Link> linkid = iterator.next().getId();
			if(vehCountsLinks.get(linkid)!=0){
				avgDelayLinks.put(linkid, totalDelayLinks.get(linkid)/vehCountsLinks.get(linkid));
			}else{
				avgDelayLinks.put(linkid, 0.0);
			}
			totalVehCountsLinks.put(linkid, totalVehCountsLinks.get(linkid)+vehCountsLinks.get(linkid));
			totalDelayLinks.put(linkid, 0.0);
			vehCountsLinks.put(linkid, 0.0);
		}
}
	public void handleEvent(Event event) {
		if (event.getEventType().equals("left link")){
		    handleLinkLeaveEvent((LinkLeaveEvent)event);
	    }
		else if (event.getEventType().equals("entered link")){
			handleLinkEnterEvent((LinkEnterEvent)event);
			if(event.getTime()-bintime>500){
				updateDelays((LinkEnterEvent)event);
				bintime = event.getTime() - event.getTime()%500;
				adjustCapacityP0( bintime, avgDelayLinks, this.satCapacities);
				Iterator<Link> iterator = inLinks.iterator();
				double totalabspreslinks = 0;int size = inLinks.size();
				while(iterator.hasNext()) {
					Id<Link> linkid = iterator.next().getId();
					this.delayLinks.get(linkid).put(bintime, avgDelayLinks.get(linkid));
					double abspreslink = avgDelayLinks.get(linkid)*satCapacities.get(linkid);
					totalabspreslinks += abspreslink;
					this.abspreslinks.get(linkid).put(bintime, abspreslink);
					this.capacities.get(linkid).put(bintime, capacitiesLinks.get(linkid));
				}
				double avgabspres=totalabspreslinks/size;
				this.avgabsolutepressures.put(bintime, avgabspres);
			}
	    }
	}
	//Adjusting capacity according to P0. Change capacities on the last day, in the same bin, according to difference in link pressure with average pressure, in current bin.
	public void adjustCapacityP0(double time, Map<Id<Link>, Double> delayinthisbinoverlinks, Map<Id<Link>, Double> satCapacities) {
		if(iteration==0){
			   return;
		   }
		Iterator<Link> iterator = inLinks.iterator();
		double m = 0.00001/iteration;//To make the capacity change dependent on number of day/iteration; As iterations stars with '0'
		Map<Id<Link>, Double> abspressures = new LinkedHashMap<Id<Link>, Double>();//
		Map<Id<Link>, Double> greendeltas = new LinkedHashMap<Id<Link>, Double>();//
		double avgpressure = 0;
		while(iterator.hasNext()){
			Id<Link> linkid = iterator.next().getId();
			System.out.println("Average Delays: " + time + "..." + delayinthisbinoverlinks.get(linkid));
			double abspres = delayinthisbinoverlinks.get(linkid)*satCapacities.get(linkid);
			abspressures.put(linkid, abspres);
			avgpressure += abspres;
		}
		avgpressure/=inLinks.size();
		Iterator<Link> iterator1 = inLinks.iterator();
//		boolean pos = false;
		while(iterator1.hasNext()){
			Id<Link> linkid = iterator1.next().getId();
			greendeltas.put(linkid, m*(abspressures.get(linkid)-avgpressure));
			System.out.println("Green Delta: " + m*(abspressures.get(linkid)-avgpressure) + ": Pres Diff Withh Avg: " + (abspressures.get(linkid)-avgpressure) );
//			double val = Math.signum(abspressures.get(linkid)-avgpressure);
//			if(val==0 && pos == true){
//				val=-1;//Sign of 0 is 0
//			}
//			else if(val==0 && pos == false){
//				val=1;pos=true;//Sign of 0 is 0
//			}
//			greendeltas.put(linkid, m*val);
		}

		Iterator<Link> iterator2 = inLinks.iterator();
		while(iterator2.hasNext()){
			Link link = iterator2.next();
			Map<Double, Double> lastcapacitieslink =  lastcapacities.get(link.getId());
			if(lastcapacitieslink.get(time)!=null){
				double newcapacity = lastcapacitieslink.get(time)+greendeltas.get(link.getId())*satCapacities.get(link.getId());
//				double newcapacity = lastcapacitieslink.get(time)+greendeltas.get(link.getId())*satCapacities.get(link.getId());
				if(newcapacity>50 && newcapacity<satCapacities.get(link.getId())){
					this.capacitiesLinks.put(link.getId(),newcapacity);
					NetworkChangeEvent change = network.getFactory().createNetworkChangeEvent(time+Math.random()/10000+Math.random()/100000+Math.random()/1000000);//To ensure the change takes effect at the start of the time bin
					change.addLink(link);
					change.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, capacitiesLinks.get(link.getId())/3600));
					events.add(change);
				}
			}
			else{
				   return;
			}
		}
	}
	//To change time in double to time in hh:mm:ss format
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
	//To keep track of capacities on the last day
	public void populatelastCapacities(){
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			Map<Double, Double> lastcapacitiesLink = new LinkedHashMap<Double, Double>();
			Map<Double, Double> capacitieslink =  capacities.get(linkid);
			Iterator<Double> iter = capacitieslink.keySet().iterator();
			while (iter.hasNext()){ 
				double bintime = iter.next();
				lastcapacitiesLink.put(bintime, capacitieslink.get(bintime));
			}
			lastcapacities.put(linkid, lastcapacitiesLink);
		}
	}
	public void printCapacityStats(){
	Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			Map<Double, Double> capacitieslink =  capacities.get(linkid);
			Iterator<Double> iter = capacitieslink.keySet().iterator();
			while (iter.hasNext()){ 
				double bintime = iter.next();
				System.out.println("Time Bin: " + bintime + " Capacity Link " + linkid.toString() +" : " + capacitieslink.get(bintime));
			}
			System.out.println();
		}
	}
	public void printDelayStats(){
		double delaylink=0;
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			delaylink=0;
			Id<Link> linkid = iterlinks.next().getId();
			Map<Double, Double> delaysLink =  delayLinks.get(linkid);
			Iterator<Double> iter = delaysLink.keySet().iterator();
			while (iter.hasNext()){ 
				double bintime = iter.next();
				delaylink+=delaysLink.get(bintime);
			}
			this.delayOverIterLinks.get(linkid).put(this.iteration, delaylink/delaysLink.size());
			System.out.println("Average Delay Link " + linkid.toString() + ": " + (delaylink/delaysLink.size()));//Delay in seconds
			System.out.println("Total Vehicles on Link " + linkid.toString() + ": " + totalVehCountsLinks.get(linkid));
		}
	}
	//Basically to write Non P0 delays to text files, in order to be read laters for plotting purposes
	public void writeDelaystoFiles(){
		TextReaderWriter rw = new TextReaderWriter();
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			Map<Double, Double> delaysLink =  delayLinks.get(linkid);
			rw.writeToTextFile(cutil.toArrayList(delaysLink.values().iterator()), scenario.getConfig().controler()
					.getOutputDirectory()+File.separator+"InitialDelays" + linkid.toString() + ".txt");
		}
	}
	//Basically to write Non P0 capacities to text files, in order to be read laters for plotting purposes
	public void writeCapacitiestoFiles(){
		TextReaderWriter rw = new TextReaderWriter();
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			Map<Double, Double> capacitiesLink =  capacities.get(linkid);
			rw.writeToTextFile(cutil.toArrayList(capacitiesLink.values().iterator()), scenario.getConfig().controler()
					.getOutputDirectory()+File.separator+"InitialCapacities" + linkid.toString() + ".txt");
		}
	}
	//For already written Non P0 delays
	public void readDelaysFromFiles(){
		TextReaderWriter rw = new TextReaderWriter();
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			ArrayList<Double> initialDelaysLink = rw.readFromTextFile(scenario.getConfig().controler()
					.getOutputDirectory()+"NP"+File.separator+"InitialDelays" + linkid.toString() + ".txt");
			withoutP0delayLinks.put(linkid, initialDelaysLink);
		}
	}
	//For already written Non P0 Capacities
	public void readCapacitiesFromFiles(){
		TextReaderWriter rw = new TextReaderWriter();
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			ArrayList<Double> initialCapacitiesLink = rw.readFromTextFile(scenario.getConfig().controler()
					.getOutputDirectory()+"NP"+File.separator+"InitialCapacities" + linkid.toString() + ".txt");
			withoutP0capacities.put(linkid, initialCapacitiesLink);
		}
	}
	public void plotStats(){
		PlotStatistics plot = new PlotStatistics();
		String path = scenario.getConfig().controler()
				.getOutputDirectory()+File.separator+"ITERS"+File.separator+"it." + iteration + File.separator+"CapacitiesStats" + ".png";
		plot.PlotCapacitiesGeneric(path, this.capacities, this.withoutP0capacities);
		path = scenario.getConfig().controler()
				.getOutputDirectory()+File.separator+"ITERS"+File.separator+"it." + iteration + File.separator+"AverageDelaysStats" + ".png";
		plot.PlotDelaysGeneric(path, this.delayLinks, this.withoutP0delayLinks);
		path = scenario.getConfig().controler()
				.getOutputDirectory()+File.separator+"ITERS"+File.separator+"it." + iteration + File.separator+"DailyAvgAbsPres" + ".png";
		plot.PlotAbsPresGeneric(path, inLinks.iterator(), dailyavgabspres, this.dailyabspreslinks);
	}
	//To keep tracks of the daily average absolute pressure on inlinks
	public void addDailyAverageAbsPres(){
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			Map<Double, Double> abspres = abspreslinks.get(linkid);
			Iterator<Double> iterbins = abspres.keySet().iterator();
			double value=0;
			while(iterbins.hasNext()){
				double bin = iterbins.next();
				value += abspres.get(bin);
			}
			value = value/abspres.size();
			this.dailyabspreslinks.get((double)this.iteration).put(linkid, value);
		}
		
	}
	//To keep track of the daily average absolute pressure difference among links
	public void addDailyAverageAbsPresDiff(){
		Iterator<Double> iterbins = avgabsolutepressures.keySet().iterator();
		while(iterbins.hasNext()){
			double bin = iterbins.next();double value=0;
			Iterator<Link> iterlinks = inLinks.iterator();
			while(iterlinks.hasNext()){
				Id<Link> linkid = iterlinks.next().getId();
				value += Math.abs(abspreslinks.get(linkid).get(bin)-avgabsolutepressures.get(bin));
			}
			value = value/inLinks.size();
			avgabsolutepressures.put(bin, value);
		}
		
		iterbins = avgabsolutepressures.keySet().iterator();
		double value = 0;
		while(iterbins.hasNext()){
			double bin = iterbins.next();
			value += avgabsolutepressures.get(bin);
		}
		value=value/avgabsolutepressures.size();
		dailyavgabspres.put((double)this.iteration, value);
	}
	public void printAverageDelayOverLast20Iters(){
		double delaylink=0;
		if(this.iteration<500){
			return;//return if too early
		}
		Iterator<Link> iterlinks = inLinks.iterator();
		while(iterlinks.hasNext()){
			delaylink=0;
			Id<Link> linkid = iterlinks.next().getId();
			for (int i=481;i<=500;i++){ 
				delaylink+=this.delayOverIterLinks.get(linkid).get(i);
				
			}
			System.out.println("Average Delay Link " + linkid + "  for Last 20 Iterations: " + (delaylink/20));//In seconds
		}
		
	}
}
