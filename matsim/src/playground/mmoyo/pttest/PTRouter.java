package playground.mmoyo.pttest;

import java.util.Arrays;
import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class PTRouter{
	public PTNode[] route;
	private PTNetworkLayer ptNetworkLayer;
	private PTLinkCostCalculator ptLinkCostCalculator;
	private NetworkLayer cityNetworkLayer;
	
	public PTRouter(NetworkLayer cityNetworkLayer, PTNetworkLayer ptNetworkLayer, PTLinkCostCalculator ptLinkCostCalculator){
		this.cityNetworkLayer= cityNetworkLayer;
		this.ptNetworkLayer = ptNetworkLayer;
		this.ptLinkCostCalculator = ptLinkCostCalculator;
	}

	public void findRoute(String startNode, String endNode, int time){
		findRoute(new IdImpl(startNode), new IdImpl(endNode), time );
		printRoute();
	}

    /* 
     * Validates the existence of start and origin nodes
     * Create the walking links
     * Invoke the routing algorithm
     * Destroy the walking links
     */
    private void findRoute(IdImpl startNode, IdImpl endNode, int time){
		
		///////////////Validates that nodes exist in citiNetwork
		if (!cityNetworkLayer.getNodes().containsKey(startNode)){
			throw new NullPointerException("Starting node does not exist");
		}
		if (!cityNetworkLayer.getNodes().containsKey(endNode)){
			throw new NullPointerException("Destination node does not exist");
		}
		///////////////////////////////////////////////////////////////////////

		List<String> newLinks = ptNetworkLayer.createWalkingLinks(startNode,endNode);
		List<Link> ll = Arrays.asList(ptNetworkLayer.getLinks().values().toArray(new Link[ptNetworkLayer.getLinks().size()]));	
		List<Node> nl = Arrays.asList(ptNetworkLayer.getNodes().values().toArray(new Node[ptNetworkLayer.getNodes().size()]));

		MiniDijkstra miniDijkstra = new MiniDijkstra(nl, ll, this.ptLinkCostCalculator);
		
		Node n1 = cityNetworkLayer.getNodes().get(startNode);
		Node n2 = cityNetworkLayer.getNodes().get(endNode);
		miniDijkstra.shortestPath(n1,n2,time);	
		route = miniDijkstra.route;
		
		ptNetworkLayer.removeWalkinkLinks(newLinks);
		ptNetworkLayer.removeWalkingNodes(startNode,endNode);	
    }

    /*
	 *Times the calculation of shortest path algorithm and prints the result 
	*///based on intervals periods
    public void timeComputation2(int numIterations1, String n1, String n2, int time){
    	long startTime=0;
    	long endTime=0;
    	long duration;
    	long duration2=0;
    	for(int x=0; x<numIterations1;x++){
    		startTime = System.currentTimeMillis();
    		for(int j=0; j<4000;j++){
        		findRoute(new IdImpl(n1), new IdImpl(n2), time);
        	}
    		endTime = System.currentTimeMillis();
    		duration= (startTime - endTime);
    		duration2= duration2+duration;
    		System.out.println(String.valueOf(x) + "-Iteration  lasted:-" + duration + "-miliseconds");	
    	}
    	duration2= duration2/2000;
    	System.out.println("Average:" + duration2 + "- miliseconds");
    }


	/*
	 *Times the calculation of shortest path algorithm and prints the result 
	*///based on individual interation
    public void timeComputation(long numIterations, String n1, String n2, int time){
    	long startTime=0;
    	long endTime=0;
    	for(int x=0; x<=numIterations;x++){
        	startTime = System.currentTimeMillis();
        	findRoute(new IdImpl(n1), new IdImpl(n2), time);
        	endTime = System.currentTimeMillis();
        	long duration= startTime-endTime;
        	System.out.println(String.valueOf(x) + "-Iteration  lasted:-" + String.valueOf(duration) + "-miliseconds");
    	}	
    }

    public void printRoute(){
    	String busLine = "";
    	for (int i = 0; i < route.length; i++){
			if (route[i].getIdPTLine() != null){
				if (busLine != route[i].getIdPTLine().toString()) {
					busLine = route[i].getIdPTLine().toString();
					System.out.println("\n" + busLine + ": ");
				}
				System.out.print(route[i].getIdFather().toString() + "-");
			}
		}
    }
}//class