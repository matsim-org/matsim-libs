package playground.toronto.gtfsutils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.toronto.demand.util.TableReader;


public class CreateScheduleToMatchPseudoNetwork{
	

	public static void main(String args[]) throws FileNotFoundException, IOException{
		
		String ScheduleFile = args[0];
		String StopsFile = args[1];
		String OutputFile = args[2];
		String NetworkFile = args[3];
		String NetworkPrefix = "1";
		
		HashMap<Id<Node>,Coord> StopAndCoordinates = new HashMap<Id<Node>,Coord>();
		HashMap<Tuple<Id<Node>,Id<Node>>,Id<Link>> Link_Nodes = new HashMap<Tuple<Id<Node>,Id<Node>>,Id<Link>>();
		//List<Tuple<TransitLine, TransitRoute>> toBeRemoved = new LinkedList<Tuple<TransitLine, TransitRoute>>();
		ArrayList<TransitRoute> RemoveRoute = new ArrayList<TransitRoute>();
    
		
		//read the stops file
		 
		TableReader rdStops = new TableReader(StopsFile);
		rdStops.open();
		rdStops.ignoreTrailingBlanks(true);
		
		while (rdStops.next()){
			Id<Node> StopID = Id.create(rdStops.current().get("stop_id").toString(), Node.class);
			Coord StopCoord = new Coord(Double.parseDouble(rdStops.current().get("X").toString()), Double.parseDouble(rdStops.current().get("Y").toString()));
			StopAndCoordinates.put(StopID, StopCoord);	
		}//end of while loop
		
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//read network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NetworkFile);
		
		//read schedule
		TransitScheduleFactoryImpl builder = new TransitScheduleFactoryImpl();
		TransitScheduleImpl Schedule = (TransitScheduleImpl) builder.createTransitSchedule();
		TransitScheduleReaderV1 tsreader = new TransitScheduleReaderV1(Schedule, new RouteFactoryImpl());
		tsreader.readFile(ScheduleFile);

		//pseudonetwork to use the getnetworkstop method
		CreateNetworkFromTransitSchedule PseudoNetwork = new CreateNetworkFromTransitSchedule(Schedule, network, NetworkPrefix);
		
		/*for (Link link : network.getLinks().values()){
			Id LinkId = link.getId();
			Id fromNodeId = link.getFromNode().getId();
			Id toNodeId = link.getToNode().getId();
			Link_Nodes.put(new Tuple<Id,Id>(fromNodeId,toNodeId),LinkId);
		}
		*/
		for (TransitLine tLine : Schedule.getTransitLines().values()){
			//for each route of a line
			//for each route of a line
			for (TransitRoute tRoute : tLine.getRoutes().values()){
				//reset the links for the route
				ArrayList<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
				TransitRouteStop prevStop =  tRoute.getStops().get(0);
				for (int i = 1; i < tRoute.getStops().size();i++) {
					TransitRouteStop stop = tRoute.getStops().get(i);
					Link link = PseudoNetwork.getNetworkStop(prevStop, stop, StopAndCoordinates);
					//Tuple<Id,Id> Nodes = new Tuple<Id,Id>(prevStop.getStopFacility().getId(), stop.getStopFacility().getId());
					//System.out.println(Nodes);
					//Id link = Link_Nodes.get(Nodes);
					/*if (link == null){
						System.out.println(link);
					}*/
					routeLinks.add(link.getId());
					prevStop = stop;
				}
				Id startLinkId = routeLinks.get(0);
				Id endLinkId = routeLinks.get(((routeLinks.size())-1));
				if (routeLinks.size()>=3){
					routeLinks.remove(0);
					int s = routeLinks.size();
					routeLinks.remove(s-1);
					tRoute.getRoute().setLinkIds(startLinkId, routeLinks, endLinkId);
				}
				else{
					//toBeRemoved.add(new Tuple<TransitLine, TransitRoute>(tLine, tRoute));
					RemoveRoute.add(tRoute);
				}
			}
			for (int i = 0; i< RemoveRoute.size(); i++){
				tLine.removeRoute(RemoveRoute.get(i));
			}
		}
		
		TransitScheduleWriterV1 tswriter = new TransitScheduleWriterV1(Schedule);
		tswriter.write(OutputFile);
			
	}
}