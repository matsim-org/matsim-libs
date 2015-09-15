package playground.toronto.transitnetworkutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;


/**
 * A class for 'guessing' route sequences of transit lines, based on shortest-path
 * between sequences of stops.
 * 
 * 
 * @author pkucirek
 */
public class MapMatching {

	private Config config;
	private Scenario scenario;
	private Network network;
	
	
	public MapMatching(String configFileName)  throws IOException{
		this.config = ConfigUtils.loadConfig(configFileName);
		this.scenario = ScenarioUtils.loadScenario(this.config);
		this.network = this.scenario.getNetwork();		
	}
	
	/**
	 * Designed to do the actual routing. It starts by filtering the base network by mode; it is
	 * designed for Toronto specifically, with the mode names hard coded.
	 * 
	 * Currently, exports to an .xml format AND and EMME .221 format. 
	 * 
	 * @param stopsFileName - the name of the file where stop locations and names are stored. It
	 * assumes that the file is formatted [StopID], [X], [Y], [LinkRef], [NodeRef]
	 * @param routes - an ArrayList of ScheduledRoute. Can be passed from ScheduleConverter
	 * @param outFileFolder - a String of the folder location to export to
	 * @throws IOException
	 */
	public void matchRoutes(String stopsFileName, ArrayList<ScheduledRoute> routes, String outFileFolder) throws IOException{
		
		//Load transit stops
		HashMap<String, String[]> stops = new HashMap<String, String[]>();
		BufferedReader stopsReader = new BufferedReader(new FileReader(stopsFileName)); 
		String fileLine = stopsReader.readLine();
		while (fileLine != null){
					
			String[] cells = fileLine.split(",");
			String id = cells[0];
			String[] properties = {cells[1], cells[2], cells[3], cells[4]};
			stops.put(id, properties);
					
			fileLine = stopsReader.readLine();
		}
		stopsReader.close();
				
				
				
			//TODO: Do I need to modify the network to include loops at stops?
				
			//filter the network by mode
			NetworkImpl BusNetwork = NetworkImpl.createNetwork(); //for buses
			NetworkImpl TrainNetwork = NetworkImpl.createNetwork(); //for GO trains
			NetworkImpl StreetcarNetwork = NetworkImpl.createNetwork(); //for mixed-ROW streetcars
			NetworkImpl SubwayNetwork = NetworkImpl.createNetwork(); //for underground heavy rail
			NetworkImpl SRTNetwork = NetworkImpl.createNetwork(); //for Scarborough RT
			NetworkImpl LRTNetwork = NetworkImpl.createNetwork(); //for dedicated-ROW streetcars
			//NetworkImpl GOBUSNetwork = NetworkImpl.createNetwork();
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(this.network);
			//filter.filter(GOBUSNetwork, CollectionUtils.stringToSet("GO_Bus"));
			filter.filter(LRTNetwork, CollectionUtils.stringToSet("LRT"));
			filter.filter(SRTNetwork, CollectionUtils.stringToSet("SRT"));
			filter.filter(SubwayNetwork, CollectionUtils.stringToSet("Subway"));
			filter.filter(StreetcarNetwork, CollectionUtils.stringToSet("Streetcar"));
			filter.filter(TrainNetwork, CollectionUtils.stringToSet("Train"));
			filter.filter(BusNetwork, CollectionUtils.stringToSet("Bus"));
			System.out.println("Base network contains " + this.network.getLinks().size() + " links.");
			//System.out.println("GO Bus network contains " + GOBUSNetwork.getLinks().size() + " links.");
			System.out.println("LRT network contains " + LRTNetwork.getLinks().size() + " links.");
			System.out.println("SRT network contains " + SRTNetwork.getLinks().size() + " links.");
			System.out.println("Subway network contains " + SubwayNetwork.getLinks().size() + " links.");
			System.out.println("Streetcar network contains " + StreetcarNetwork.getLinks().size() + " links.");
			System.out.println("Train network contains " + TrainNetwork.getLinks().size() + " links.");
			System.out.println("Bus network contains " + BusNetwork.getLinks().size() + " links.");
			System.out.println("Network filtering done.");
				
			//Node fromNode = this.network.getNodes().get(Id.create(stops.get(stopSequence.get(0))[3]));
			//Path linkSequence = pather.calcLeastCostPath(fromNode, toNode, 0F);
			
		BufferedWriter xmlWriter = new BufferedWriter(new FileWriter(outFileFolder + "\\linksequences.xml"));
		xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<routes>\n");
		BufferedWriter EMMEWriter = new BufferedWriter(new FileWriter(outFileFolder + "\\routeseq.221"));
		EMMEWriter.write("t lines");
		int routeNumber = 0; //counter for a route number, for EMME
			
			for (ScheduledRoute route : routes){
				ArrayList<String> stopSequence = route.getStopSequence();
				String xmlLinkSequence = "\t<route routename=\"" + route.routename + "\" direction=\"" + 
						route.direction + "\" branch=\"" + route.branch +"\" mode=\""+ route.mode + "\">\n\t\t<linksequence>";
				String EMMESequence = "a \'G_"+ route.mode.charAt(0) + routeNumber++ + "\'";
				int EMMEColumnNumber = 0; //The EMME input format only accepts 8 columns.
				
				//pather uses a SimpleTravelTimeCalculator to produce travel times and costs based on freeflow speed.
				LeastCostPathCalculator pather = null;
				Network subNetwork = null;
				
				FreeSpeedTravelTime freespeedCalc = new FreeSpeedTravelTime();
				OnlyTimeDependentTravelDisutility disutilityCalc = new OnlyTimeDependentTravelDisutility(freespeedCalc);
				
				if (route.mode == "train"){					
					pather = new Dijkstra(TrainNetwork, disutilityCalc, freespeedCalc);
					// line#   mode   veh   hdwy   speed   descr   ut1   ut2   ut3
					EMMESequence += " r 1 5 99";
					subNetwork = TrainNetwork;
				}
				else if (route.mode == "subway"){
					pather = new Dijkstra(SubwayNetwork,disutilityCalc,freespeedCalc);
					EMMESequence += " m 2 5 99";
					subNetwork = SubwayNetwork;
				}
				else if (route.mode == "streetcar"){
					pather = new Dijkstra(StreetcarNetwork, disutilityCalc, freespeedCalc);
					EMMESequence += " l 5 5 99";
					subNetwork = StreetcarNetwork;
				}
				else { //default mode is bus
					pather = new Dijkstra(BusNetwork,disutilityCalc, freespeedCalc);
					EMMESequence += " b 7 5 99";
					subNetwork = BusNetwork;
				}
				EMMESequence += " \'" + route.routename + " " + route.direction.toCharArray()[0] + route.branch + "\'\n";
				//EMMESequence += " path=no";
				//EMMEColumnNumber++;
				
				
				Path P = null;
				Node fromNode = getNearestNode(new Coord(Double.parseDouble(stops.get(stopSequence.get(0))[0]), Double.parseDouble(stops.get(stopSequence.get(0))[1])), subNetwork);
				EMMESequence += " " + fromNode.getId().toString(); EMMEColumnNumber++;
				for (int i = 1; i < stopSequence.size(); i++){
					
					//TODO create VIA (waypoint) nodes and write code to handle them.
					
					Node toNode = null;
					try{
						toNode = getNearestNode(new Coord(Double.parseDouble(stops.get(stopSequence.get(i))[0]), Double.parseDouble(stops.get(stopSequence.get(i))[1])), subNetwork);
					} catch (java.lang.NullPointerException e) {
						continue;
					}
					
					Person person = PersonImpl.createPerson(Id.create("transit driver", Person.class));
					VehicleImpl veh = new VehicleImpl(Id.create("test vehicle", Vehicle.class), new VehicleTypeImpl(Id.create("no type", VehicleType.class)));
					
					P = pather.calcLeastCostPath(fromNode, toNode, 0F, person, veh);
					
					/*
					if(P == null){ //catch 'no path', and try with 'bus' network.
						LeastCostPathCalculator baseNtwkPather = new Dijkstra(BusNetwork, new SimpleTravelTimeCalculator(), new SimpleTravelTimeCalculator());
						P = baseNtwkPather.calcLeastCostPath(fromNode, fromNode, 0F);
						System.out.println("Path not found from node " + fromNode.getId().toString() + " to node " + toNode.getId().toString() + ", for route \"" + route.id + 
								"\". Trying on bus network instead.");
					}*/
					
					
					for (Link L : P.links){
						xmlLinkSequence += "\n\t\t\t<link refId=\"" + L.getId().toString() + "\"/>";
						
						if(EMMEColumnNumber > 8){
							EMMESequence += "\n " + L.getToNode().getId().toString();
							EMMEColumnNumber = 1;
						}
						else{
							EMMESequence += " " + L.getToNode().getId().toString();
							EMMEColumnNumber++;
						}
						
					}
					
					fromNode = toNode;
					
				}
				xmlLinkSequence += "\n\t\t</linksequence>\n</route>\n";
				xmlWriter.write(xmlLinkSequence);
				
				EMMEWriter.write("\n" + EMMESequence);
					
				System.out.println("Route \"" + route.id + "\" is done.");
						
			}
			xmlWriter.write("</routes>");
			xmlWriter.close();
			EMMEWriter.close();
	}
	
		
	/**
	 * Searches through the nodes in a network for the nearest node.
	 * 
	 * @param point
	 * @param network
	 * @return
	 */
	private Node getNearestNode(Coord point, Network network){
		
		//final Double searchRadius = 5000.0; //search radius, in meters
		Double minDistannce = Double.MAX_VALUE;
		Node result = null;
		
		for (Node n : network.getNodes().values()){
			// Simple pythagorean distance calculation
			double distance = Math.sqrt(Math.pow(point.getX() - n.getCoord().getX(), 2) + Math.pow(point.getY() - n.getCoord().getY(), 2));
			
			if (distance < minDistannce) { 
				minDistannce = new Double(distance);
				result = n;
			}
		}
		
		return result;
		
	}
	

	
}
