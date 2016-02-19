package playground.toronto.gtfsutils;

/**
 * still under development
 */

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
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.toronto.demand.util.TableReader;

public class CreateNetworkFromGTFS{
	
	public static void main(String args[]) throws NumberFormatException, IOException{
		//String ScheduleFile = args[0];
		String NetworkFile = args [0];
		String StopsFile = args[1];
		String StopSequenceFile = args[2];
		//String NetworkPrefix = "17";
		//String NetworkOutput = args[3];
		
        HashMap<Id<Node>,Coord> StopAndCoordinates = new HashMap<>();
        HashMap<Id<Link>,Link> RemoveLinks = new HashMap<>();
        
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NetworkFile);
		
		/*read the stops file
		 * create nodes for each stop
		 */
		TableReader rdStops = new TableReader(StopsFile);
		rdStops.open();
		rdStops.ignoreTrailingBlanks(true);
		
		while (rdStops.next()){
			Id<Node> StopID = Id.create(rdStops.current().get("stop_id").toString(), Node.class);
			Coord StopCoord = new Coord(Double.parseDouble(rdStops.current().get("stop_lon").toString()), Double.parseDouble(rdStops.current().get("stop_lat").toString()));
			network.createAndAddNode(StopID, StopCoord);
			StopAndCoordinates.put(StopID, StopCoord);	
		}//end of while loop
		
		rdStops.close();
		
	   
		/*read the stop sequence file
		 * 
		 */
		
		TableReader rdStopSequence = new TableReader(StopSequenceFile);
		rdStopSequence.open();
		rdStopSequence.ignoreTrailingBlanks(true);
		
		int prevStop = 1;
		int nextStop = 2;
		
		Node fromNode;
		Node toNode;
		Link link;
		//arbitrary speed capacity and lanes
		double linklength;
		double linkspeed = (90.0/3.6);
		double linkcapacity = 9999;
		double numlanes = 1;
		ArrayList<Id<Link>> ListofLinks = new ArrayList<>();
		
		
		while (rdStopSequence.next()){
			prevStop = 1;
			nextStop = 2;
			while (rdStopSequence.current().containsKey(""+nextStop) && rdStopSequence.current().get(""+nextStop).isEmpty()!= true){
				//System.out.println(""+prevStop);
				//System.out.println(""+nextStop);
				Id<Node> fromNodeID = Id.create(rdStopSequence.current().get(""+prevStop).toString(), Node.class);
				Id<Node> toNodeID = Id.create(rdStopSequence.current().get(""+nextStop).toString(), Node.class);
				//System.out.println(fromNodeID);
				//System.out.println(toNodeID);
				fromNode = network.getFactory().createNode(fromNodeID, StopAndCoordinates.get(fromNodeID));
				toNode = network.getFactory().createNode(toNodeID, StopAndCoordinates.get(toNodeID));
				//System.out.println(fromNode);
				//System.out.println(toNode);
				Id<Link> linkID = Id.create(fromNodeID.toString()+"_"+toNodeID.toString(), Link.class);
				if (ListofLinks.contains(linkID)!=true){
					//System.out.println(linkID);
					linklength = CoordUtils.calcEuclideanDistance(fromNode.getCoord(),toNode.getCoord());
					//System.out.println(linklength);
					if (linklength <= 2){
						network.createAndAddLink(linkID, fromNode, toNode, linklength,linkspeed, linkcapacity, numlanes);
						ListofLinks.add(linkID);
						//System.out.println(ListofLinks);
					}
				}
				prevStop++;
				//System.out.println(prevStop);
				nextStop++;	
				//System.out.println(nextStop);
			}
			
		}
		
		//TransitScheduleReaderV1 ReadSchedule = new TransitScheduleReaderV1(scenario);
		
		//TransitScheduleFactoryImpl builder = new TransitScheduleFactoryImpl();
		//TransitScheduleImpl Schedule = (TransitScheduleImpl) builder.createTransitSchedule();
		
	
		//what is moderoute factory thing?
		//TransitScheduleReaderV1 tsreader = new TransitScheduleReaderV1(Schedule, new ModeRouteFactory(), scenario);
		//tsreader.readFile(ScheduleFile);
		
		//CreateNetworkFromTransitSchedule PseudoNetwork = new CreateNetworkFromTransitSchedule(Schedule, network, NetworkPrefix);
		//PseudoNetwork.createNetwork(StopAndCoordinates);
		
		//remove express links
		/*for (Link link : network.getLinks().values()){
			if (link.getLength()>2000){
			   RemoveLinks.put(link.getId(),link);
			}
		}
		for (Link link : RemoveLinks.values()){
			network.removeLink(link.getId());
		}*/
		
		new NetworkWriter(network).write(NetworkFile);
	}

}