/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraForSelectNodes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

// Zusätzlich Punkte innerhalb des aufgespannten Polygons finden?
// -> http://www.coding-board.de/board/showthread.php?t=23953 : 
// Herausfinden, ob ein beliebiger Punkt innerhalb eines Polygons liegt

package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.TravelCost;
import org.matsim.utils.misc.Time;

import playground.christoph.knowledge.utils.GetAllNodes;


public class DijkstraForSelectNodes {
	
	private static final Logger log = Logger.getLogger(DijkstraForSelectNodes.class);
	
	// Verkehrsnetz
	NetworkLayer network;
	
	// Verbindung zwischen Nodes und den zugehörigen DijkstraNodes herstellen.
	HashMap<Node, DijkstraNode> dijkstraNodeMap;
	
	// Kostenrechner
	TravelCost costCalculator = new FreespeedTravelTimeCost();

	// Zeit für den Kostenrechner
	double time = Time.UNDEFINED_TIME;
	
	// Initiale Länge der PriorityQueue. Wert aus Dijkstra.java übernommen.
	private static final int INITIAL_CAPACITY = 500;
	
	// alle Nodes des Netzwerks
	ArrayList<Node> networkNodes;
	
	
	// Knoten anhand ihres Abstandes zum Ursprungsknoten sortieren.
	private final Comparator<DijkstraNode> shortestDistanceComparator = new Comparator<DijkstraNode>()
	{
		public int compare(DijkstraNode a, DijkstraNode b)
	    {
			// note that this trick doesn't work for huge distances, close to Integer.MAX_VALUE
	        double diff = a.getMinDist() - b.getMinDist();
	        
	        // dist von b grösser als dist von a
	        if (diff < 0) return -1;

	        // dist von a grösser als dist von b
	        else if (diff > 0) return 1;
	        
	        // also dieselbe Distanz
	        else return 0;
	    }
	};
	    
	   
	// Liste von Knoten, sortiert nach ihrer Distanz zum Ursprungsknoten.
	private final PriorityQueue<DijkstraNode> unvisitedNodes = new PriorityQueue<DijkstraNode>(INITIAL_CAPACITY, shortestDistanceComparator);
	
	
	public DijkstraForSelectNodes(ArrayList<Node> networkNodes)
	{
		this.networkNodes = networkNodes;
		
		dijkstraNodeMap = new HashMap<Node, DijkstraNode>();
		DijkstraNode.setNodeMap(dijkstraNodeMap);
	}
	
	public DijkstraForSelectNodes(NetworkLayer network, ArrayList<Node> networkNodes)
	{
		this.network = network;
		this.networkNodes = networkNodes;
		
		dijkstraNodeMap = new HashMap<Node, DijkstraNode>();
		DijkstraNode.setNodeMap(dijkstraNodeMap);
		initDijkstraNodes();	
	}
	
	public void setNetwork(NetworkLayer network)
	{
		this.network = network;
	}
	
	public void setNetworkNodes(ArrayList<Node> nodes)
	{
		this.networkNodes = nodes;
	}
	
	protected void initDijkstraNodes()
	{		
		// aufräumen
//		dijkstraNodeMap.clear();
		
		for(int i = 0; i < networkNodes.size(); i++)
		{
			Node node = networkNodes.get(i);
			
			DijkstraNode dijkstraNode = new DijkstraNode(node);
			
			// DijkstraNodeMap füllen
			dijkstraNodeMap.put(node, dijkstraNode);
		}
		
	}

	// initialisieren
	private void init(DijkstraNode start)
	{
		// Knoten zurücksetzen
		resetDijkstraNodes();

	    unvisitedNodes.clear();
	 	        
	    // add source
	    setMinDistance(start, 0.0);
	    unvisitedNodes.add(start);
	}
	   
	
	// Kürzesten Weg für eine Route suche. Abbrechen, wenn dieser gefunden wurde.
	public void executeRoute(Node start, Node end)
	{	
		DijkstraNode startNode = dijkstraNodeMap.get(start);
		init(startNode);
	        
		DijkstraNode endNode = dijkstraNodeMap.get(end);
		
	    // the current node
	    DijkstraNode node;
	        
	    // extract the node with the shortest distance
	    while ((node = unvisitedNodes.poll()) != null)
	    {
	    	// Fehler abfangen, falls sich falscher Node eingeschlichen hat
	    	assert !isVisited(node);
	            
	        // Ziel erreicht -> abbrechen
	        if (node.getNode().equals(endNode.getNode())) break;
	            
	        node.setVisited(true);
	           
	        relaxNode(node);
	    }
	}

	// Kürzeste Wege zu allen Knoten des Netzwerks suchen.
	public void executeNetwork(Node start)
	{		
		DijkstraNode startNode = dijkstraNodeMap.get(start);
		init(startNode);
		
	    // the current node
	    DijkstraNode node;
	    
	    // extract the node with the shortest distance
	    while ((node = unvisitedNodes.poll()) != null)
	    {
	    	// Fehler abfangen, falls sich falscher Node eingeschlichen hat
	    	assert !isVisited(node);
	    	
	        node.setVisited(true);
	           
	        relaxNode(node);
	    }
	}
	
	// Map mit den Distanzen zu jedem Knoten zurückgeben.
	// Wird von ausserhalb ausgerufen, darum Rückgabe von
	// Nodes statt DijkstraNodes.
	public Map<Node, Double> getMinDistances()
	{
		Map<Node, Double> minDistances = new HashMap<Node, Double>();
		
		//for(int i = 0; i < DijkstraNodeMap.)
//		Map<Id, Node> myMap = (Map<Id, Node>)startNode.getOutNodes();
		
		Iterator nodeIterator = dijkstraNodeMap.values().iterator();
		while(nodeIterator.hasNext())
		{
			// Den DijkstraNode holen...
			DijkstraNode node = (DijkstraNode)nodeIterator.next();
			
			// ... dieser enthält den zugrundeliegenden Node und dessen kürzeste Wegdistanz
			minDistances.put(node.getNode(), node.getMinDist());
		}
		
		return minDistances;
	}
	
	
	// Knoten expandieren
	private void relaxNode(DijkstraNode node)
	{		
		ArrayList<Link> outgoingLinks = node.outgoingLinks();
		for (int i = 0; i < outgoingLinks.size(); i++)
		{
			// aktuellen Link holen
			Link link = outgoingLinks.get(i);
			
			// Zielknoten holen
			DijkstraNode toNode = dijkstraNodeMap.get(link.getToNode());
			
			// Falls Knoten noch nicht besucht wurde...
			if (!toNode.isVisited())
			{			
				double shortDist = node.getMinDist() + getLinkDist(link, time); 
	            
				// neue kürzeste Route zum Zielknoten gefunden?
				if (shortDist < toNode.getMinDist())
				{
					// neue "kürzeste" Distanz hinterlegen
					//toNode.setMinDist(shortDist);
					setMinDistance(toNode, shortDist);
		                                
					// neuen "vorherigen" Knoten hinterlegen
					//toNode.setPrevNode(node);
					setPreviousNode(toNode, node);
				}

			}
		}        
	}

	private boolean isVisited(DijkstraNode node)
	{
		return node.isVisited();
	}

	// für externen Aufruf
	public double getMinDistance(DijkstraNode node)
	{
		return node.getMinDist(); 
	}

	// Die "kürzeste" Distanz zu einem Knoten hinterlegen.
	// In diesem Zuge auch die Queue aktualisieren 
	// (Knoten entfernen und neu hinzufügen -> neu einordnen).
	// Fall der Knonten noch nicht in der Queue war -> neu hinzugefügt.
	private void setMinDistance(DijkstraNode node, double distance)
	{
		unvisitedNodes.remove(node);

		node.setMinDist(distance);

		// Knoten neu in der Queue einordnen
		unvisitedNodes.add(node);
	}
	
	// "Distanz" des übergebenen Links holen.
	// Dies kann z.B. dessen Länge oder die Fahrzeit auf dem Link sein.
	// Eventuell einen matsim CostCalculator verwenden, den man dann auch extern setzen kann?
	protected double getLinkDist(Link link, double time)
	{   
		return costCalculator.getLinkTravelCost(link, time);
		//return link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
		//return link.getLength();
	}

	public void setCostCalculator(TravelCost calculator)
	{
		costCalculator = calculator;
	}
	
	public TravelCost getCostCalculator()
	{
		return costCalculator;
	}
	
	public void setCalculationTime(double time)
	{
		this.time = time;
	}
	
	public double getCalculationTime()
	{
		return time;
	}
	
	public DijkstraNode getPreviousNode(DijkstraNode node)
	{
		return node.getPrevNode();
	}
	
	
	private void setPreviousNode(DijkstraNode a, DijkstraNode b)
	{
		a.setPrevNode(b);
	}
	
	// Knoten reseten: visited -> false; mindist -> Double.MAX_DOUBLE
	private void resetDijkstraNodes()
	{
		Iterator nodeIterator = dijkstraNodeMap.values().iterator();
		while(nodeIterator.hasNext())
		{
			// Den DijkstraNode holen...
			DijkstraNode node = (DijkstraNode)nodeIterator.next();
			
			node.reset();
		}
	}	// resetDijkstraNodes
	
}	// class DijkstraForSelectNodes


// Internes Datenkonstrukt, das Informationen über einen Knoten, 
// die Distanz zu diesem und seinen Vorgängerknoten enthält.
class DijkstraNode
{
	static private HashMap<Node, DijkstraNode> dijkstraNodeMap;
	
	Node node = null;
	DijkstraNode prevNode = null;
	boolean visited;
	double minDist;

	static void setNodeMap(HashMap<Node, DijkstraNode> map)
	{
		dijkstraNodeMap = map;
	}
	
	protected DijkstraNode(Node node) 
	{
		this.node = node;
		reset();
	}
	
	protected Node getNode()
	{
		return node;
	}
	
	protected void setPrevNode(DijkstraNode node)
	{
		prevNode = node;
	}
	
	protected DijkstraNode getPrevNode() 
	{
		return prevNode;
	}
	
	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public double getMinDist() 
	{
		return minDist;
	}

	public void setMinDist(double minDist) 
	{
		//System.out.println("Setting minDist..." + minDist + " for ID..." + node.getId().toString());
		this.minDist = minDist;
	}
	
	protected void reset()
	{
		visited = false;
		minDist = Double.MAX_VALUE;
	}
	
	protected ArrayList<DijkstraNode> outgoingNodes()
	{
		ArrayList<DijkstraNode> outgoingNodes = new ArrayList<DijkstraNode>();
		
		Map<Id, Node> myMap = (Map<Id, Node>)node.getOutNodes();
		
		Iterator nodeIterator = myMap.values().iterator();
		while(nodeIterator.hasNext())
		{
			Node node = (Node)nodeIterator.next();

			// zugehörigen DijsktraNode aus der Map holen
			DijkstraNode dijkstraNode = dijkstraNodeMap.get(node);
			outgoingNodes.add(dijkstraNode);
		}
		return outgoingNodes;
	}

	protected ArrayList<Link> outgoingLinks()
	{
		ArrayList<Link> outgoingLinks = new ArrayList<Link>();
		
		Map<Id, Link> myMap = (Map<Id, Link>)node.getOutLinks();
		
		Iterator linkIterator = myMap.values().iterator();
		while(linkIterator.hasNext())
		{
			Link link = (Link)linkIterator.next();
			outgoingLinks.add(link);
		}
		return outgoingLinks;
	}	
	
}	// class DijkstraNode
