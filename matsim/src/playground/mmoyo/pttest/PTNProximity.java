package playground.mmoyo.pttest;

import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import org.matsim.utils.collections.QuadTree;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

/**
 *  Searches in the Public Transport Network for the closest bus/train stops
 *  Replaces the search within an euclidian distance
 *	@param ptNetworkLayer 
 */
public class PTNProximity {
	private QuadTree<Node> PTQuadTree;
	
	public PTNProximity (NetworkLayer cityNetworkLayer, PTNetworkLayer ptNetworkLayer){
		createQuadTree (cityNetworkLayer,ptNetworkLayer);
	}
	
	/**
	 * Creates a QuadTree out from the Public Transport Network
	 * It represent the maps the bus/train stops to be searched
	 * @param ptNetworkLayer 
	 */
	private void createQuadTree (NetworkLayer cityNetworkLayer, PTNetworkLayer ptNetworkLayer){
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;

		List<Node> FatherNodeList = new ArrayList<Node>();
		
		Iterator iter = ptNetworkLayer.getNodes().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			PTNode ptNode= (PTNode)entry.getValue();
			Node node = cityNetworkLayer.getNode(ptNode.getIdFather());
			if (!FatherNodeList.contains(node)) {
				FatherNodeList.add(node);
				
				//Define Bouns
				if (node.getCoord().getX() < minX) { 
					minX = node.getCoord().getX(); 
				}
				if (node.getCoord().getY() < minY) { 
					minY = node.getCoord().getY(); 
				}
				if (node.getCoord().getX() > maxX) { 
					maxX = node.getCoord().getX(); 
				}
				if (node.getCoord().getY() > maxY) { 
					maxY = node.getCoord().getY(); 
				}
			
			}//if !FatherNodeList	
		}//While iter
		iter = null;
		this.PTQuadTree = new QuadTree<Node>(minX, minY, maxX, maxY);

		for (Iterator<Node> iter2 = FatherNodeList.iterator(); iter2.hasNext();) {
			Node node = iter2.next();	
			this.PTQuadTree.put(node.getCoord().getX(), node.getCoord().getY(), node);
		}
	}//CreateQuadTree
	
	
	/**
	 * Returns a list of bus/train stops after the query 
	 * If bus/train stops are found in the distance required, return the closes stop.
	 *
	 * @param searchRadius
	 * @param startPoint
	 * @return list of nearest nodes
	 */
	public Node[] getNearestBusStops(Node node, int distance){
		Collection<Node> stopList = this.PTQuadTree.get(node.getCoord().getX(), node.getCoord().getY(), distance);
		if (stopList.size() == 0) {
			Node node1 = this.PTQuadTree.get(node.getCoord().getX(), node.getCoord().getY());
			if (node1!= null) {
				stopList.add(node1);
			}
		}
		return stopList.toArray(new Node[stopList.size()]);
	}
	
	public void printNearesBusStops(Node node, int distance){
		Node[] stops = getNearestBusStops(node, distance);
		System.out.println ("Bus stops near " + node.getId().toString());
		for(int x=0; x< stops.length;x++){
			System.out.println (stops[x].getId().toString());
		}
	}
	
	public void dumpNet(){
		System.out.println(this.PTQuadTree.size());
		Node node;
		for (Iterator<Node> iter = this.PTQuadTree.values().iterator(); iter.hasNext();) {
			node = iter.next();	
			System.out.println(node.getId());
		}		
	}
}