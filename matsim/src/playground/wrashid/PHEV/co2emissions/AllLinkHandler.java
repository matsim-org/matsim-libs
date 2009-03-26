package playground.wrashid.PHEV.co2emissions;

import java.util.HashMap;


import org.matsim.core.api.network.Link;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.NetworkLayer;

/**
 * This class computes the summary of co2 emissions per link over the day.
 */
public class AllLinkHandler implements LinkLeaveEventHandler {

	// key: linkId, value: emissions
	private HashMap<String, Double> co2EmissionsWholeDay=new HashMap<String, Double>();
	private double CO2EmissionsGrammPerkm;
	private NetworkLayer network;

	
	
	public AllLinkHandler(double CO2EmissionsGrammPerkm, NetworkLayer network) {
		// initialize 
		this.CO2EmissionsGrammPerkm =CO2EmissionsGrammPerkm;
		this.network=network;
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		if (!co2EmissionsWholeDay.containsKey(event.getLinkId().toString())){
			co2EmissionsWholeDay.put(event.getLinkId().toString(), 0.0);
		}
		co2EmissionsWholeDay.put(event.getLinkId().toString(), co2EmissionsWholeDay.get(event.getLinkId().toString())+ network.getLink(event.getLinkId().toString()).getLength()/1000*CO2EmissionsGrammPerkm);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void printCO2EmissionsWholeDay() {
		System.out.println("linkId\temissins [g CO2]");
		for (String linkId:co2EmissionsWholeDay.keySet()){
			System.out.println(linkId + "\t" + co2EmissionsWholeDay.get(linkId));
		}
		
		
	}

}
