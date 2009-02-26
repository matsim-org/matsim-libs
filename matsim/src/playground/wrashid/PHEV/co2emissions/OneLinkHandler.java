package playground.wrashid.PHEV.co2emissions;

import java.util.HashMap;


import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;

/**
 * This class computes the summary of co2 emissions for one specified link, for an interval specified in seconds
 */
public class OneLinkHandler implements LinkLeaveEventHandler {

	// key: hour, value: emissions
	private HashMap<Long, Double> co2EmissionsEachHour=new HashMap<Long, Double>();
	private double CO2EmissionsGrammPerkm;
	private Link link;
	private int interval;

	
	
	public OneLinkHandler(double CO2EmissionsGrammPerkm, NetworkLayer network, String linkId, int interval) {
		// initialize 
		this.CO2EmissionsGrammPerkm =CO2EmissionsGrammPerkm;
		this.link = network.getLink(linkId);
		this.interval=interval;
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		if (event.linkId.equals(link.getId().toString())){
			long hour= Math.round(event.time/interval);
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
