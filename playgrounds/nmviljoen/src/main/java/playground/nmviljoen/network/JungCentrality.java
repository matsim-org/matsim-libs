package playground.nmviljoen.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.apache.commons.collections15.Transformer;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import playground.nmviljoen.network.MyDirectedGraphCreatorVer2.MyLink;
import playground.nmviljoen.network.MyDirectedGraphCreatorVer2.MyNode;
import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.algorithms.util.*;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.algorithms.util.ConstantMap;
import edu.uci.ics.jung.algorithms.util.Indexer;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.event.GraphEvent.Vertex;

public class JungCentrality {
	public static void calculateAndWriteUnweightedCloseness(DirectedGraph<MyNode,MyLink> myGraph, String nodeCloseUnweighted,
			ArrayList<MyNode> nodeList) {
		ClosenessCentrality<MyNode, MyLink> ranker = new ClosenessCentrality<MyNode, MyLink>(myGraph);
			BufferedWriter bw = IOUtils.getBufferedWriter(nodeCloseUnweighted);
			try{
				bw.write("NodeID,X,Y,Long,Lat,C_C");
				bw.newLine();
				for(int i=0;i<nodeList.size();i++)
				{
					bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));

				}
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't write to file.");
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
//					LOG.error("Oops, couldn't close");
				}
			}
			System.out.println("Unweighted node Closeness written to file");
		}
			
	public static void calculateAndWriteWeightedCloseness(DirectedGraph<MyNode,MyLink> myGraph, String nodeCloseWeighted,
			ArrayList<MyNode> nodeList) {
		Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>(){
			public Double transform(MyLink link){
				return link.getWeight();
			}
		};
		ClosenessCentrality<MyNode, MyLink> ranker = new ClosenessCentrality<MyNode, MyLink>(myGraph, wtTransformer);
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeCloseWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_C");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("Weighted node Closeness written to file");
	}
		
	public static void calculateAndWriteUnweightedBetweenness(DirectedGraph<MyNode,MyLink> myGraph,String nodeBetUnweighted,
			String edgeBetUnweighted, ArrayList<MyNode> nodeList,
			LinkedList<MyLink> linkList) {
		BetweennessCentrality<MyNode, MyLink> ranker = new BetweennessCentrality<MyNode, MyLink>(myGraph);
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeBetUnweighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_B");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("Unweighted Node Betweenness written to file");
	}

	public static void calculateAndWriteWeightedBetweenness(DirectedGraph<MyNode,MyLink> myGraph,String nodeBetWeighted,
			String edgeBetWeighted, ArrayList<MyNode> nodeList,
			LinkedList<MyLink> linkList) {
		Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>(){
			public Double transform(MyLink link){
				return link.getWeight();
			}
		};
		BetweennessCentrality<MyNode, MyLink> ranker = new BetweennessCentrality<MyNode, MyLink>(myGraph,wtTransformer);
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeBetWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_B");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("Weighted Edge Betweenness written to file");
	}

	public static void calculateAndWriteUnweightedEigenvector(DirectedGraph<MyNode,MyLink> myGraph, String nodeEigenUnweighted,
			ArrayList<MyNode> nodeList) {
		EigenvectorCentrality<MyNode, MyLink> ranker = new EigenvectorCentrality<MyNode, MyLink>(myGraph);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeEigenUnweighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_E");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.10f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("Unweighted Eigenvector written to file");
	}
	
	public static void calculateAndWriteWeightedEigenvector(DirectedGraph<MyNode,MyLink> myGraph, String nodeEigenWeighted,
			ArrayList<MyNode> nodeList,final LinkedList<MyLink> linkList) {

		//Create adjacency graph with edgeWeights as values
		SparseDoubleMatrix2D adj;
		adj=nadiaGraphToSparseMatrix(myGraph, nodeList);

		//Calculate row sums of the matrix
		double[] rowsums = new double[nodeList.size()];
		for (int t = 0; t<nodeList.size();t++){
			for (int r = 0; r<nodeList.size();r++){
				rowsums[t] = rowsums[t]+adj.getQuick(t, r);
			}
			
		}
		//Create the Transition Probability Matrix
		double[][] transProb = new double[nodeList.size()][nodeList.size()];
		for (int p = 0; p<nodeList.size();p++){
			for (int y=0;y<nodeList.size();y++){
				transProb[p][y] = adj.getQuick(p, y)/rowsums[p];
			}
		}
		
		//Set the transition probabilities
		for (int i = 0; i<nodeList.size();i++){
			MyNode begin = nodeList.get(i);
			for (Iterator o_iter = myGraph.getOutEdges(begin).iterator(); o_iter.hasNext(); )
			{
				MyLink e = (MyLink) o_iter.next();
				if(e.getTransProb()==-99){
					MyNode end = myGraph.getOpposite(begin,e);
					int j = nodeList.indexOf(end);
					e.setTransProb(transProb[i][j]);
				}
			}
		}
		Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>(){
			public Double transform(MyLink link){
				return link.getTransProb();
			}
		};
		EigenvectorCentrality<MyNode, MyLink> ranker = new EigenvectorCentrality<MyNode, MyLink>(myGraph, wtTransformer);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeEigenWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_E");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.10f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));

			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("Weighted Eigenvector written to file");
	}
	public static void calculateAndWriteUnweightedPageRank(DirectedGraph<MyNode,MyLink> myGraph, String nodePageRankUnweighted,
			ArrayList<MyNode> nodeList) {
		double alpha = 0;
		PageRank<MyNode, MyLink> ranker = new PageRank<MyNode, MyLink>(myGraph,alpha);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodePageRankUnweighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_P");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.10f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("Unweighted PageRank written to file");
	}

	public static void calculateAndWriteWeightedPageRank(DirectedGraph<MyNode,MyLink> myGraph, String nodePageRankWeighted,
			ArrayList<MyNode> nodeList) {
		Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>(){
			public Double transform(MyLink link){
				return link.getWeight();
			}
		};
		EigenvectorCentrality<MyNode, MyLink> ranker = new EigenvectorCentrality<MyNode, MyLink>(myGraph, wtTransformer);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodePageRankWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_E");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.10f\n", nodeList.get(i).getId(),nodeList.get(i).getX(), nodeList.get(i).getY(),null,null,ranker.getVertexScore(nodeList.get(i))));

			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		System.out.println("Weighted PageRank written to file");
	}
	 public static SparseDoubleMatrix2D nadiaGraphToSparseMatrix(DirectedGraph<MyNode,MyLink> g, ArrayList<MyNode> nodeList)
	  {
		 int numVertices = g.getVertices().size();
	        SparseDoubleMatrix2D matrix = new SparseDoubleMatrix2D(numVertices,numVertices);
	        for (int i = 0; i < numVertices; i++)
	         {
	            MyNode v = nodeList.get(i);
	            
	             for (Iterator o_iter = g.getOutEdges(v).iterator(); o_iter.hasNext(); )
	             {
	                MyLink e = (MyLink) o_iter.next();
	                 MyNode w = g.getOpposite(v,e);
	                
	                //find it's position in nodeList
	                 int j = nodeList.indexOf(w);
	                 
	                 matrix.set(i, j, matrix.getQuick(i,j) + e.getWeight());
	             }
	         }
	        //the sequence of the matrix indices is the sequence of nodeList
	         return matrix;
	  }
		 

}

