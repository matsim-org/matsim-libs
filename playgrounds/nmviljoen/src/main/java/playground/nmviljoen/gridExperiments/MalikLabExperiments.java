package gridExperiments;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.DirectedGraph;
import gridExperiments.JungCentrality;
import gridExperiments.JungClusters;
import gridExperiments.JungGraphDistance;
import gridExperiments.NmvLink;
import gridExperiments.NmvNode;
import gridExperiments.ShortestPath;
//import playground.southafrica.utilities.Header;
public class MalikLabExperiments {
	
		private final static Logger LOG = Logger.getLogger(MalikLabExperiments.class);
		
		private DirectedGraph<NmvNode, NmvLink> myGraphGrid; 
		private DirectedGraph<NmvNode, NmvLink> myGraphMalik;
		private DirectedGraph<NmvNode, NmvLink> myGraphGhost;

		private String path;
		private int[][] assocList;
		
		public MalikLabExperiments(String path, int ModeSwitch, int SimDim, int fullPathSize, int segSize, int segPathLength) {       
			this.path = path;
			if (ModeSwitch == 0){
				this.myGraphMalik = TriGraphConstructor.constructMalikGraph(path);
				LOG.info("Malik Graph created");
				this.myGraphGrid = TriGraphConstructor.constructGridGraph(path);
				LOG.info("Grid Graph created");
				assocList = TriGraphConstructor.layerMalik(path,myGraphMalik,myGraphGrid);
				TriGraphConstructor.constructGhostGraphMalik(path, assocList,fullPathSize, segSize, segPathLength );
				LOG.info("The ghost lives");
			}else{
				this.myGraphMalik = TriGraphConstructor.constructMalikGraph(path);
				LOG.info("Malik Graph created");
				this.myGraphGrid = TriGraphConstructor.constructGridGraphSim(path,SimDim);
				LOG.info("Grid Graph created");
				assocList = TriGraphConstructor.layerMalikSim(path);
				TriGraphConstructor.constructGhostGraphMalik(path, assocList,fullPathSize , segSize,segPathLength);
				LOG.info("The ghost lives");	
			}
			
		}
		
		
		public void runThisSpecificExperiment(){
			LinkedList<NmvLink> linkListGrid = new LinkedList<NmvLink>(myGraphGrid.getEdges());
			ArrayList<NmvNode> nodeListGrid = new ArrayList<NmvNode>(myGraphGrid.getVertices());
	
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
			

		}
		
		public static void main(String[] args) throws FileNotFoundException{
//			Header.printHeader(MalikLabExperimentsLocal.class.toString(), args);
			
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
				
				MalikLabExperiments mle = new MalikLabExperiments(path, ModeSwitch, SimDim,fullPathSize,segSize,segPathLength);
				mle.runThisSpecificExperiment();
			}
			
//			Header.printFooter();
		}

}
