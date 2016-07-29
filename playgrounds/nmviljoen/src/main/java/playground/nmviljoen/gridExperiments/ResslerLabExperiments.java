package playground.nmviljoen.gridExperiments;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;

public class ResslerLabExperiments {
	private final static Logger LOG = Logger.getLogger(MalikLabExperiments.class);
	
	private DirectedGraph<NmvNode, NmvLink> myGraphGrid; 

	private DirectedGraph<NmvNode, NmvLink> myGraphGhost;
	private DirectedGraph<NmvNode, NmvLink> myGraphRessler;
	private String path;
	private int[][] assocList;
	
	public ResslerLabExperiments(String path, int ModeSwitch, int SimDim, int fullPathSize, int segSize, int segPathLength) {       
		this.path = path;
		if (ModeSwitch == 0){
			this.myGraphRessler = TriGraphConstructor.constructResslerGraph(path);
			LOG.info("Ressler Graph created");
			this.myGraphGrid = TriGraphConstructor.constructGridGraph(path);
			LOG.info("Grid Graph created");
			assocList = TriGraphConstructor.layerRessler(path,myGraphRessler,myGraphGrid);
			TriGraphConstructor.constructGhostGraphRessler(path, assocList,fullPathSize, segSize, segPathLength );
			LOG.info("The ghost lives");
		}else{
			this.myGraphRessler = TriGraphConstructor.constructResslerGraph(path);
			LOG.info("Ressler Graph created");
			this.myGraphGrid = TriGraphConstructor.constructGridGraphSim(path,SimDim);
			LOG.info("Grid Graph created");
			assocList = TriGraphConstructor.layerResslerSim(path);
			TriGraphConstructor.constructGhostGraphRessler(path, assocList,fullPathSize , segSize,segPathLength);
			LOG.info("The ghost lives");	
		}
		
	}
	
	
	public void runThisSpecificExperiment(){
		LinkedList<NmvLink> linkListGrid = new LinkedList<NmvLink>(myGraphGrid.getEdges());
		ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(myGraphGrid.getVertices());
//		LinkedList<NmvLink> linkListGhost = new LinkedList<NmvLink>(myGraphGhost.getEdges());
//		ArrayList<NmvNode> nodeListGhost = new ArrayList<NmvNode>(myGraphGhost.getVertices());
		LinkedList<NmvLink> linkListRessler = new LinkedList<NmvLink>(myGraphRessler.getEdges());
		ArrayList<NmvNode> nodeListRessler = new ArrayList<NmvNode>(myGraphRessler.getVertices());
		String shortFile=path+"ResslerShortPathStat.csv";
		ShortestPath.collectShortest(myGraphGrid, myGraphRessler, linkListGrid, linkListRessler, nodeListGrid, nodeListRessler, assocList, shortFile);
		
		//Grid graph metrics
		String Gridpath=path+"GridGraph";
		
		//Centrality scores
		
		JungCentrality.calculateAndWriteUnweightedBetweenness(myGraphGrid,Gridpath+"unweightedNodeBetweenness.csv", Gridpath+"unweightedEdgeBetweenness.csv",nodeListGrid, linkListGrid);
		JungCentrality.calculateAndWriteUnweightedCloseness(myGraphGrid, Gridpath+"unweightedCloseness.csv", nodeListGrid);
		JungCentrality.calculateAndWriteUnweightedEigenvector(myGraphGrid, Gridpath+"unweightedEigen.csv", nodeListGrid);
		JungCentrality.calculateAndWriteDegreeCentrality(myGraphGrid, Gridpath+"Degree.csv", nodeListGrid, linkListGrid);

		//Clustering
		JungClusters.calculateAndWriteClusteringCoefficient(myGraphGrid, Gridpath+"clusterCoeff.csv");
		JungClusters.calculateAndWriteWeakComponents(myGraphGrid, Gridpath+"weakComp.csv");
		JungClusters.calculateAndWriteTriadicCensus(myGraphGrid, Gridpath+"triadCensus.csv");

		//Graph distance
		JungGraphDistance.calculateAndWriteUnweightedDistances(myGraphGrid, path+"unweightedDist.csv");
		
		//Ghost graph metrics
		String Ghostpath=path+"GhostGraph";
		
		//Centrality scores
		
//		JungCentrality.calculateAndWriteUnweightedBetweenness(myGraphGhost,Ghostpath+"unweightedNodeBetweenness.csv", Ghostpath+"unweightedEdgeBetweenness.csv",nodeListGhost, linkListGhost);
//		JungCentrality.calculateAndWriteUnweightedCloseness(myGraphGhost, Ghostpath+"unweightedCloseness.csv", nodeListGhost);
//		JungCentrality.calculateAndWriteUnweightedEigenvector(myGraphGhost, Ghostpath+"unweightedEigen.csv", nodeListGhost);
//		JungCentrality.calculateAndWriteDegreeCentrality(myGraphGhost, Ghostpath+"Degree.csv", nodeListGhost, linkListGhost);
//
//		//Clustering
//		JungClusters.calculateAndWriteClusteringCoefficient(myGraphGhost, Ghostpath+"clusterCoeff.csv");
//		JungClusters.calculateAndWriteWeakComponents(myGraphGhost, Ghostpath+"weakComp.csv");
//		JungClusters.calculateAndWriteTriadicCensus(myGraphGhost, Ghostpath+"triadCensus.csv");
//
//		//Graph distance
//		JungGraphDistance.calculateAndWriteUnweightedDistances(myGraphGhost, Ghostpath+"unweightedDist.csv");
		
	}
	
	public static void main(String[] args) throws FileNotFoundException{
//		Header.printHeader(ResslerLabExperimentsLocal.class.toString(), args);
		
		
		int ModeSwitch = Integer.parseInt(args[2]); //if ModeSwitch = 0 then it's a base run; if =1 then it's a simulation
		int runStart = Integer.parseInt(args[3]);
		int runEnd = Integer.parseInt(args[4]);
		int SimDim = Integer.parseInt(args[5]);
		int fullPathSize = Integer.parseInt(args[6]);
		int segSize = Integer.parseInt(args[7]);
		int segPathLength = Integer.parseInt(args[8]);
		for (int y = runStart; y<=runEnd;y++){
			String path = args[0];
			String sim =args[1];
			if(ModeSwitch==0){
				path = path+y+"/"+"Base_"+y+"/";
			}else{
				path = path+y+"/"+sim+y+"/";
			}
			
			ResslerLabExperiments mle = new ResslerLabExperiments(path, ModeSwitch, SimDim,fullPathSize,segSize,segPathLength);
			mle.runThisSpecificExperiment();
		}
		
//		Header.printFooter();
	}

}
