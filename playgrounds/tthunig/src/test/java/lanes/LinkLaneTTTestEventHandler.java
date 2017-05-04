package lanes;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.lanes.data.Lane;

/**
 * @author Tilmann Schlenther
 */
class LinkLaneTTTestEventHandler implements LinkEnterEventHandler,  LinkLeaveEventHandler , LaneEnterEventHandler, LaneLeaveEventHandler{
	
	
	private int caseNr;
	private Map<Id<Link>,Double> linkTravelTimes ;
	private Map<Id<Link>,Double> linkEnterTimes ;
	private Map<Id<Lane>,Double> laneTravelTimes ;
	private Map<Id<Lane>,Double> laneEnterTimes ;
	private TreeMap<Integer,Double> caselinkTravelTimes ;
	private TreeMap<Integer,Map<Id<Lane>,Double>> caseLaneTravelTimes;

	public LinkLaneTTTestEventHandler(){
		this.caseNr = 1;
		this.linkEnterTimes = new HashMap<Id<Link>,Double>();
		this.linkTravelTimes = new HashMap<Id<Link>,Double>();
		this.laneEnterTimes = new HashMap<Id<Lane>,Double>();
		this.laneTravelTimes = new HashMap<Id<Lane>,Double>();
		this.caselinkTravelTimes = new TreeMap<Integer,Double>() ;
		this.caseLaneTravelTimes = new TreeMap<Integer,Map<Id<Lane>,Double>>();
	}

	public void setToNextCase(){
		this.caseNr ++;
		this.linkEnterTimes.clear();
		this.linkTravelTimes.clear();
		this.laneEnterTimes.clear();
		this.laneTravelTimes.clear();
	}
	
	@Override
	public void reset(int iteration) {
		setToNextCase();
	}
	
	@Override
	public void handleEvent(LaneEnterEvent event) {
		this.laneEnterTimes.put(event.getLaneId(), event.getTime());
	}
	
	@Override
	public void handleEvent(LaneLeaveEvent event) {
		this.laneTravelTimes.put(event.getLaneId(), event.getTime() 
				- this.laneEnterTimes.get(event.getLaneId()));
		
	}	

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// handle link enter events of links between 1 and 3
		// (in some cases there are several links between Link1 and Link3)
		if(!(event.getLinkId().equals(Id.create("Link1", Link.class)) ||  				
				event.getLinkId().equals(Id.create("Link3", Link.class )))){
			
			this.linkEnterTimes.put(event.getLinkId(),event.getTime());
		}
		
		if(event.getLinkId().equals(Id.createLinkId("Link3"))){
			this.caselinkTravelTimes.put(caseNr, getTTofCase());
			if(this.caseNr >=3 && this.caseNr <=6){
				Map<Id<Lane>,Double> laneTT = new HashMap<Id<Lane>, Double>(this.laneTravelTimes);
				this.caseLaneTravelTimes.put(caseNr, laneTT);
			}
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// handle link enter events of links between 1 and 3
		// (in some cases there are several links between Link1 and Link3)
		if(!(event.getLinkId().equals(Id.create("Link1",Link.class)) ||  				
				event.getLinkId().equals(Id.create("Link3", Link.class )))){
			
			this.linkTravelTimes.put(event.getLinkId() , event.getTime() 
					- this.linkEnterTimes.get(event.getLinkId()));
		}
	}

	private double getTTofCase(){
		if (this.caseNr >= 1 && this.caseNr <= 6) {
			return this.linkTravelTimes.get(Id.createLinkId("Link2"));
		} else if (this.caseNr == 7 || this.caseNr == 8) {
			return this.linkTravelTimes.get(Id.createLinkId("Link2"))
					+ this.linkTravelTimes.get(Id.createLinkId("Link2.1"));
		}
		return -1;
	}
	
	public void printResults(){
		String laneString;
		System.out.println("\n----- RESULTS -----");
		
		System.out.println("\n °°NOTE: See quellcode of LinkLaneTTTest for further "
				+ "information and graphical view of the link configurations°°\n");
		
		System.out.println("normal link of 200 m");
		System.out.println("TT when freespeed is 75:\t" + this.caselinkTravelTimes.get(1));
		System.out.println("TT when freespeed is 76:\t"	+ this.caselinkTravelTimes.get(2) + "\n");

		System.out.println("separated in one 50m link and one 150m link");
		System.out.println("TT when freespeed is 75:\t"	+ this.caselinkTravelTimes.get(7));
		System.out.println("TT when freespeed is 76:\t"	+ this.caselinkTravelTimes.get(8) + "\n");

		System.out.println("200m link separated in 1 Lane of 50m and 1 following lane of 150m");
		laneString = ("\t Lane Travel Times:" + this.caseLaneTravelTimes.get(5).toString());
		System.out.println("TT when freespeed is 75:\t" + this.caselinkTravelTimes.get(5) + laneString);
		laneString = ("\t Lane Travel Times:" + this.caseLaneTravelTimes.get(6).toString() + "\n");
		System.out.println("TT when freespeed is 76:\t"	+ this.caselinkTravelTimes.get(6) + laneString);

		System.out.println("200m link separated in 4 lanes of 50m");
		laneString = ("\t Lane Travel Times:" + this.caseLaneTravelTimes.get(3).toString());
		System.out.println("TT when freespeed is 75:\t" + this.caselinkTravelTimes.get(3) + laneString);
		laneString = ("\t Lane Travel Times:" + this.caseLaneTravelTimes.get(4).toString() + "\n");
		System.out.println("TT when freespeed is 76:\t" + this.caselinkTravelTimes.get(4) + laneString);
	}
}
	

