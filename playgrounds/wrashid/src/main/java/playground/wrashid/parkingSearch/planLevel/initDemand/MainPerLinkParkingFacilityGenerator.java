package playground.wrashid.parkingSearch.planLevel.initDemand;

import java.util.HashMap;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;

/**
 * Add parking facilities to each road according to their length (at least on per road).
 * 
 * @author rashid_waraich
 * 
 */

public class MainPerLinkParkingFacilityGenerator {

	static HashMap<String, Integer> hm = new HashMap<String, Integer>();
	static int numberOfAgents = 0;

	public static void main(String[] args) {

		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\network.xml.gz";
		String facilitiesPath = "C:\\data\\workspace\\playgrounds\\wrashid\\test\\scenarios\\berlin\\parkingFacilities.xml.gz";

		new MatsimNetworkReader(sc).readFile(networkFile);
		NetworkLayer net = sc.getNetwork();

		ActivityFacilitiesImpl activityFacilities = new ActivityFacilitiesImpl();

		int parkPlatzId = 1;
		int totalNumberOfParkingsAdded=0;
		for (Link link : net.getLinks().values()) {
			// 5m long car, half of the street available for parking
			// don't do this change - will be done later probably
			int parkingCapacity = (int) Math.round(Math.ceil(link.getLength() / 2.0 / 5.0/100.0/2));
			totalNumberOfParkingsAdded+=parkingCapacity;
			ActivityFacilityImpl activityFacility = activityFacilities.createFacility(new IdImpl(parkPlatzId), link.getCoord());
			activityFacility.createActivityOption("parking").setCapacity(parkingCapacity);
			parkPlatzId++;
		}
		
		System.out.println("total number of parking facilities added: " + totalNumberOfParkingsAdded);

		FacilitiesWriter fw = new FacilitiesWriter(activityFacilities);
		fw.write(facilitiesPath);
	}

}
