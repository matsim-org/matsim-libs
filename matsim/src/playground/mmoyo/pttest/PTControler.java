package playground.mmoyo.pttest;

import java.util.Arrays;
import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class PTControler {
	private static final String NETWORKFILENAME="C://Users/manuel/Desktop/berlinOEV/network.xml";
	private static final String PTLINES = "C://Users/manuel/Desktop/berlinOEV/ptLines.xml";
	private static final String PTLINESTIMETABLE = "C://Users/manuel/Desktop/berlinOEV/ptLinesSchedule.xml";
	private static NetworkLayer cityNetworkLayer = null;
	private static PTNetworkLayer ptNetworkLayer = null;
	private static PTLinesReader ptLinesReader = new PTLinesReader();
	private static PTTimeTableReader ptLinesScheduleReader = new PTTimeTableReader();
	private static PTTimeTableInfo ptTimeTableInfo = null;
	private static PTLinkCostCalculator ptLinkCostCalculator =null;
	
	public static void main(String[] args) {
		int option =3;
	    switch (option) {
	    	case 0:   //routing of passengers  
	    		ReadCityNetwork();

	    		ptLinesReader.readFile(PTLINES);
	    		ptLinesScheduleReader.readFile(PTLINESTIMETABLE);           
	    		break;
	    	case 1:   	//creation of PTNetwork to visualize
	    	 	ReadCityNetwork();
	    		ptLinesReader.readFile(PTLINES);
	    		CreatePTView(); 
	            break;
	    	case 2:     //Creation of Timetables
	    		ptLinesScheduleReader.readFile(PTLINESTIMETABLE);
	    		
	    		for (int i = 0; i <= 10; i++) {
		    		long inicio = System.currentTimeMillis();
		    		//ptnDepartureInforman.NextDepartureH("5", time);
					long fin = System.currentTimeMillis();
					System.out.println("\n"+ i + "-ended:-" + (fin - inicio) + "-miliseconds");
	    		}
	    		break;
	    	case 3: // new version of router
	    		ReadCityNetwork();
	    		CreatePTNetwork();
	    		CreateTimetable();
	    		OptimalPTPath();
	    		break;
	    	case 4: //Print Ptnetwork view
	    		ReadCityNetwork();
	    		ptNetworkLayer = new PTNetworkLayer(cityNetworkLayer);
	    		ptLinesReader.readFile(PTLINES);
	    		ptNetworkLayer.CreatePTNetwork(ptLinesReader.ptLineList);
	    		ptNetworkLayer.PrintLinks();	
	    		break;
	    }
	}//main

	private static void ReadCityNetwork() {
		NetworkFactory networkFactory = new NetworkFactory();
		cityNetworkLayer = new NetworkLayer(networkFactory);
		new MatsimNetworkReader(cityNetworkLayer).readFile(NETWORKFILENAME);
	}
	
	private static void CreatePTNetwork(){
		ptNetworkLayer = new PTNetworkLayer(cityNetworkLayer);
		ptLinesReader.readFile(PTLINES);
		ptNetworkLayer.CreatePTNetwork(ptLinesReader.ptLineList);
	}
	
	private static void CreateTimetable(){
		ptLinesScheduleReader.readFile(PTLINESTIMETABLE);
		ptTimeTableInfo = new PTTimeTableInfo(ptLinesScheduleReader.GetTimeTable());
		ptLinkCostCalculator = new PTLinkCostCalculator(ptTimeTableInfo);
	}
	
	private static void CreatePTView() {
		PTNetwork2View ptNetwork2view = new PTNetwork2View(cityNetworkLayer);
		ptNetwork2view.writePTNetwork();
		ptNetwork2view.PrintLinks();
	}

	private static void OptimalPTPath() {
		//Validates starting and destination nodes, Prepares the
		//data for the dijkstra class and display the results
		IdImpl startNode= new IdImpl("0");
		IdImpl endNode = new IdImpl("24");
		Node n1 = null;
		Node n2 = null;
		if (cityNetworkLayer.getNodes().containsKey(startNode)){
			n1 = cityNetworkLayer.getNodes().get(startNode);
		}
		else{
			throw new NullPointerException("Starting node does not exist");
		}
		if (cityNetworkLayer.getNodes().containsKey(endNode)){
			n2 = cityNetworkLayer.getNodes().get(endNode);
		}
		else{
			throw new NullPointerException("Destination node does not exist");
		}
		
		//TODO: Esto es lo ideal despues considerar partir el dijstra para que se cre una ve y despues reciba unicamente los nodos a rutear
		MiniDijkstra miniDijkstra = null;
		
		long inicio, fin;
		
		for (int i = 0; i <100000; i++) {
			inicio = System.currentTimeMillis();
			List<String> newLinks = ptNetworkLayer.CreateWalkingLinks(startNode,endNode);
			List<Link> ll = Arrays.asList(ptNetworkLayer.getLinks().values().toArray(new Link[ptNetworkLayer.getLinks().size()]));	
			List<Node> nl = Arrays.asList(ptNetworkLayer.getNodes().values().toArray(new Node[ptNetworkLayer.getNodes().size()]));
			miniDijkstra = new MiniDijkstra(nl, ll, n1,n2, ptLinkCostCalculator,32400);
			ptNetworkLayer.RemoveWalkinkLinks(newLinks);
			ptNetworkLayer.RemoveWalkingNodes(startNode,endNode);
			fin = System.currentTimeMillis();
			System.out.println("Iteration:"+ i + "-ended:-" + (fin - inicio) + "-miliseconds");
			
			String busLine = "";
			for (int ii = 0; ii < miniDijkstra.route.length; ii++){
				if (miniDijkstra.route[ii].getIdPTLine() != null){
					if (busLine != miniDijkstra.route[ii].getIdPTLine().toString()) {
						busLine = miniDijkstra.route[ii].getIdPTLine().toString();
						System.out.println("\n" + busLine + ": ");
					}//if busLine
					System.out.print(miniDijkstra.route[ii].getIdFather().toString() + "-");
				}//if router
			}//for ii
		}//for i
	}//OptimalPath

}// class

// Old code
/*
 * //Calculates the maximal id link or Node of the network 
 * public static int MaxNodeKey(NetworkLayer net){ 
 * 		int key = 0;
 * 		while (net.getNodes().keySet().contains(new IdImpl(String.valueOf(key)))){
 * 			key++; 
 * 		} 
 * 		net=null; 
 * 		return key-1; 
 * }
 * 
 * public static int MaxLinkKey(NetworkLayer net){ 
 * 		int key = 0; 
 * 		while(net.getLinks().keySet().contains(new IdImpl(String.valueOf(key)))){ 
 * 			key++; 
 * 		}
 * 		net=null; 
 * 		return key-1; 
 * }
 */