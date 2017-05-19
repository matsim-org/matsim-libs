package playground.toronto.gtfsutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;

public class LinkStopsToNearestNode {

	public static void main(String[] args) throws IOException{
		
		String stopsFile = args[0];
		String networkInFile = args[1];
		String outputFolder = args[2];

		//Create base network
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = (Network) scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInFile);
		
		MutableScenario scenario2 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network noHighways = (Network) scenario.getNetwork();
		new MatsimNetworkReader(scenario2.getNetwork()).readFile(networkInFile);
		
		//Remove highways and on/off ramps. Should not affect routes which travel on highways since transit lines don't stop ON highways.	
		ArrayList<Id<Link>> linksToRemove = new ArrayList<>();
		for (Link l : noHighways.getLinks().values()){
			Link L = (Link) l;
			if (NetworkUtils.getType(L).equals("Highway") || NetworkUtils.getType(L).equals("Toll Highway") || NetworkUtils.getType(L).equals("On/Off Ramp")) linksToRemove.add(L.getId());
		}
		for (Id<Link> i : linksToRemove) noHighways.removeLink(i);
		
		// Create filtered networks, one for each of the four main modes.
		Network BusNetwork = NetworkUtils.createNetwork(); //for buses
		Network TrainNetwork = NetworkUtils.createNetwork(); //for GO trains
		Network StreetcarNetwork = NetworkUtils.createNetwork(); //for mixed-ROW streetcars
		Network SubwayNetwork = NetworkUtils.createNetwork(); //for underground heavy rail
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(noHighways);
		filter.filter(SubwayNetwork, CollectionUtils.stringToSet("Subway"));
		filter.filter(StreetcarNetwork, CollectionUtils.stringToSet("Streetcar"));
		filter.filter(TrainNetwork, CollectionUtils.stringToSet("Train"));
		filter.filter(BusNetwork, CollectionUtils.stringToSet("Bus"));
			
		BufferedWriter fixedStopsWriter = new BufferedWriter(new FileWriter(outputFolder + "/fixedStops.txt"));
		BufferedWriter exportForEsriWriter = new BufferedWriter(new FileWriter(outputFolder + "/stopLinks.txt"));
		exportForEsriWriter.write("stop_id,stop_x,stop_y,node_id,node_x,node_y\n");
		
		BufferedReader reader = new BufferedReader(new FileReader(stopsFile));
		String header = reader.readLine();
		int idCol = Arrays.asList(header.split(",")).indexOf("stop_id");
		int loncol = Arrays.asList(header.split(",")).indexOf("stop_lon");
		int latCol = Arrays.asList(header.split(",")).indexOf("stop_lat");
		int modCol = Arrays.asList(header.split(",")).indexOf("stop_modes"); //Optional column, not created in the original GTFS file but pre-processed by ValidateGTFS
		
		String line;
		ArrayList<Id> loopedNodes = new ArrayList<Id>();
		LinkFactory factory = NetworkUtils.createLinkFactory();
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			double stopLon = Double.parseDouble(cells[loncol]);
			double stopLat = Double.parseDouble(cells[latCol]);
			String id = cells[idCol];
			String modes = "";
			if (modCol != -1) {
				modes = cells[modCol];
			}
			
			Node N;
			fixedStopsWriter.write(id + "\n");
			
			// Get nearest node for the mode-filtered network (or base, if no mode is specified). This will be slow, but hopefully okay.
			if (modes.isEmpty()){
				N = (Node) NetworkUtils.getNearestNode(noHighways,new Coord(stopLon, stopLat));
			}
			else{

				Coord c = new Coord(stopLon, stopLat);
				
				if (modes.equals("[Bus]")){
					final Coord coord = c;
					N = (Node) NetworkUtils.getNearestNode(BusNetwork,coord);
				}
				else if (modes.equals("[Streetcar]")){
					final Coord coord = c;
					N = (Node) NetworkUtils.getNearestNode(StreetcarNetwork,coord);
				}
				else if (modes.equals("[Subway]")){
					final Coord coord = c;
					N = (Node) NetworkUtils.getNearestNode(SubwayNetwork,coord);
				}
				else if (modes.equals("[Train]")){
					final Coord coord = c;
					N = (Node) NetworkUtils.getNearestNode(TrainNetwork,coord);
				}
				else if (modes.equals("[Streetcar; Bus]") || modes.equals("[Bus; Streetcar]")){
					final Coord coord = c;
					N = (Node) NetworkUtils.getNearestNode(StreetcarNetwork,coord);
				}
				else{
					System.err.println("Error: mode combination " + modes + " not supported!");
					continue;
				}
				
			}
			
			if(!loopedNodes.contains(N.getId())){//Loop link DNE
				Link l = (Link) NetworkUtils.createLink(Id.create(N.getId() + "_LOOP", Link.class), N, N, network, 0.0, 10.0, 999.0, 1.0);
				NetworkUtils.setType( l, (String) "LOOP");
				
				/*if(!modes.isEmpty()){
					l.setAllowedModes(modes);
				}*/

				try {
					network.addLink(l);
				} catch (IllegalArgumentException e) {
					
				}
				loopedNodes.add(N.getId());
			}

			fixedStopsWriter.write(network.getLinks().get(Id.create(N.getId().toString() + "_LOOP", Link.class)).getId().toString() + "\n");
			exportForEsriWriter.write(id + "," + stopLon + "," + stopLat + "," + N.getId().toString() + "," + N.getCoord().getX() + "," + N.getCoord().getY() + "\n");
		}
		
		fixedStopsWriter.close();
		exportForEsriWriter.close();
		new NetworkWriter(network).write(outputFolder + "/new_network.xml");
		
	}
	
	
}
