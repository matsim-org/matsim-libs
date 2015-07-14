package playground.johannes.gsv.misc;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class TransitScheduleStats {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.nosbahn.xml");
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		
		System.out.println(String.format("Number of lines: %s", schedule.getTransitLines().size()));
		System.out.println(String.format("Number of stop facilities: %s", schedule.getFacilities().size()));
		
//		int numLines = 0;
		int numRoutes = 0;
		int numStops = 0;
		for(TransitLine line : schedule.getTransitLines().values()) {
//			numLines++;
			for(TransitRoute route : line.getRoutes().values()) {
				numRoutes++;
				numStops += route.getStops().size();
			}
		}
		
		System.out.println(String.format("Number of routes: %s", numRoutes));
		System.out.println(String.format("Number of route stops: %s", numStops));
	}

}
