package playground.mmoyo.Validators;

import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTCase2.PTTravelCost;
import playground.mmoyo.PTCase2.PTTravelTime1;
import playground.mmoyo.PTRouter.MyDijkstra;
import playground.mmoyo.PTRouter.PTLine;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.api.network.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * Identifies isolated PTlines
 */
public class PTLineValidator {
	
	
	
	/**
	 * Shows ptlines with no connection with other ptLines
	 * @param ptTimeTable
	 * @param net
	 */
	public void getIsolatedPTLines(PTTimeTable2 ptTimeTable, Network net){
		int isolated=0;
		int comparisons=0;
		PTTravelCost ptTravelCost = new PTTravelCost(ptTimeTable);
		PTTravelTime1 ptTravelTime1 = new PTTravelTime1();
		LeastCostPathCalculator expressDijkstra = new MyDijkstra(net, ptTravelCost, ptTravelTime1);
		
		List<Id[]> ptLineIdList = new ArrayList<Id[]>();
		for (PTLine ptLine: ptTimeTable.getPtLineList()){
			
			Id firstNodeId = ptLine.getNodeRoute().get(0);
			for (PTLine ptLine2 : ptTimeTable.getPtLineList()){
				Id lastNodeId = ptLine2.getNodeRoute().get(ptLine2.getNodeRoute().size()-1);
				Node node1 = net.getNode(firstNodeId);
				if(!ptLine.equals(ptLine2)){
					Node node2 = net.getNode(lastNodeId);
					Path path = expressDijkstra.calcLeastCostPath(node1, node2, 600);
					comparisons++;
					if (path==null){
						//Id[2] intArray = [ptLine.getId(),ptLine2.getId()];
						Id[] idArray = new Id[2];
						idArray[0] = ptLine.getId();
						idArray[1] = ptLine2.getId();
						ptLineIdList.add(idArray);
						isolated++;
					}
				}
				
			}
		}
	
		for(Id[] idarray: ptLineIdList){
			System.out.println("\n" + idarray[0] + "\n" + idarray[1] );
		}
		System.out.println(	"Total comparisons: " + comparisons + "\nisolated: " + isolated);
	}
	
	
	/**
	 * Returns the minimal distance between two PTLines. This can help the decision to join them with a Detached Transfer 
	 */
	public double getMinimalDistance (PTLine ptl1, PTLine ptl2 ){
		double minDistance=0;
		// ->compare distances from first ptline with ever node of secondptline, store the minimal distance		
		return minDistance;
	}	
}