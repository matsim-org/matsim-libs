package playground.mmoyo.PTCase1;

import java.util.Iterator;
import java.util.ArrayList;
import org.matsim.utils.collections.QuadTree;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import playground.mmoyo.PTRouter.PTNode;

/**
 *  Searches in the Public Transport Network for the closest bus/train stops
 *  Replaces the search within an euclidian distance
 *	@param ptNetworkLayer 
 */
public class PTNProximity
{
	private QuadTree<Node> ptQuadTree;
	
	public PTNProximity (NetworkLayer cityNetworkLayer, NetworkLayer ptNetworkLayer){
		createCityQuadTree (cityNetworkLayer,ptNetworkLayer);

	}
	
	public PTNProximity(NetworkLayer ptNetworkLayer){
		createPTQuadTree (ptNetworkLayer);
	}
	
	/**
	 * Creates a QuadTree out from the Public Transport Network
	 * It represent the maps the bus/train stops to be searched
	 * @param ptNetworkLayer 
	 */
	private void createCityQuadTree (NetworkLayer cityNetworkLayer, NetworkLayer ptNetworkLayer){
		ArrayList<Node> FatherNodeList = new ArrayList<Node>();
		
		for (Node node: ptNetworkLayer.getNodes().values()) {
			PTNode ptnode = (PTNode)node;
			Node fatherNode= cityNetworkLayer.getNode(ptnode.getIdFather());
			if (!FatherNodeList.contains(fatherNode)) {
				FatherNodeList.add(fatherNode);
			}
		}
		/*
		Iterator iter = ptNetworkLayer.getNodes().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			PTNode ptNode= (PTNode)entry.getValue();
			Node node = cityNetworkLayer.getNode(ptNode.getIdFather());
			if (!FatherNodeList.contains(node)) {
				FatherNodeList.add(node);
			}	
		}
		*/
		
		createQuadTree (FatherNodeList);
	}
	
	
	/**
	 * Creates a QuadTree out from the Public Transport Network
	 * It represent the maps the bus/train stops to be searched
	 * @param ptNetworkLayer 
	 */
	private void createPTQuadTree (NetworkLayer ptNetworkLayer){
		ArrayList<Node> nodeList = new ArrayList<Node>();
		/*
		Iterator <Map.Entry>iter = ptNetworkLayer.getNodes().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			PTNode ptNode= (PTNode)entry.getValue();
			if (!nodeList.contains(ptNode)) {
				nodeList.add(ptNode);
			}	
		}
		*/

		for (Node node: ptNetworkLayer.getNodes().values()) {
			if (!nodeList.contains(node)) {
				nodeList.add(node);
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
	
	/*
	public PTNode[] getNearestBusStops(Coord coord, int distance, boolean force){
		double x = coord.getX();
		double y= coord.getY();
		Collection<Node> stopList = this.ptQuadTree.get(x, y, distance);
		if (stopList.size() == 0) {
			Node node1 = this.ptQuadTree.get(x, y);
			
			//TODO: The agent is forced to walk more than the distance, must be adjusted.
			if (node1!= null && force) {
				distance =(int)coord.calcDistance(node1.getCoord()) + 1;
				stopList = this.ptQuadTree.get(x, y, distance);
			}
	
		}
		return stopList.toArray(new PTNode[stopList.size()]);
	}
	*/
	
	
	
	public PTNode getNearestNode(final double x, final double y){
		return (PTNode)ptQuadTree.get(x, y);
	}
	
	/*
	public void printNearestBusStops(Node node, int distance){
		System.out.println ("Bus stops near " + node.getId().toString());
		printNearestBusStops(node.getCoord(), distance);
	}
	
	
	public void printNearestBusStops(Coord coord, int distance){
		System.out.println ("Bus stops near " + coord.getX() + ", " + coord.getY());
		Node[] stops = getNearestBusStops(coord, distance, false);
		for(int x=0; x <stops.length;x++){
			System.out.println (stops[x].getId().toString());
		}
	}
	*/
	
	public void dumpNet(){
		System.out.println("Size of the Quedtreemap:" + this.ptQuadTree.size());
		Node node;
		for (Iterator<Node> iter = this.ptQuadTree.values().iterator(); iter.hasNext();) {
			node = iter.next();	
			System.out.println(node.getId());
		}		
	}
}
