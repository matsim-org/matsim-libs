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
 * First version of network factory for the PTCase1 (with a main node for station) 
 * Represent a network layer with independent route with transfer links at intersections 
 * @param cityNet City layer with streets description 	
 */
public class PTNetworkFactory {
	int maxNodekey = 24;  	// -> Get these values should be got from the city network, not from here 
	int maxLinkKey = 79;    //  but these are temporary values for the 5x5 scenario
	private NetworkLayer cityNet;
	public NetworkLayer ptNetworkLayer;
	
	/** This map stores the children nodes of each father node, necessary to create the transfer between them*/
	public Map<Id, ArrayList<String>> childrenList = new TreeMap<Id, ArrayList<String>>();
	
	public PTNetworkFactory(NetworkLayer cityNet) {
		super();
		this.cityNet = cityNet;
	}

	/**
	 * Read the route of every PTline and adds the corresponding links and nodes
	 */
	public void createPTNetwork(List<PTLine> ptLineList) {
		for (PTLine ptLine: ptLineList) {
			boolean first = true;
			String idFromNode = "";
			for (Id idNode : ptLine.getNodeRoute()) {
				Node n = this.cityNet.getNode(idNode);
				/* WRONG!
				idFromNode = addPTLinks(l, idFromNode, firstLink, ptLine.getId());
				*/
				first = false;
			}
		}
		createTransferlinks();
	}

	/**
	 *  Creates a copy of the original nodes and link in the PTN layer 
	 */
	public String addPTLinks(Link l, String idFromNode, boolean firstLink,Id IdPTLine) {
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
	}
	
	private void addWalkingNode(Id id) {
		if (this.ptNetworkLayer.getNodes().containsKey(id)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		Node n = this.cityNet.getNode(id);
		PTNode node = new PTNode(id, n.getCoord(), n.getType());
		node.setIdStation(id);        //All ptnodes must have a father, including fathers (themselves)
		node.setIdPTLine(new IdImpl("Walk"));
		this.ptNetworkLayer.getNodes().put(id, node);
		n= null;
	}

	/**
	 * Create a PTNode out from a Node. Same coordinates different id
	 */
	private void addPTNode(String strId, Node original, Id IdPTLine) {
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
	}

	/**
	 * TreeMap printer, meant to be used to show father node and all its children 
	 */
	public static void showMap(Map map) {
		Iterator iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
		iter = null;
	}
	
	/**
	 *Creates transfer links that join all children nodes of a station
	 */
	private void createTransferlinks() {
		for(ArrayList<String> chList: childrenList.values() ){
			ArrayList<String> chList2 = chList; 
			if (chList.size() > 1) {
				for (String strId1 : chList) {
					for (String strId2 : chList2) {
						if (strId1.equals(strId2)) {
							maxLinkKey++;
							this.createLink(maxLinkKey, strId1, strId2,"Transfer");
						}
					}
				}
			}
		}
	}

	/**
	*Adds temporarily the origin and destination nodes and create new temporary links between 
	*they and its respective children to start the routing process
	*/
	public List<String> createWalkingLinks(Id idFromNode, Id idToNode ){
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
	
	/**
	 * Removes temporal walk links at the end of the each individual routing process so that they do not interfere in the next route request 
	 */
	public void removeWalkinkLinks(List<String> WalkingLinkList){
		for (String strWalkLink : WalkingLinkList) {
			ptNetworkLayer.removeLink(ptNetworkLayer.getLink(strWalkLink));
		}
	}

	/**
	 *Removes temporal walk nodes  at the end of the each individual routing process so that they do not interfere in the next route request
	 */
	public void removeWalkingNodes(Id node1, Id node2){
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode(node1));
		this.ptNetworkLayer.removeNode(this.ptNetworkLayer.getNode(node2));
	}
	

	/**
	 * Creates only irrelevant values to create a PTLink. Actually the cost is calculated on other parameters
	 */
	private void createLink(int intId, String strIdFromNode, String strToNode, String type){
		//->move this method to LinkFactory
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
	
	/**
	 * Displays a quick visualization of links with from- and to- nodes
	 */
	public void printLinks() {
		for (Link l : this.ptNetworkLayer.getLinks().values()) {
			System.out.print("\n(" ); 
			System.out.print(l.getFromNode().getId().toString()); 
			System.out.print( ")----" ); 
			System.out.print( l.getId().toString() ); 
			System.out.print( "--->("); 
			System.out.print( l.getToNode().getId().toString() ); 
			System.out.print( ")   " + l.getType() ); 
			System.out.print( "      (" ); 
			System.out.print( ((PTNode) l.getFromNode()).getIdStation().toString()); 
			System.out.print( ")----" ); 
			System.out.print( l.getId().toString() ); 
			System.out.print( "--->(" ); 
			System.out.print( ((PTNode) l.getToNode()).getIdStation().toString() ); 
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

