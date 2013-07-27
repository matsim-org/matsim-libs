package others.sergioo.mains;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
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
		int numRoutesBus = 0, numRoutesRail = 0, numDeparturesBus = 0, numDeparturesRail = 0;
		Set<Id> railPlatforms = new HashSet<Id>();
		Set<String> railStops = new HashSet<String>();
		Set<Id> busStops = new HashSet<Id>();
		Set<Id> railLines = new HashSet<Id>();
		Set<Id> busLines = new HashSet<Id>();
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values()) {
			for(TransitRoute route:line.getRoutes().values())
				if(route.getTransportMode().contains("bus")) {
					busLines.add(line.getId());
					numDeparturesBus+=route.getDepartures().size();
					numRoutesBus++;
					for(TransitRouteStop stop:route.getStops())
						busStops.add(stop.getStopFacility().getId());
				}
				else {
					railLines.add(line.getId());
					numDeparturesRail+=route.getDepartures().size();
					numRoutesRail++;
					for(TransitRouteStop stop:route.getStops()) {
						railPlatforms.add(stop.getStopFacility().getId());
						railStops.add(stop.getStopFacility().getName());
					}
				}
		}
		System.out.println(busLines.size());
		System.out.println(railLines.size());
		System.out.println(numRoutesBus);
		System.out.println(numRoutesRail);
		System.out.println(numDeparturesBus);
		System.out.println(numDeparturesRail);
		System.out.println(busStops.size());
		System.out.println(railPlatforms.size());
		System.out.println(railStops.size());
		/*
		PrintWriter writer = new PrintWriter(args[1]);
		writer.println("Line"+S+"Route"+S+"Stop Id"+S+"Index");
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				for(int i = 0; i<route.getStops().size(); i ++)
					writer.println(line.getId()+S+route.getId()+S+route.getStops().get(i).getStopFacility().getId()+S+(i+1));
		writer.close();*/
	}

}
