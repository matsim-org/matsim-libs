package playground.nmviljoen.network;


import java.io.IOException;
import java.util.ArrayList;

import java.util.Iterator;
import java.util.LinkedList;

import java.io.BufferedWriter;


import org.matsim.core.utils.io.IOUtils;
import org.apache.commons.collections15.Transformer;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedGraph;


public class JungCentrality {
	public static void calculateAndWriteUnweightedCloseness(DirectedGraph<NmvNode,NmvLink> myGraph, String nodeCloseUnweighted,
			ArrayList<NmvNode> nodeList) {
		ClosenessCentrality<NmvNode, NmvLink> ranker = new ClosenessCentrality<NmvNode, NmvLink>(myGraph);
			BufferedWriter bw = IOUtils.getBufferedWriter(nodeCloseUnweighted);
			try{
				bw.write("NodeID,X,Y,Long,Lat,C_C");
				bw.newLine();
				for(int i=0;i<nodeList.size();i++)
				{
					bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", 
							nodeList.get(i).getId(),
							nodeList.get(i).getXAsString(), 
							nodeList.get(i).getYAsString(),
							null,
							null,
							ranker.getVertexScore(nodeList.get(i))));

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
			ranker = null;
		}
			
	public static void calculateAndWriteWeightedCloseness(DirectedGraph<NmvNode,NmvLink> myGraph, String nodeCloseWeighted,
			ArrayList<NmvNode> nodeList) {
		Transformer<NmvLink, Double> wtTransformer = new Transformer<NmvLink,Double>(){
			public Double transform(NmvLink link){
				return link.getWeight();
			}
		};
		ClosenessCentrality<NmvNode, NmvLink> ranker = new ClosenessCentrality<NmvNode, NmvLink>(myGraph, wtTransformer);
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeCloseWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_C");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", 
						nodeList.get(i).getId(),
						nodeList.get(i).getXAsString(), 
						nodeList.get(i).getYAsString(),
						null,
						null,
						ranker.getVertexScore(nodeList.get(i))));
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
		ranker = null;
	}
		
	public static void calculateAndWriteUnweightedBetweenness(DirectedGraph<NmvNode,NmvLink> myGraph,String nodeBetUnweighted,
			String edgeBetUnweighted, ArrayList<NmvNode> nodeList,
			LinkedList<NmvLink> linkList) {
		BetweennessCentrality<NmvNode, NmvLink> ranker = new BetweennessCentrality<NmvNode, NmvLink>(myGraph);
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeBetUnweighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_B");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", 
						nodeList.get(i).getId(),
						nodeList.get(i).getXAsString(), 
						nodeList.get(i).getYAsString(),
						null,
						null,
						ranker.getVertexScore(nodeList.get(i))));
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
		ranker = null;
	}

	public static void calculateAndWriteWeightedBetweenness(DirectedGraph<NmvNode,NmvLink> myGraph,String nodeBetWeighted,
			String edgeBetWeighted, ArrayList<NmvNode> nodeList,
			LinkedList<NmvLink> linkList) {
		Transformer<NmvLink, Double> wtTransformer = new Transformer<NmvLink,Double>(){
			public Double transform(NmvLink link){
				return link.getWeight();
			}
		};
		BetweennessCentrality<NmvNode, NmvLink> ranker = new BetweennessCentrality<NmvNode, NmvLink>(myGraph,wtTransformer);
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeBetWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_B");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.5f\n", 
						nodeList.get(i).getId(),
						nodeList.get(i).getXAsString(), 
						nodeList.get(i).getYAsString(),
						null,
						null,
						ranker.getVertexScore(nodeList.get(i))));
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
		ranker = null;
	}

	public static void calculateAndWriteUnweightedEigenvector(DirectedGraph<NmvNode,NmvLink> myGraph, String nodeEigenUnweighted,
			ArrayList<NmvNode> nodeList) {
		EigenvectorCentrality<NmvNode, NmvLink> ranker = new EigenvectorCentrality<NmvNode, NmvLink>(myGraph);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeEigenUnweighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_E");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.10f\n", 
						nodeList.get(i).getId(),
						nodeList.get(i).getXAsString(), 
						nodeList.get(i).getYAsString(),
						null,
						null,
						ranker.getVertexScore(nodeList.get(i))));
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
		ranker = null;
	}
	
	public static void calculateAndWriteWeightedEigenvector(DirectedGraph<NmvNode,NmvLink> myGraph, String nodeEigenWeighted,
			ArrayList<NmvNode> nodeList,final LinkedList<NmvLink> linkList) {

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
				//this didn't fix it - but maybe because I haven't fixed the transProb matrix yet.
				double roundOff = (double) Math.round((adj.getQuick(p, y)/rowsums[p]) * 1000000) / 1000000;
				transProb[p][y] = roundOff;
//				transProb[p][y]=(adj.getQuick(p, y)/rowsums[p]);
			}
		}
		
		//TESTING TRANSPROB ADD TO 1
		
		double[] transSums = new double[nodeList.size()];
		for (int p = 0; p<nodeList.size();p++){
			for (int y=0;y<nodeList.size();y++){
				transSums[p] = transSums[p]+transProb[p][y];
			}
		}
		// 	I BROKE THIS AND NOT SURE HOW.... i think it's fixed now
		for (int test=0;test<nodeList.size();test++){
			if(transSums[test]!=1){
				//				System.out.println("An transSum wasn't == 1");
//				boolean s = true;

				int small = 0;
				int big = 0;
				if(transSums[test]<1){
					for (int count=0;count<nodeList.size();count++){
						if (transProb[test][count]<transProb[test][small]){
							small = count;
						}

					}
					transProb[test][small]=transProb[test][small]+(1-transSums[test]);//add the missing fraction to the first non-zero element
				}else{
					for (int count=0;count<nodeList.size();count++){
						if (transProb[test][count]>transProb[test][big]){
							big = count;
						}
					}
					transProb[test][big]=transProb[test][big]-(transSums[test]-1);//add the missing fraction to the first non-zero element
				}
			}
		}
					
//					while (s==true){
//						
//							transProb[test][count]=transProb[test][count]+(1-transSums[test]);//add the missing fraction to the first non-zero element
////							System.out.println("Added the missing bit");
//							s=false;
//						}else count++;
					
//				}else{
//					if (transProb[test][count]!=0){
//						transProb[test][count]=transProb[test][count]-(transSums[test]-1);//take the additional fraction from the first non-zero element
////						System.out.println("Took away the extra bit");
//						s=false;
//					}else count++;
//				}
//			}
//		}
		double[] transSumsNew = new double[nodeList.size()];
		for (int p = 0; p<nodeList.size();p++){
			for (int y=0;y<nodeList.size();y++){
				transSumsNew[p] = transSumsNew[p]+transProb[p][y];
			}
		}
		
		BufferedWriter btrans = IOUtils.getBufferedWriter("/Users/nadiaviljoen/Desktop/transProb_WestRand_big.csv");
		try{
			for (int i=0;i<nodeList.size();i++){
				btrans.write(String.format("%d", i));
				btrans.write(",");
			}
			btrans.newLine();
			for (int row=0;row<nodeList.size();row++){
				btrans.write(String.format("%d", row));
				btrans.write(",");
				for (int col=0; col<nodeList.size();col++){
					btrans.write(String.format("%.8f", transProb[row][col]));
					btrans.write(",");
				}
				btrans.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				btrans.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		
//		//wite nodelist to file
//		BufferedWriter bnode = IOUtils.getBufferedWriter("/Users/nadiaviljoen/Desktop/nodeList_WestRand_big.csv");
//		try{
//			String dummy;
//			for (int k=0;k<nodeList.size();k++){
////				System.out.println(nodeList.get(k).getId());
//				dummy = nodeList.get(k).getId();
//				bnode.write(dummy);
//				bnode.write(",");
//				bnode.newLine();
//			}
//			
//		} catch (IOException e) {
//			e.printStackTrace();
////			LOG.error("Oops, couldn't write to file.");
//		} finally{
//			try {
//				bnode.close();
//			} catch (IOException e) {
//				e.printStackTrace();
////				LOG.error("Oops, couldn't close");
//			}
//		}
//		
//		
		BufferedWriter btest = IOUtils.getBufferedWriter("/Users/nadiaviljoen/Desktop/test.csv");
		try{

			for (int row=0;row<nodeList.size();row++){
				btest.write(String.valueOf(transSumsNew[row]));
				btest.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				btest.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		
		
		//Set the transition probabilities - METHOD 1 OUT Edges
		for (int i = 0; i<nodeList.size();i++){
			NmvNode begin = nodeList.get(i);
			int debug = 0;
//			System.out.println(begin.getId());
//			System.out.println(myGraph.getOutEdges(begin));
			for (Iterator o_iter = myGraph.getOutEdges(begin).iterator(); o_iter.hasNext(); )
			{

				debug++;
//				if(debug>1){
//				System.out.print(" ");
//				}
				//no idea why it never goes in here!

				NmvLink e = (NmvLink) o_iter.next();
				if(e.getTransProb()==-99){
					NmvNode end = myGraph.getOpposite(begin,e);
					int j = nodeList.indexOf(end);
					double roundOff = (double) Math.round((transProb[i][j]) * 1000000) / 1000000;
					e.setTransProb(roundOff);
//					e.setTransProb((transProb[i][j]));
//					System.out.println(String.format("%d,%.15f",j,e.getTransProb()));
//					if(debug>1){
//					System.out.print(" ");
//					}
				}else{
					//This is to test whether there are multiple edges with multiple values but there aren't this is not the problem
					NmvNode end = myGraph.getOpposite(begin,e);
					int j = nodeList.indexOf(end);
					if(e.getTransProb()!=transProb[i][j]){
						System.out.println("VarkSteaks!!");
					}
				}
			}
		}
		
//		//Set trans probs - METHOD 2 - ENUMERATE
//		
//		for (int i = 0; i<nodeList.size();i++){
//			for (int p = 0; p<nodeList.size();p++){
//			//Some null pointer exception I cannot figure out	
//		if(myGraph.findEdge(nodeList.get(i), nodeList.get(p)).getTransProb()==-99){
//					myGraph.findEdge(nodeList.get(i), nodeList.get(p)).setTransProb(transProb[i][p]);
//				}
//				
//			}
//		}
		
		
		
		
		//test that TransProb still add to 1 after setting
		
		for (int i = 0; i<nodeList.size();i++){
			double sum=0;
			NmvNode testNode = nodeList.get(i);
			for (Iterator o_iter = myGraph.getOutEdges(testNode).iterator(); o_iter.hasNext(); )
			{
				NmvLink e = (NmvLink) o_iter.next();
				sum=sum+e.getTransProb();
			
			}
			if (Math.abs(1-sum)>0.000001){
				System.out.println(String.format("%s,%.20f",testNode.getId(),sum));
				boolean s =true;
				if(sum<1){
					Iterator o_iter = myGraph.getOutEdges(testNode).iterator();
					//add to the smallest element
					
					while (s==true&&o_iter.hasNext()){
						NmvLink e = (NmvLink) o_iter.next();
						double current = e.getTransProb();
						//check that by adding you don't make one element bigger than 1
						if(current+1-sum<1){
							e.setTransProb(current+(1-sum));
							System.out.println(String.format("%.20f,%.20f",current,e.getTransProb()));
							s = false;
						}
					}
				}else{
					
					Iterator o_iter = myGraph.getOutEdges(testNode).iterator();
					while (s==true&&o_iter.hasNext()){
						NmvLink e = (NmvLink) o_iter.next();
						double current = e.getTransProb();
						//check that you don't make any element negative
						if(current-(sum-1)>0){
							e.setTransProb(current-(sum-1));
							System.out.println(String.format("%.20f,%.20f",current,e.getTransProb()));
							s = false;
						}
					}
				}
			}
		}
		
		//Iron out precision errors
//		for (int r=0; r<linkList.size();r++){
//			double current;
//			current = Math.floor(linkList.get(r).getTransProb()*1000000)/1000000;
//			linkList.get(r).setTransProb(current);
//			
//		}
		
		System.out.println("TESTING");
		for (int i = 0; i<nodeList.size();i++){
			double sum2=0;
			NmvNode testNode2 = nodeList.get(i);
			for (Iterator o_iter = myGraph.getOutEdges(testNode2).iterator(); o_iter.hasNext(); )
			{
				NmvLink e = (NmvLink) o_iter.next();
				sum2=sum2+e.getTransProb();

			}
			if (sum2!=1){
				System.out.println(String.format("%s,%.20f",testNode2.getId(),sum2));
			}
		}
		
		Transformer<NmvLink, Double> wtTransformer = new Transformer<NmvLink,Double>(){
			public Double transform(NmvLink link){
				return link.getTransProb();
			}
		};
		EigenvectorCentrality<NmvNode, NmvLink> ranker = new EigenvectorCentrality<NmvNode, NmvLink>(myGraph, wtTransformer);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodeEigenWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_E");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.15f\n", 
						nodeList.get(i).getId(),
						nodeList.get(i).getXAsString(), 
						nodeList.get(i).getYAsString(),
						null,
						null,
						ranker.getVertexScore(nodeList.get(i))));

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
	public static void calculateAndWriteDegreeCentrality(DirectedGraph<NmvNode,NmvLink> myGraph, String degreeFile,
			ArrayList<NmvNode> nodeList,final LinkedList<NmvLink> linkList){
		int currentDegree;
		int currentInDegree;
		int currentOutDegree;
		BufferedWriter bdegree = IOUtils.getBufferedWriter(degreeFile);
		try{
			bdegree.write("NodeID,X,Y,Long,Lat,C_Dtot,C_Din,C_Dout");
			bdegree.newLine();
			for (int i=0;i<nodeList.size();i++){
				NmvNode current = nodeList.get(i);
				currentDegree=myGraph.degree(current);
				currentInDegree=myGraph.inDegree(current);
				currentOutDegree=myGraph.outDegree(current);
				bdegree.write(String.format("%s,%s,%s,%.6f,%.6f,%d,%d,%d\n", 
						current.getId(),
						current.getXAsString(), 
						current.getYAsString(),
						null,
						null,
						currentDegree,currentInDegree,currentOutDegree));
			}
		} catch (IOException e) {
			e.printStackTrace();
//			LOG.error("Oops, couldn't write to file.");
		} finally{
			try {
				bdegree.close();
			} catch (IOException e) {
				e.printStackTrace();
//				LOG.error("Oops, couldn't close");
			}
		}
		
	}
	public static void calculateAndWriteUnweightedPageRank(DirectedGraph<NmvNode,NmvLink> myGraph, String nodePageRankUnweighted,
			ArrayList<NmvNode> nodeList) {
		double alpha = 0;
		PageRank<NmvNode, NmvLink> ranker = new PageRank<NmvNode, NmvLink>(myGraph,alpha);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodePageRankUnweighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_P");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.10f\n", 
						nodeList.get(i).getId(),
						nodeList.get(i).getXAsString(), 
						nodeList.get(i).getYAsString(),
						null,
						null,
						ranker.getVertexScore(nodeList.get(i))));
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

	public static void calculateAndWriteWeightedPageRank(DirectedGraph<NmvNode,NmvLink> myGraph, String nodePageRankWeighted,
			ArrayList<NmvNode> nodeList) {
		Transformer<NmvLink, Double> wtTransformer = new Transformer<NmvLink,Double>(){
			public Double transform(NmvLink link){
				return link.getWeight();
			}
		};
		EigenvectorCentrality<NmvNode, NmvLink> ranker = new EigenvectorCentrality<NmvNode, NmvLink>(myGraph, wtTransformer);
		ranker.acceptDisconnectedGraph(true);
		ranker.evaluate();
		BufferedWriter bw = IOUtils.getBufferedWriter(nodePageRankWeighted);
		try{
			bw.write("NodeID,X,Y,Long,Lat,C_E");
			bw.newLine();
			for(int i=0;i<nodeList.size();i++)
			{
				bw.write(String.format("%s,%s,%s,%.6f,%.6f,%.10f\n", 
						nodeList.get(i).getId(),
						nodeList.get(i).getXAsString(), nodeList.get(i).getYAsString(),
						null,
						null,
						ranker.getVertexScore(nodeList.get(i))));

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
	 public static SparseDoubleMatrix2D nadiaGraphToSparseMatrix(DirectedGraph<NmvNode,NmvLink> g, ArrayList<NmvNode> nodeList)
	  {
		 int numVertices = g.getVertices().size();
	        SparseDoubleMatrix2D matrix = new SparseDoubleMatrix2D(numVertices,numVertices);
	        for (int i = 0; i < numVertices; i++)
	         {
	            NmvNode v = nodeList.get(i);
	            
	             for (Iterator o_iter = g.getOutEdges(v).iterator(); o_iter.hasNext(); )
	             {
	                NmvLink e = (NmvLink) o_iter.next();
	                 NmvNode w = g.getOpposite(v,e);
	                
	                //find it's position in nodeList
	                 int j = nodeList.indexOf(w);
	                 
	                 matrix.set(i, j, matrix.getQuick(i,j) + e.getWeight());
	             }
	         }
	        //the sequence of the matrix indices is the sequence of nodeList
	         return matrix;
	  }
		 

}

