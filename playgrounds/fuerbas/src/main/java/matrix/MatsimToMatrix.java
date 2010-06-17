package matrix;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;

import playground.johannes.socialnetworks.graph.matrix.EdgeCostFunction;
import playground.johannes.socialnetworks.graph.matrix.MatrixCentrality;
import playground.johannes.socialnetworks.graph.matrix.WeightedDijkstraFactory;


public class MatsimToMatrix {
	
	@SuppressWarnings("unchecked")
	public static void main(String args[]) {
	
	Scenario scenario = new ScenarioImpl();
	NetworkLayer network = (NetworkLayer) scenario.getNetwork();
	new MatsimNetworkReader(scenario).readFile(args[0]);
	
	final List<Node> nodeList = new ArrayList<Node>(network.getNodes().size());
	final List<Link> linkList = new ArrayList<Link>(network.getLinks().size());
	final int[][] linkIdxFromTo = new int[network.getLinks().size()][network.getLinks().size()];
	
	AdjacencyMatrix y = new AdjacencyMatrix();
	
	for (Node node : network.getNodes().values()) {
		int idx = y.addVertex();
//		System.out.println(idx+node.toString());
		nodeList.add(idx, node);
//		System.out.println(nodeList.get(idx));
	}
	
	int linkIdx = 0;
	for (Link link : network.getLinks().values()) {
		if (nodeList.contains(link.getFromNode())) {
			y.addEdge(nodeList.indexOf(link.getFromNode()), nodeList.indexOf(link.getToNode()));
			linkList.add(linkIdx, link);
			linkIdxFromTo[nodeList.indexOf(link.getFromNode())][nodeList.indexOf(link.getToNode())] = linkIdx;
			linkIdx++;
//			System.out.println(linkIdx);
		}			
	}
	
//	for (int i=0; i<100; i++){
//		for (int j=0; j<100; j++)
//			System.out.println(linkIdxFromTo[i][j]);
//	}
	
	class EdgeCost implements EdgeCostFunction {

		private double costs;
		@Override
		public double edgeCost(int i, int j) {
			int linkIndex = linkIdxFromTo[i][j];  
			costs=(linkList.get(linkIndex).getLength() / linkList.get(linkIndex).getFreespeed());		
			return costs;
		}
		
	}
	
	EdgeCost edgeCost = new EdgeCost();
	
	WeightedDijkstraFactory factory = new WeightedDijkstraFactory(edgeCost);
	MatrixCentrality MatrixCent = new MatrixCentrality();
	MatrixCent.setDijkstraFactory(factory);
	MatrixCent.run(y);
	
	}
	
	

}
