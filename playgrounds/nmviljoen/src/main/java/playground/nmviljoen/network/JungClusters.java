package playground.nmviljoen.network;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import edu.uci.ics.jung.algorithms.metrics.Metrics;
import edu.uci.ics.jung.algorithms.metrics.TriadicCensus;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;

public class JungClusters {
	public static void calculateAndWriteClusteringCoefficient(DirectedGraph<NmvNode,NmvLink> myGraph, String clusterFile){
		final Map<NmvNode,Double> clustCoeffMap;
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
	public static void calculateAndWriteWeakComponents(DirectedGraph<NmvNode,NmvLink> myGraph, String weakCompFile){
		Set<Set<NmvNode>> clusterSet = new HashSet<Set<NmvNode>>();
		WeakComponentClusterer< NmvNode, NmvLink> weakComps = new WeakComponentClusterer();
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
				Set<NmvNode> thisCluster = (Set<NmvNode>) iter.next();
				Iterator nodeIter = thisCluster.iterator();
				while(nodeIter.hasNext()){
					NmvNode thisNode = (NmvNode) nodeIter.next();
					bw.write(String.format("%d,%s,%s,%s\n", cluster, thisNode.getId(), thisNode.getXAsString(), thisNode.getYAsString()));
				}
				cluster++;
			}
			bw.close();
			System.out.println("Weak component file written");

		} catch (IOException e) {
			e.printStackTrace();
		}
				
		
	}
	public static void calculateAndWriteTriadicCensus(DirectedGraph<NmvNode,NmvLink> myGraph, String triadFile){
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
