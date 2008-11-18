package playground.mmoyo.PTRouter;

import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.utils.collections.QuadTree;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.Coord;

/**
 *  Searches in the Public Transport Network for the closest bus/train stops
 *  Replaces the search within an euclidian distance
 *	@param ptNetworkLayer 
 */
public class PTNProximity {
	private QuadTree<Node> ptQuadTree;
	
	public PTNProximity (NetworkLayer cityNetworkLayer, NetworkLayer ptNetworkLayer){
		createCityQuadTree (cityNetworkLayer,ptNetworkLayer);
	}
	
	public PTNProximity (NetworkLayer ptNetworkLayer){
		createPTQuadTree (ptNetworkLayer);
	}
	
	/**
	 * Creates a QuadTree out from the Public Transport Network
	 * It represent the maps the bus/train stops to be searched
	 * @param ptNetworkLayer 
	 */
	private void createCityQuadTree (NetworkLayer cityNetworkLayer, NetworkLayer ptNetworkLayer){
		ArrayList<Node> FatherNodeList = new ArrayList<Node>();
		Iterator iter = ptNetworkLayer.getNodes().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			PTNode ptNode= (PTNode)entry.getValue();
			Node node = cityNetworkLayer.getNode(ptNode.getIdFather());
			if (!FatherNodeList.contains(node)) {
				FatherNodeList.add(node);
			}	
		}
		createQuadTree (FatherNodeList);
	}
	
	
	/**
	 * Creates a QuadTree out from the Public Transport Network
	 * It represent the maps the bus/train stops to be searched
	 * @param ptNetworkLayer 
	 */
	private void createPTQuadTree (NetworkLayer ptNetworkLayer){
		ArrayList<Node> nodeList = new ArrayList<Node>();
		Iterator iter = ptNetworkLayer.getNodes().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			PTNode ptNode= (PTNode)entry.getValue();
			if (!nodeList.contains(ptNode)) {
				nodeList.add(ptNode);
			}	
		}
		createQuadTree (nodeList);
	}
	
	private void createQuadTree (ArrayList<Node>nodeList){
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;

		for (Iterator<Node> iter = nodeList.iterator(); iter.hasNext();) {
			Node node= (Node)iter.next();
			//Define Bounds
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
		}
		minX -= 1.0;
		minY -= 1.0;
		maxX += 1.0;
		maxY += 1.0;
		
		this.ptQuadTree = new QuadTree<Node>(minX, minY, maxX, maxY);

		for (Iterator<Node> iter = nodeList.iterator(); iter.hasNext();) {
			Node node = iter.next();	
			this.ptQuadTree.put(node.getCoord().getX(), node.getCoord().getY(), node);
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
	public PTNode[] getNearestBusStops(Coord coord, int distance){
		double x = coord.getX();
		double y= coord.getY();
		Collection<Node> stopList = this.ptQuadTree.get(x, y, distance);
		if (stopList.size() == 0) {
			Node node1 = this.ptQuadTree.get(x, y);
			if (node1!= null) {
				stopList.add(node1);
			}
		}
		return stopList.toArray(new PTNode[stopList.size()]);
	}
	
	public PTNode getNearestNode(final double x, final double y){
		return (PTNode)ptQuadTree.get(x, y);
	}
	
	public void printNearesBusStops(Node node, int distance){
		Node[] stops = getNearestBusStops(node.getCoord(), distance);
		System.out.println ("Bus stops near " + node.getId().toString());
		for(int x=0; x <stops.length;x++){
			System.out.println (stops[x].getId().toString());
		}
	}
	
	public void dumpNet(){
		System.out.println("Size of the Quedtreemap:" + this.ptQuadTree.size());
		Node node;
		for (Iterator<Node> iter = this.ptQuadTree.values().iterator(); iter.hasNext();) {
			node = iter.next();	
			System.out.println(node.getId());
		}		
	}
}
