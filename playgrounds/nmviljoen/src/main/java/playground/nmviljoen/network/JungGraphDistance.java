package playground.nmviljoen.network;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections15.Transformer;

import playground.nmviljoen.network.MyDirectedGraphCreatorVer2.MyLink;
import playground.nmviljoen.network.MyDirectedGraphCreatorVer2.MyNode;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.shortestpath.Distance;
import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.graph.DirectedGraph;
public class JungGraphDistance {
	public static void calculateAndWriteUnweightedDistances(DirectedGraph<MyNode,MyLink> myGraph, String distUnweightedFile){
		double diameterUnweighted = DistanceStatistics.diameter(myGraph);
		Transformer<MyNode, Double> avgDistTransformer = DistanceStatistics.averageDistances(myGraph);
		Map<String, Double> distanceMap = new HashMap<String, Double>();
		for(MyNode node : myGraph.getVertices()){
			double dist = avgDistTransformer.transform(node); 
			distanceMap.put(node.getId(), dist);
		}
		try {
			File fileClust = new File(distUnweightedFile);
			FileWriter fw = new FileWriter(fileClust.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("Graph unweighted diameter = "+diameterUnweighted);
			bw.newLine();
			for (String id : distanceMap.keySet()){
				bw.write(String.format("%s,%f", id, distanceMap.get(id)));
				bw.newLine();
			}
			bw.close();
			System.out.println("Unweighted distance file written");
		} catch (IOException e) {
			e.printStackTrace();
		}
		distanceMap = null;

	}
	public static void calculateAndWriteWeightedDistances(DirectedGraph<MyNode,MyLink> myGraph, String distWeightedFile){

		
//		//Don't know how to use this to add weight
////		Distance<MyNode> d = new Distance<MyNode>();
////				{
////			public Double transform(MyLink link){
////				return link.getWeight();
////			}
////		};
////		double diameterWeighted = DistanceStatistics.diameter(myGraph,d);
//		Transformer<MyNode, Double> avgDistTransformer = DistanceStatistics.averageDistances(myGraph);
//		Map<String, Double> distanceMap = new HashMap<String, Double>();
//		for(MyNode node : myGraph.getVertices()){
//			double dist = avgDistTransformer.transform(node); 
//			distanceMap.put(node.getId(), dist);
//		}
//		try {
//			File fileClust = new File(distWeightedFile);
//			FileWriter fw = new FileWriter(fileClust.getAbsoluteFile());
//			BufferedWriter bw = new BufferedWriter(fw);
//			bw.write("Graph unweighted diameter = "+diameterUnweighted);
//			bw.newLine();
//			for (String id : distanceMap.keySet()){
//				bw.write(String.format("%s,%f", id, distanceMap.get(id)));
//				bw.newLine();
//			}
//			bw.close();
//			System.out.println("Unweighted distance file written");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
	}

}
