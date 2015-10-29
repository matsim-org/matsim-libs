package playground.artemc.scenarioTools;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class NodeDistances {
	
	private MutableScenario scenario;
	private Network network;
	public Double[][] distances;
	
	
	public NodeDistances(String networkPath){
	
		scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(networkPath); 
		network = scenario.getNetwork();
		Map<Id<Node>, ? extends Node> nodes = network.getNodes();
		
		distances = new Double[nodes.size()][nodes.size()];
		
		for(Id node1:nodes.keySet()){
			Coord node1Coord = nodes.get(node1).getCoord();	
			for(Id node2:nodes.keySet()){
				Coord node2Coord = nodes.get(node2).getCoord();
				Double nodeDistance = Math.sqrt((node1Coord.getX()-node2Coord.getX())*(node1Coord.getX()-node2Coord.getX())+(node1Coord.getY()-node2Coord.getY())*(node1Coord.getY()-node2Coord.getY()));
				
				distances[Integer.valueOf(node1.toString())-1][Integer.valueOf(node2.toString())-1] = nodeDistance;
			}	
		}
		
		
	}
	
	public HashMap<Integer, Double> getDistanceMapForNode(String node){
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		Integer column = 1;
		for(Double d:this.distances[Integer.valueOf(node)-1]){
			map.put(column, d);
			column++;
		}
		return map;
	}
	

	
	public static void main(String[] args) {
		String networkPath = args[0];
		NodeDistances n = new NodeDistances(networkPath);
		
		for(Double[] row:n.distances){
			for(Double dist:row){
				System.out.print(dist+",");
			}
			System.out.println();
		}
	
	}
}
