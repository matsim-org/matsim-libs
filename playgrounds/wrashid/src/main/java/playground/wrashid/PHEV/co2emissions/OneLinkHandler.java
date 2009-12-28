package playground.wrashid.PHEV.co2emissions;

import java.util.HashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;

/**
 * This class computes the summary of co2 emissions for one specified link, for an interval specified in seconds
 */
public class OneLinkHandler implements LinkLeaveEventHandler {

	// key: hour, value: emissions
	private HashMap<Long, Double> co2EmissionsEachHour=new HashMap<Long, Double>();
	private double CO2EmissionsGrammPerkm;
	private Link link;
	private int interval;

	
	
	public OneLinkHandler(double CO2EmissionsGrammPerkm, Network network, String linkId, int interval) {
		// initialize 
		this.CO2EmissionsGrammPerkm =CO2EmissionsGrammPerkm;
		this.link = network.getLinks().get(new IdImpl(linkId));
		this.interval=interval;
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().equals(link.getId())){
			long hour= Math.round(event.getTime()/interval);
			if (!co2EmissionsEachHour.containsKey(hour)){
				co2EmissionsEachHour.put(hour, 0.0);
			}
			co2EmissionsEachHour.put(hour, co2EmissionsEachHour.get(hour)+ link.getLength()/1000*CO2EmissionsGrammPerkm);
		}
		

	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void printHourlyCO2Emissions() {
		System.out.println("linkId: " + link.getId().toString());
		System.out.println("time [h]\temissions [g CO2]");
		for (Long intv:co2EmissionsEachHour.keySet()){
			System.out.println(intv*interval/ (double)3600+ "\t" + co2EmissionsEachHour.get(intv));
		}
		
		
	}

}
