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
	
	System.out.println("EINLESEN DER MATRIX KOMPLETT"+Runtime.getRuntime().freeMemory());
	
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
	
	System.out.println("LINK ADDIEREN KOMPLETT"+Runtime.getRuntime().freeMemory());
	
//	for (int i=0; i<100; i++){
//		for (int j=0; j<100; j++)
//			if (linkIdxFromTo[i][j]>0){
//			System.out.println(linkIdxFromTo[i][j]);
//			System.out.println(linkList.get(linkIdxFromTo[i][j]).getFreespeed());
//			}
//	}
	
	System.out.println("EDGE ADDIEREN KOMPLETT"+Runtime.getRuntime().freeMemory());
	
	class EdgeCost implements EdgeCostFunction {

		private double costs;
		@Override
		public double edgeCost(int i, int j) {
			int linkIndex = linkIdxFromTo[i][j];  
			costs=(linkList.get(linkIndex).getLength()/linkList.get(linkIndex).getFreespeed());		
			return costs;
		}
		
	}
	
	EdgeCost edgeCost = new EdgeCost();
	System.out.println("EDGE COST INIT"+Runtime.getRuntime().freeMemory());
	WeightedDijkstraFactory factory = new WeightedDijkstraFactory(edgeCost);
	System.out.println("WEIGHTED DIJKSTRA INIT"+Runtime.getRuntime().freeMemory());
	MatrixCentrality MatrixCent = new MatrixCentrality();
	System.out.println("MATRIX CENT INIT"+Runtime.getRuntime().freeMemory());
	MatrixCent.setDijkstraFactory(factory);
	System.out.println("MATRIX CENT SET FACTORY"+Runtime.getRuntime().freeMemory());
	MatrixCent.run(y);
	System.out.println("MATRIX CENT RUN CPLT"+Runtime.getRuntime().freeMemory());
	}
	
	

}
