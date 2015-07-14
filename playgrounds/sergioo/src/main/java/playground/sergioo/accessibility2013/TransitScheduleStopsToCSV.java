package playground.sergioo.accessibility2013;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TransitScheduleStopsToCSV {
	
	private static final String S = ",";
	private static final String S2 = ":";

	/**
	 * 
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		(new TransitScheduleReader(scenario)).readFile(args[0]);
		PrintWriter writer = new PrintWriter(args[1]);
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values()) {
			String textLine = stop.getId()+S+stop.getCoord().getX()+S+stop.getCoord().getY()+S+stop.getLinkId()+S+S2;
			LINES:
			for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
				for(TransitRoute route:line.getRoutes().values())
					for(TransitRouteStop routeStop:route.getStops())
						if(routeStop.getStopFacility().equals(stop)) {
							textLine+=line.getId()+S2;
							continue LINES;
						}
			writer.println(textLine);
		}
		writer.close();
	}
	
}
