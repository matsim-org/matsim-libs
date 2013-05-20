package others.sergioo.mains;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class TransitStopsToCSV {

	private static final String S = ",";

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		PrintWriter writer = new PrintWriter(args[1]);
		writer.println("Line"+S+"Route"+S+"Stop Id"+S+"Index");
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(int i = 0; i<route.getStops().size(); i ++)
					writer.println(line.getId()+S+route.getId()+S+route.getStops().get(i).getStopFacility().getId()+S+(i+1));
		writer.close();
	}

}
