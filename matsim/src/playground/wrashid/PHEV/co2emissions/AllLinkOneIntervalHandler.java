package playground.wrashid.PHEV.co2emissions;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.network.NetworkLayer;

/**
 * This class computes the summary of co2 emissions per link for a specified interval.
 */
public class AllLinkOneIntervalHandler implements LinkLeaveEventHandler {

	// key: linkId, value: emissions
	private HashMap<Id, Double> co2EmissionsWholeDay=new HashMap<Id, Double>();
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
	
	public void handleEvent(LinkLeaveEventImpl event) {
		// only the events in the given interval are considered
		if (event.getTime()<intervalStart  || event.getTime()>intervalEnd){
			return;
		}
		Id linkId = event.getLinkId();
				
		if (!co2EmissionsWholeDay.containsKey(linkId)){
			co2EmissionsWholeDay.put(linkId, 0.0);
		}
		co2EmissionsWholeDay.put(linkId, co2EmissionsWholeDay.get(linkId)+ network.getLink(linkId).getLength()/1000*CO2EmissionsGrammPerkm);
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public void printCO2EmissionsSpecifiedInterval() {
		System.out.println("interal start: " + intervalStart + "; interval end: " + intervalEnd);
		System.out.println("linkId\temissions [g CO2]");
		for (Id linkId:co2EmissionsWholeDay.keySet()){
			System.out.println(linkId + "\t" + co2EmissionsWholeDay.get(linkId));
		}
		
		
	}

}
