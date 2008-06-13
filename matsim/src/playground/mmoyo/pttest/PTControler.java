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
		
	public static void main(String[] args) {
		int option = 2;
	    switch (option) {
	    	case 0:   //routing of passengers  
	        	readCityNetwork();
	    		ptLinesReader.readFile(PTLINES);
	    		Route();
	            break;
	    	case 1:   	//creation of PTNetwork to visualize
	    	 	readCityNetwork();
	    		ptLinesReader.readFile(PTLINES);
	    		CreatePTView(); 
	            break;
	    	case 2:     //Creation of Timetables
	    		ptLinesScheduleReader.readFile(PTLINESTIMETABLE);
	    		PTTimeTableInfo ptnDepartureInforman = new PTTimeTableInfo(ptLinesScheduleReader.GetTimeTable());   
	    		//ptnDepartureInforman.printTimetable(); 
	            
	    		break;
	        }
	}//main

	private static void Route(){
		ptNetworkLayer = new PTNetworkLayer(cityNetworkLayer);
		ptNetworkLayer.CreatePTNetwork(ptLinesReader.ptLineList);
		OptimalPTPath();
	}

	private static void readCityNetwork() {
		NetworkFactory networkFactory = new NetworkFactory();
		cityNetworkLayer = new NetworkLayer(networkFactory);
		new MatsimNetworkReader(cityNetworkLayer).readFile(NETWORKFILENAME);
	}

	private static void CreatePTView() {
		PTNetwork2View ptNetwork2view = new PTNetwork2View(cityNetworkLayer);
		ptNetwork2view.writePTNetwork();
		ptNetwork2view.PrintLinks();
	}

	private static void OptimalPTPath() {
		MiniDijkstra router = null;
		
		//Validate that the starting and destination nodes exist
		IdImpl startNode= new IdImpl("0");
		IdImpl endNode = new IdImpl("24");
		PTNode n1 = null;
		PTNode n2 = null;
		if (cityNetworkLayer.getNodes().containsKey(startNode)){
			n1 = (PTNode)ptNetworkLayer.getNodes().get(startNode);
		}
		else{
			throw new NullPointerException("Starting node does not exist");
		}
		if (cityNetworkLayer.getNodes().containsKey(endNode)){
			n2 = (PTNode)ptNetworkLayer.getNodes().get(endNode);
		}
		else{
			throw new NullPointerException("Destination node does not exist");
		}
		
		List<Node> nl = Arrays.asList(ptNetworkLayer.getNodes().values().toArray(new Node[ptNetworkLayer.getNodes().size()]));
		List<Link> ll = Arrays.asList(ptNetworkLayer.getLinks().values().toArray(new Link[ptNetworkLayer.getLinks().size()]));

		long inicio, fin;
		for (int i = 0; i <= 10; i++) {
			inicio = System.currentTimeMillis();
			router = new MiniDijkstra(nl, ll, n1, n2);
			fin = System.currentTimeMillis();
			System.out.println("\n"+ i + "-ended:-" + (fin - inicio) + "-miliseconds");
			
			String busLine = "";
			for (int ii = 0; ii < router.route.length; ii++){
				if (router.route[ii].getIdPTLine() != null){
					if (busLine != router.route[ii].getIdPTLine().toString()) {
						busLine = router.route[ii].getIdPTLine().toString();
						System.out.println("\n" + busLine + ": ");
					}//if busLine
					System.out.print(router.route[ii].getIdFather().toString() + "-");
				}//if router
			}//for ii
		}//for i
	}//OptimalPath

	
}// class

// Old code
/*
 * //This is not longer necessary now because the PTNetwork is independent with own nodes and links 
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