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
	private static final String NETWORKFILENAME="./network.xml";
	private static final String PTLINES = "./ptLines.xml";
	private static NetworkLayer cityNetworkLayer = null;
	private static PTNetworkLayer ptNetworkLayer = null;
	private static PTLinesReader ptLinesReader = new PTLinesReader();

	public static void main(String[] args) {
		readCityNetwork();
		ptLinesReader.readFile(PTLINES);
		// CreatePTView();
		Route();
	}// main

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
		PrintLinks();
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

	private static void PrintLinks() {
		// Console quick visualization of links with from and to nodes
		for (org.matsim.network.Link l : ptNetworkLayer.getLinks().values()) {
			// This one is the normal displayer
			// System.out.println("(" + l.getFromNode().getId().toString()+ ")----" + l.getId().toString() + "--->(" +  l.getToNode().getId().toString()+ ") ");
			// System.out.println (((PTNode)l.getFromNode()).getIdFather().toString() + "-------- " +((PTNode)l.getToNode()).getIdFather().toString()); 
			// This display also the id of the father node
			System.out.println("(" + l.getFromNode().getId().toString() + ")----" + l.getId().toString() + "--->(" + l.getToNode().getId().toString() + ")   " + "      (" + ((PTNode) l.getFromNode()).getIdFather().toString()+ ")----" + l.getId().toString() + "--->(" + ((PTNode) l.getToNode()).getIdFather().toString() + ")");
		}
	}
}// class