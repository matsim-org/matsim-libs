package playground.mmoyo.pttest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.Node;

public class MiniDijkstra {
	public PTNode[] route;

	public MiniDijkstra(List<Node> nodeList, List<Link> linkList, Node OriginNode, Node DestinationNode) {

		int iniPosition = nodeList.indexOf(OriginNode);
		int endPosition = nodeList.indexOf(DestinationNode);

		if (iniPosition == -1) {
			//throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
			System.out.println("The Origin Node does not not exist");
		}
		if (endPosition == -1) {
			//throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
			System.out.println("The Destination Node does not not exist");
		}

		List<Integer> nodePath = new ArrayList<Integer>();
		int linkArray[][] = new int[nodeList.size()][nodeList.size()];
		boolean notDone = true;
		boolean pathExists = true;
		boolean pathPossible = false;
		int as[] = new int[nodeList.size()];
		int at[][] = new int[2][nodeList.size()];

		for (int i = 0; i < nodeList.size(); i++) {
			for (int x = 0; x < nodeList.size(); x++) {
				linkArray[i][x] = -1;
			}
			as[i] = 0;
			at[0][i] = Integer.MAX_VALUE;
			at[1][i] = -1;
		}

		for (int i = 0; i < linkList.size(); i++) {
			int r = nodeList.indexOf(linkList.get(i).getFromNode());
			int c = nodeList.indexOf(linkList.get(i).getToNode());
			linkArray[r][c] = Cost(linkList.get(i));
			linkArray[c][r] = Cost(linkList.get(i));
		}

		as[iniPosition] = 1;
		at[0][iniPosition] = 0;
		at[1][iniPosition] = iniPosition;
		for (int i = 0; i < nodeList.size(); i++) {
			if ((iniPosition != i) && (linkArray[iniPosition][i] > -1)) {
				at[0][i] = linkArray[iniPosition][i];
				at[1][i] = iniPosition;
			}
		}

		for (int i = 0; i < nodeList.size(); i++)
			if (linkArray[iniPosition][i] > -1)
				pathPossible = true;

		while (notDone && pathExists && pathPossible) {
			double minT = Double.MAX_VALUE;
			int v = -1;
			for (int i = 0; i < nodeList.size(); i++) {
				if ((as[i] == 0) && (at[0][i] < minT)) {
					minT = at[0][i];
					v = i;
				}
			}
			as[v] = 1;
			for (int i = 0; i < nodeList.size(); i++) {
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
				for (int i = 0; i < nodeList.size(); i++) {
					if ((as[i] == 0) && (at[0][i] < Integer.MAX_VALUE))
						pathExists = true;
				}// for
			}// else
		}// while
		
		if (pathExists && pathPossible) {
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
	}// Router

	private int Cost(Link link) {
		// TODO:!!! get here the real cost of travel through this link
		/*
		 * switch (linktype){ 
		 * case 1: Cost= Traveltime; 	//NormalPtlink 
		 * case 2: cost= TransferRate; 	//Transferlin 
		 * case 3: Cost= 0; 			//Walking Link: }
		 */
		return (int) link.getLength();
	}// Cost

}// Class
