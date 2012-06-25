package playground.toronto.gtfsutils;


import java.io.File;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import GTFS2PTSchedule.GTFS2MATSimTransitSchedule;

public class GTFSController {

	public static void main(final String[] args){
			
		if(args.length != 2) return;
		
		final String GTFSCONFIGNAME = args[0];
		final String SCHEDULEOUTPUTNAME = args[1];
		
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
		
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(output);
		writer.write(SCHEDULEOUTPUTNAME);
		
		
	}
	
	
}
