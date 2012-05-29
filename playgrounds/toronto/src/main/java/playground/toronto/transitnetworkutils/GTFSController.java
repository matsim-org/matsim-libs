package playground.toronto.transitnetworkutils;


import java.io.BufferedWriter;
import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import GTFS2PTSchedule.GTFS2MATSimTransitSchedule;

public class GTFSController {

	public static void main(final String[] args){
		
		final String GTFSCONFIGNAME = "C:/Users/Peter Work/Desktop/NETWORK DATA/MATSIM NETWORK/gtfsconfig.xml";
		
		Config config = ConfigUtils.loadConfig(GTFSCONFIGNAME);
		String[] rts = config.getParam("gtfs", "roots").split(",");
		File[] roots = new File[rts.length];
		for(int i = 0; i < roots.length; i++) roots[i] = new File(rts[i]);
		
		//(File[] roots, String[] modes, Network network, String[] serviceIds, String outCoordinateSystem)
		GTFS2MATSimTransitSchedule gtfsTTC = new GTFS2MATSimTransitSchedule(roots, 
				config.getParam("gtfs", "modes").split(","), 
				ScenarioUtils.loadScenario(config).getNetwork(), 
				config.getParam("gtfs", "service").split(","), 
				config.getParam("gtfs", "outCoordinateSystem"));
		
		TransitSchedule output = gtfsTTC.getTransitSchedule();
		
		
		
		System.out.print("GTFS conversion done.");
		
	}
	
	
}
