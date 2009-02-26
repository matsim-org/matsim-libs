package playground.wrashid.PHEV.co2emissions;

import java.util.HashMap;


import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;

/**
 * This class computes the summary of co2 emissions per link for a specified interval.
 */
public class AllLinkOneIntervalHandler implements LinkLeaveEventHandler {

	// key: linkId, value: emissions
	private HashMap<String, Double> co2EmissionsWholeDay=new HashMap<String, Double>();
	private double CO2EmissionsGrammPerkm;
	private NetworkLayer network;
	private int intervalStart;
	private int intervalEnd;

	
	/**
	 * 
	 * @param CO2EmissionsGrammPerkm
	 * @param network
	 * @param intervalStart (in seconds)
	 * @param intervalEnd (in seconds)
	 */
	public AllLinkOneIntervalHandler(double CO2EmissionsGrammPerkm, NetworkLayer network, int intervalStart, int intervalEnd) {
		// initialize 
		this.CO2EmissionsGrammPerkm =CO2EmissionsGrammPerkm;
		this.network=network;
		this.intervalStart=intervalStart;
		this.intervalEnd=intervalEnd;
	}
	
	public void handleEvent(LinkLeaveEvent event) {
		// only the events in the given interval are considered
		if (event.time<intervalStart  || event.time>intervalEnd){
			return;
		}
		

		
		if (!co2EmissionsWholeDay.containsKey(event.linkId)){
			co2EmissionsWholeDay.put(event.linkId, 0.0);
		}
		co2EmissionsWholeDay.put(event.linkId, co2EmissionsWholeDay.get(event.linkId)+ network.getLink(event.linkId).getLength()/1000*CO2EmissionsGrammPerkm);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void printCO2EmissionsSpecifiedInterval() {
		System.out.println("interal start: " + intervalStart + "; interval end: " + intervalEnd);
		System.out.println("linkId\temissions [g CO2]");
		for (String linkId:co2EmissionsWholeDay.keySet()){
			System.out.println(linkId + "\t" + co2EmissionsWholeDay.get(linkId));
		}
		
		
	}

}
