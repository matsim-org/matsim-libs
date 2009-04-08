package playground.mmoyo.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTRouter.*;

/** 
 * Represent a network layer with independent route with special (transfer) links at intersections 
 * 
 */

public class PTNetworkFactory {
	// -> Get these values out from the city network
	// These are temporary values for the 5x5 scenario
	int maxNodekey = 24;
	int maxLinkKey = 79;

	private NetworkLayer cityNet;
	public NetworkLayer ptNetworkLayer;
	
	// This map stores the children nodes of each father node, necessary to
	// create the transfer between them
	public Map<Id, ArrayList<String>> childrenList = new TreeMap<Id, ArrayList<String>>();
	
	public PTNetworkFactory(NetworkLayer cityNet) {
		super();
		this.cityNet = cityNet;
	}

	public void createPTNetwork(List<PTLine> ptLineList) {
		// Read the route of every PTline and adds the corresponding links and
		// nodes
		for (PTLine ptLine: ptLineList) {
			boolean firstLink = true;
			String idFromNode = "";
			for (String strId : ptLine.getRoute()) {
				Link l = this.cityNet.getLink(strId);
				idFromNode = addToSubwayPTN(l, idFromNode, firstLink, ptLine.getId());
				firstLink = false;
			}
		}
		createTransferlinks();
	}

	public String addToSubwayPTN(Link l, String idFromNode, boolean firstLink,Id IdPTLine) {
		// Create the "Metro underground paths" related to the city network
		
		// Create FromNode
		if (firstLink) {
			maxNodekey++;
			idFromNode = String.valueOf(maxNodekey);
			addPTNode(idFromNode, l.getFromNode(), IdPTLine);
		}

		// Create ToNode
		maxNodekey++;
		String idToNode = String.valueOf(maxNodekey);
		addPTNode(idToNode, l.getToNode(), IdPTLine);// ToNode

		// Create the Link
		maxLinkKey++;
		this.createLink(maxLinkKey, idFromNode, idToNode, "Standard");

		return idToNode;
	}// AddToSub
	
	private void addWalkingNode(Id id) {
		//System.out.print(idImpl.toString() + " " + this.nodes.containsKey(idImpl) + " " );
		
		if (this.ptNetworkLayer.getNodes().containsKey(id)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		
		Node n = this.cityNet.getNode(id);
		PTNode node = new PTNode(id, n.getCoord(), n.getType());
		node.setIdFather(id);        //All ptnodes must have a father, including fathers (themselves)
		node.setIdPTLine(new IdImpl("Walk"));
		this.ptNetworkLayer.getNodes().put(id, node);
		n= null;
	}

	private void addPTNode(String strId, Node original, Id IdPTLine) {
		// Creates a underground clone of a node with a different ID
		Id id = new IdImpl(strId);
		Id idFather = new IdImpl(original.getId().toString());

		PTNode ptNode = new PTNode(id, original.getCoord(), original.getType(), idFather, IdPTLine);

		if (this.ptNetworkLayer.getNodes().containsKey(id)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		this.ptNetworkLayer.getNodes().put(id, ptNode);

		// updates this list of childrenNodes
		if (!childrenList.containsKey(idFather)) {
			ArrayList<String> ch = new ArrayList<String>();
			childrenList.put(idFather, ch);
		}
		childrenList.get(idFather).add(strId);

		id = null;
		idFather = null;
		ptNode = null;
		//i = null;
	}

	// To print nodes and his respective children
	public static void showMap(Map map) {
		Iterator iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
		iter = null;
	}

	private void createTransferlinks() {
		// (like stairs between lines in a subway station)
		Iterator it = childrenList.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			List <String>chList1 = (ArrayList) pairs.getValue();
			List <String>chList2 = (ArrayList) pairs.getValue();
			if (chList1.size() > 1) {
				for (String n1 : chList1) {
					for (String n2 : chList2) {// Create links between children nodes lines
						if (n1 != n2) {
							maxLinkKey++;
							this.createLink(maxLinkKey, n1, n2,"Transfer");
						}//if n1
					}//for iter2
				}// for iter1
			}// if chlist
		}// while
		it = null;
	}// CreateTransfer


	public List<String> createWalkingLinks(Id idFromNode, Id idToNode ){
		//Adds temporary the origin and destination node and create new temporary links between 
		//between them  and its respective children to the routing process
		
		addWalkingNode(idFromNode);
		addWalkingNode(idToNode);
		
		int i = 0;
		List<String> WalkingLinkList = new ArrayList<String>(); 
		
		//Starting links
		List<String> uChildren = this.childrenList.get(idFromNode);
		for (String uChild : uChildren) {
			this.createLink(--i, idFromNode.toString(), uChild , "Walking");
			WalkingLinkList.add(String.valueOf(i));
		}
		
		//Endings links
		uChildren = this.childrenList.get(idToNode);
		for (String uChild: uChildren) {
			this.createLink(--i, uChild, idToNode.toString(), "Walking");
			WalkingLinkList.add(String.valueOf(i));
		}
		return WalkingLinkList;
	}
	
	public void removeWalkinkLinks(List<String> WalkingLinkList){
		//Removes temporal links at the end of the ruting process
		for (String strWalkLink : WalkingLinkList) {
			ptNetworkLayer.removeLink(ptNetworkLayer.getLink(strWalkLink));
		}
	}
	
	public void removeWalkingNodes(Id node1, Id node2){
		//Removes temporal links at the end of the routing process
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode(node1));
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode(node2));
	}
	
	//->move this methos to LinkFactory
	// Creates only irrelevant values to create a PTLink. Actually the cost is calculated on other parameters
	private void createLink(int intId, String strIdFromNode, String strToNode, String type){
		Id id =  new IdImpl(intId);
		Node fromNode = this.ptNetworkLayer.getNode(strIdFromNode); 
		Node toNode = this.ptNetworkLayer.getNode(strToNode);
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		double freespeed= 1;
		double capacity = 1;
		double numLanes = 1;
		String origId = "0";

		this.ptNetworkLayer.createLink(id, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type); 
	}
	
	public void printLinks() {
		//Displays a quick visualization of links with from- and to- nodes
		for (Link l : this.ptNetworkLayer.getLinks().values()) {
			System.out.print("\n(" ); 
			System.out.print(l.getFromNode().getId().toString()); 
			System.out.print( ")----" ); 
			System.out.print( l.getId().toString() ); 
			System.out.print( "--->("); 
			System.out.print( l.getToNode().getId().toString() ); 
			System.out.print( ")   " + l.getType() ); 
			System.out.print( "      (" ); 
			System.out.print( ((PTNode) l.getFromNode()).getIdFather().toString()); 
			System.out.print( ")----" ); 
			System.out.print( l.getId().toString() ); 
			System.out.print( "--->(" ); 
			System.out.print( ((PTNode) l.getToNode()).getIdFather().toString() ); 
			System.out.print( ")");
		}
	}
}

/*
 * Old CODE
 *  public void setMaxNodekey(int maxNodekey) { 
 *  	this.maxNodekey = maxNodekey; 
 *  }
 * 
 * public void setMaxLinkKey(int maxLinkKey) { 
 *		this.maxLinkKey = maxLinkKey;
 * }
 * 
*/

