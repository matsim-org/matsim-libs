package playground.wrashid.PHEV.co2emissions;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.network.NetworkLayer;

/**
 * This class computes the summary of co2 emissions per link over the day.
 */
public class AllLinkHandler implements BasicLinkLeaveEventHandler {

	// key: linkId, value: emissions
	private HashMap<Id, Double> co2EmissionsWholeDay=new HashMap<Id, Double>();
	private double CO2EmissionsGrammPerkm;
	private NetworkLayer network;

	
	
	public AllLinkHandler(double CO2EmissionsGrammPerkm, NetworkLayer network) {
		// initialize 
		this.CO2EmissionsGrammPerkm =CO2EmissionsGrammPerkm;
		this.network=network;
	}
	
	public void handleEvent(BasicLinkLeaveEvent event) {
		if (!co2EmissionsWholeDay.containsKey(event.getLinkId())){
			co2EmissionsWholeDay.put(event.getLinkId(), 0.0);
		}
		co2EmissionsWholeDay.put(event.getLinkId(), co2EmissionsWholeDay.get(event.getLinkId()) + network.getLinks().get(event.getLinkId()).getLength()/1000*CO2EmissionsGrammPerkm);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	public void printCO2EmissionsWholeDay() {
		System.out.println("linkId\temissins [g CO2]");
		for (Id linkId:co2EmissionsWholeDay.keySet()){
			System.out.println(linkId + "\t" + co2EmissionsWholeDay.get(linkId));
		}
		
		
	}

}
