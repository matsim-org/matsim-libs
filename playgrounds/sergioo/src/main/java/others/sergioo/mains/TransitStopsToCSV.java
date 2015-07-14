package others.sergioo.mains;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TransitStopsToCSV {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		int numRoutesBus = 0, numRoutesRail = 0, numDeparturesBus = 0, numDeparturesRail = 0;
		Set<Id<TransitStopFacility>> railPlatforms = new HashSet<Id<TransitStopFacility>>();
		Set<String> railStops = new HashSet<String>();
		Set<Id<TransitStopFacility>> busStops = new HashSet<Id<TransitStopFacility>>();
		Set<Id<TransitLine>> railLines = new HashSet<Id<TransitLine>>();
		Set<Id<TransitLine>> busLines = new HashSet<Id<TransitLine>>();
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
