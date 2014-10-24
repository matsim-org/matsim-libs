package playground.nmviljoen.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import playground.nmviljoen.network.MyDirectedGraphCreatorVer2.MyLink;
import playground.nmviljoen.network.MyDirectedGraphCreatorVer2.MyNode;
import edu.uci.ics.jung.algorithms.metrics.Metrics;
import edu.uci.ics.jung.algorithms.metrics.TriadicCensus;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;

public class JungClusters {
	public static void calculateAndWriteClusteringCoefficient(DirectedGraph<MyNode,MyLink> myGraph, String clusterFile){
		final Map<MyNode,Double> clustCoeffMap;
		clustCoeffMap= Metrics.clusteringCoefficients(myGraph);
//		int counter = 0;
		String nodeID ="";
		try {
			File fileClust = new File(clusterFile);
			FileWriter fw = new FileWriter(fileClust.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (Map.Entry entry : clustCoeffMap.entrySet()) {
//				counter++;
				bw.write(String.format("%s,%s", entry.getKey(),entry.getValue()));
				bw.newLine();
			}
			bw.close();
			System.out.println("Cluster file written");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void calculateAndWriteWeakComponents(DirectedGraph<MyNode,MyLink> myGraph, String weakCompFile){
		Set<Set<MyNode>> clusterSet = new HashSet<Set<MyNode>>();
		WeakComponentClusterer< MyNode, MyLink> weakComps = new WeakComponentClusterer();
		clusterSet = weakComps.transform(myGraph);
		Iterator iter = clusterSet.iterator();
		try {
			File fileClust = new File(weakCompFile);
			FileWriter fw = new FileWriter(fileClust.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			int cluster = 0;
			bw.write("Component#,NodeID,X,Y");
			bw.newLine();
			while (iter.hasNext()) {
				Set<MyNode> thisCluster = (Set<MyNode>) iter.next();
				Iterator nodeIter = thisCluster.iterator();
				while(nodeIter.hasNext()){
					MyNode thisNode = (MyNode) nodeIter.next();
					bw.write(String.format("%d,%s,%s,%s\n", cluster, thisNode.getId(), thisNode.getX(), thisNode.getY()));
				}
				cluster++;
			}
			bw.close();
			System.out.println("Weak component file written");

		} catch (IOException e) {
			e.printStackTrace();
		}
				
		
	}
	public static void calculateAndWriteTriadicCensus(DirectedGraph<MyNode,MyLink> myGraph, String triadFile){
		String[] triadConfig = new String[]{"","003","012","102","021D","021U","021C","111D","111U","030T","030C","201","120D","120U","120C","210","300"};
		int count;
		long[] triadArray;
		triadArray=TriadicCensus.getCounts(myGraph);
		try {
			File fileClust = new File(triadFile);
			FileWriter fw = new FileWriter(fileClust.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			// i = 0 is essentially a blank entry for TriadicCensus "(The 0th element is not meaningful; this array is effectively 1-based.)"
			bw.write("Index,Motif,Count");
			bw.newLine();
			for (int i=1;i<=16;i++){
				count=i+1;
				bw.write(String.format("%d,%s,%d",i,triadConfig[i],triadArray[i]));
				bw.newLine();
				}
			bw.close();
			System.out.println("Triadic census file written");

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
