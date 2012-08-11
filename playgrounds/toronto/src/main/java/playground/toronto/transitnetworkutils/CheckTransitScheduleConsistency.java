package playground.toronto.transitnetworkutils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CheckTransitScheduleConsistency {

	private static final Logger log = Logger.getLogger(CheckTransitScheduleConsistency.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String networkFile = args[0];
		String scheduleFile = args[1];
		String vehiclesFile = args[2];
		String reportFile = args[3];
		String stopLinkGeometryFile = args[4];
		
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("network", "inputNetworkFile", networkFile);
		config.setParam("transit", "transitScheduleFile", scheduleFile);
		config.setParam("transit", "vehiclesFile", vehiclesFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile));
		writer.write("Transit Schedule Consistency Check Report\n" +
				"=================================================\n\n");
		
		BufferedWriter writer2 = new BufferedWriter(new FileWriter(stopLinkGeometryFile));
		writer2.write("stop_id,stop_x,stop_y,to_node_id,to_node_x,to_node_y");
		
		//Check stop link references
		log.info("Checking stops...");
		writer.write("STOPS:\n" +
				"--------------");
		
		int badStops = 0;
		int goodStops = 0;
		for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()){
			Link link = scenario.getNetwork().getLinks().get(stop.getLinkId());
			if (link == null){
				writer.write("\n\tStop id=\"" + stop.getId().toString() + "\" does not reference a valid link.");
				badStops++;
			}else{
				writer2.write("\n" + stop.getId() + "," + 
						stop.getCoord().getX() + "," + stop.getCoord().getY() + ","
						+ link.getToNode().getId() + "," + link.getToNode().getCoord().getX()
						+ "," + link.getToNode().getCoord().getY());
				goodStops++;
			}
		}
		
		log.info("Checking routes...");
		
		//Check route-stop references (@deprecated)
		//Check route-sequence-link references (& that they create a contiguous path)
		writer.write("\nROUTES:\n" +
				"----------------");
		
		int goodRoutes = 0;
		int routesWithBadLinks = 0;
		int routesWithInconsistentPaths = 0;
		HashSet<Id> missingLinks = new HashSet<Id>();
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				//Check route-sequence-link references
				HashSet<Id> routeMissingLinks = new HashSet<Id>();
				Link link;
				{
					Id i = route.getRoute().getStartLinkId();
					link = scenario.getNetwork().getLinks().get(i);
					if (link == null) {
						routeMissingLinks.add(i);
						missingLinks.add(i);
					}
				}
				for (Id linkId : route.getRoute().getLinkIds()){
					link = scenario.getNetwork().getLinks().get(linkId);
					if (link == null) {
						routeMissingLinks.add(linkId);
						missingLinks.add(linkId);
					}
				}
				{
					Id i = route.getRoute().getEndLinkId();
					link = scenario.getNetwork().getLinks().get(i);
					if (link == null){
						routeMissingLinks.add(i);
						missingLinks.add(i);
					}
				}
				if (routeMissingLinks.size() > 0){
					routesWithBadLinks++;
					writer.write("\n\tLine \"" + line.getId().toString() + "\", route \"" + route.getId().toString() + "\" is missing the following links:");
					for (Id i : routeMissingLinks) writer.write(" " + i.toString());
					continue; //do not check path consistency for routes with missing links
				}
				
				//Check that route creates a contiguous path
				HashSet<Tuple<Id, Id>> gaps = new HashSet<Tuple<Id,Id>>();
				Link prevLink = scenario.getNetwork().getLinks().get(route.getRoute().getStartLinkId());
				for (Id i : route.getRoute().getLinkIds()){
					Link currentLink = scenario.getNetwork().getLinks().get(i);
					if (! prevLink.getToNode().getId().equals(currentLink.getFromNode().getId())){
						gaps.add(new Tuple<Id, Id>(prevLink.getId(), i));
					}
					prevLink = currentLink;
				}
				if (gaps.size() > 0 ){
					routesWithInconsistentPaths++;
					writer.write("\n\tLine \"" + line.getId().toString() + "\", route \"" + route.getId().toString() + "\" has gaps between the following pairs of links:");
					for (Tuple<Id, Id> t : gaps){
						writer.write(" " + t.getFirst().toString() + "," + t.getSecond().toString() + ";");
					}
					continue;
				}
				
				goodRoutes++;
			}
		}
		
		writer.write("\n\nLIST OF MISSING LINKS\n" +
				"---------------------------------\n");
		
		for (Id i : missingLinks) writer.write("\n" + i.toString());

		log.info("Concistency Check Complete. \n\nResults:\n\n" +
				"Stops:\n" +
				goodStops + " stops are OK. " + badStops + " stops are missing link references.\n\n" +
				"Routes:\n" +
				goodRoutes + " routes are OK. " + routesWithInconsistentPaths + " routes have inconsistent paths, and " + routesWithBadLinks + " are missing links.");
		
		writer.close();
		
	}

}
