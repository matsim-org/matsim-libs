package playground.mmoyo.PTCase1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.Node;
import playground.mmoyo.PTRouter.PTNode;

//TODO: use iterators

/** 
 * Determines the optimal path of a passenger in a Public transport Network 
 * according to the information of timetables
 *  *
 * @param nodeList  PTNodes in stored a a Node-List
 * @param linkList  Collection of org.matsim.network.Link
 * @param OriginNode Node where the trip begins
 * @param DestinationNode Node where the trip must finish
 * @param ptLinkCostCalculator Class that contains the weight information of links
 * @param time Milliseconds after the midnight in which the trip must begin
 */

public class MiniDijkstra {
	public PTNode[] route;
	public int tripTime=0;
	
	private int nodeNumber;
	private int linkArray[][];
	private int at[][] = new int[2][nodeNumber];
	private int as[] = new int[nodeNumber];
	private List<Node> nodeList;
	private List<Link> linkList;
	private PTLinkCostCalculator ptLinkCostCalculator;
	
	public MiniDijkstra(List<Node> nodeListP, List<Link> linkList,  PTLinkCostCalculator ptLinkCostCalculatorP) {
		this.nodeList=nodeListP;
		this.linkList = linkList;
		this.nodeNumber = nodeList.size();
		this.linkArray = new int[nodeNumber][nodeNumber];
		this.ptLinkCostCalculator= ptLinkCostCalculatorP;
		as = new int[nodeNumber];
		at = new int[2][nodeNumber];
	}
	
	public void shortestPath(Node OriginNode, Node DestinationNode, int iniTime){
		////validate the nodes
		int iniPosition = nodeList.indexOf(OriginNode);
		int endPosition = nodeList.indexOf(DestinationNode);

		boolean notDone = true;
		boolean pathExists = true;
		boolean pathPossible = false;
		
		this.resetValues();
		
		//TODO: here must time be accumulated
		for (Iterator<Link> iter = linkList.iterator(); iter.hasNext();) {
			updateCost ((Link) iter.next(), iniTime);
		}
		
		as[iniPosition] = 1;
		at[0][iniPosition] = 0;
		at[1][iniPosition] = iniPosition;
		for (int i = 0; i < nodeNumber; i++) {
			if ((iniPosition != i) && (linkArray[iniPosition][i] > -1)) {
				at[0][i] = linkArray[iniPosition][i];
				at[1][i] = iniPosition;
			}
		}

		for (int i = 0; i < nodeNumber; i++)
			if (linkArray[iniPosition][i] > -1)
				pathPossible = true;

		while (notDone && pathExists && pathPossible) {
			double minT = Double.MAX_VALUE;
			int v = -1;
			for (int i = 0; i < nodeNumber; i++) {
				if ((as[i] == 0) && (at[0][i] < minT)) {
					minT = at[0][i];
					v = i;
				}
			}
			as[v] = 1;
			for (int i = 0; i < nodeNumber; i++) {
				if ((as[i] == 0) && (linkArray[v][i] > -1)) {
					if ((at[0][v] + linkArray[v][i]) < at[0][i]) {
						at[0][i] = at[0][v] + linkArray[v][i];
						at[1][i] = v;
					}
				}
			}
			if (as[endPosition] == 1)
				notDone = false;
			else {
				notDone = true;
				pathExists = false;
				for (int i = 0; i < nodeNumber; i++) {
					if ((as[i] == 0) && (at[0][i] < Integer.MAX_VALUE))
						pathExists = true;
				}// for
			}// else
		}// while
		
		if (pathExists && pathPossible) {
			List<Integer> nodePath = new ArrayList<Integer>();
			int n = endPosition;
			n = at[1][endPosition];
			while (n != iniPosition) {
				nodePath.add(n);
				n = at[1][n];
			}
			nodePath.add(n);

			// revert the result
			int i = nodePath.size();
			route = new PTNode[i];
			for (Iterator<Integer> iter = nodePath.listIterator(); iter.hasNext();) {
				route[--i] = (PTNode) nodeList.get(iter.next());
			}

		}// if
		else {
			System.out.println("There is no path from node " + OriginNode.getId().toString() + " to node " + OriginNode.getId().toString());
		}// else
	}// Constructor 

	private void resetValues(){
		for (int i = 0; i < nodeNumber; i++) {
			for (int x = 0; x < nodeNumber; x++) {
				linkArray[i][x] = -1;
			}
			as[i] = 0;
			at[0][i] = Integer.MAX_VALUE;
			at[1][i] = -1;
		}	
	}
	
	private void updateCost(Link link, int iniTime){
		int cost=ptLinkCostCalculator.cost(link,iniTime);
		
		int fromNodeIndex = nodeList.indexOf(link.getFromNode());
		int toNodeIndex = nodeList.indexOf(link.getToNode());
		linkArray[fromNodeIndex][toNodeIndex] = cost;
		linkArray[toNodeIndex][fromNodeIndex] = cost;
	}
	
}// Class