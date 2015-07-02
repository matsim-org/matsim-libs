package lanes;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

class LinkLaneTTTestEventHandler implements LinkEnterEventHandler,  LinkLeaveEventHandler
		{
	
	private Map<Id<Link>,Double> linkTravelTimes = new HashMap<Id<Link>,Double>();
	private Map<Id<Link>,Double> linkEnterTimes = new HashMap<Id<Link>,Double>();
	
	@Override
	public void reset(int iteration) {
		this.linkEnterTimes.clear();
		this.linkTravelTimes.clear();
	}
	

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(!(event.getLinkId().equals(Id.create("outgoing", Link.class)) ||  event.getLinkId().equals(Id.create("incoming", Link.class )))){
			this.linkEnterTimes.put(event.getLinkId(),event.getTime());
		}
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(!(event.getLinkId().equals(Id.create("outgoing", Link.class)) ||  event.getLinkId().equals(Id.create("incoming", Link.class )))){
			this.linkTravelTimes.put(event.getLinkId() , event.getTime() - this.linkEnterTimes.get(event.getLinkId()));
		}
	}

	double getTTV1Sp75(){
		return this.linkTravelTimes.get(Id.createLinkId("200noLanesSpeed75"));	
	}
	
	double getTTV1Sp76(){
		return this.linkTravelTimes.get(Id.createLinkId("200noLanesSpeed76"));	
	}
	
	double getTTV2Sp75(){
		return this.linkTravelTimes.get(Id.createLinkId("50noLanesSpeed75")) + this.linkTravelTimes.get(Id.createLinkId("150noLanesSpeed75"));	
	}
	
	double getTTV2Sp76(){
		return this.linkTravelTimes.get(Id.createLinkId("50noLanesSpeed76")) + this.linkTravelTimes.get(Id.createLinkId("150noLanesSpeed76"));	
	}
	
	double getTTV3Sp75(){
		return this.linkTravelTimes.get(Id.createLinkId("200LanesSpeed75"));
	}
	
	double getTTV3Sp76(){
		return this.linkTravelTimes.get(Id.createLinkId("200LanesSpeed76"));	
	}
	
	public void printResults(){
		System.out.println("-----PRINTING RESULTS-----");
		
		System.out.println("\n see quellcode of LinkLaneTTTest for further information and graphical view of the links configurations\n");
		
		System.out.println("°FIRST version° \n normal 200m Link");
		System.out.println("TT when freespeed is 75:\t" + getTTV1Sp75());
		System.out.println("TT when freespeed is 76:\t" + getTTV1Sp76());
			
		System.out.println("\n °SECOND version° \n link splitted in one 50m link and one 150m link");
		System.out.println("TT when freespeed is 75:\t" +  getTTV2Sp75());
		System.out.println("TT when freespeed is 76:\t" + getTTV2Sp76());
			
		System.out.println("\n °THIRD version° \n link splitted in one 50m original lane and one 150m lane");
		System.out.println("TT when freespeed is 75:\t" + getTTV3Sp75());
		System.out.println("TT when freespeed is 76:\t" +  getTTV3Sp76());

		}
		
	}
	

