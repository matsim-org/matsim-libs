package playground.mmoyo.PTCase1;

import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import playground.mmoyo.PTRouter.*;

public class PTControler {
	private static final String NETWORKFILENAME="C://Users/manuel/Desktop/berlinOEV/network.xml";
	private static final String PTLINES = "C://Users/manuel/Desktop/berlinOEV/ptLines.xml";
	private static final String PTLINESTIMETABLE = "C://Users/manuel/Desktop/berlinOEV/ptLinesSchedule.xml";
	private static NetworkLayer cityNetworkLayer = null;
	private static PTNetworkLayer ptNetworkLayer = null;
	private static PTLinesReader ptLinesReader = new PTLinesReader();
	private static PTTimeTableReader ptTimeTableReader = new PTTimeTableReader();
	private static PTTimeTableInfo ptTimeTableInfo = null;
	private static PTLinkCostCalculator ptLinkCostCalculator;
	
	public static void main(String[] args) {
		int option =3;
	    switch (option) {
	    	case 0:   //
	    		break;
	    	case 1:  //creation of PTNetwork to visualize
	    	 	readCityNetwork();
	    		ptLinesReader.readFile(PTLINES);
	    		createPTView(); 
	            break;
	            
	    	case 3: // new version of router
	    		readCityNetwork();
	    		createPTNetwork();
	    		createTimetable();
	    		
	    		PTRouter ptRouter = new PTRouter (cityNetworkLayer, ptNetworkLayer, ptLinkCostCalculator);
	    		//ptRouter.findRoute("0", "24", 32400);
	    		ptRouter.timeComputation2(1000, "0", "24", 32400);
	    		//ptTimeTableInfo.printTimeTable();
	    		break;

	    	case 4: //Print Ptnetwork view
	    		readCityNetwork();
	    		createPTNetwork();
	    		ptNetworkLayer.printLinks();	
	    		break;
	    	case 5: //Test with the Matsim Disjktra
	    		readCityNetwork();
	    		createPTNetwork();
	    		createTimetable();
	    		
	    	//	PTRouter2 ptDijkstra2 = new PTRouter2(ptNetworkLayer, ptTimeTableInfo);
	    	//	ptDijkstra2.PrintRoute("37", "43", 100);
	    }
	}//main

	private static void readCityNetwork() {
		NetworkFactory networkFactory = new NetworkFactory();
		cityNetworkLayer = new NetworkLayer(networkFactory);
		new MatsimNetworkReader(cityNetworkLayer).readFile(NETWORKFILENAME);
	}
	
	private static void createPTNetwork(){
		ptNetworkLayer = new PTNetworkLayer(cityNetworkLayer);
		ptLinesReader.readFile(PTLINES);
		ptNetworkLayer.createPTNetwork(ptLinesReader.ptLineList);
	}
	
	private static void createTimetable(){
		ptTimeTableReader.readFile(PTLINESTIMETABLE);
		ptTimeTableInfo = new PTTimeTableInfo(ptTimeTableReader.getTimeTable());
		ptLinkCostCalculator = new PTLinkCostCalculator(ptTimeTableInfo);
	}
	
	private static void createPTView() {
		PTNetwork2View ptNetwork2view = new PTNetwork2View(cityNetworkLayer);
		ptNetwork2view.writePTNetwork();
		ptNetwork2view.printLinks();
	}

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