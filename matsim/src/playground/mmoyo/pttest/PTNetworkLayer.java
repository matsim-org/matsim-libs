package playground.mmoyo.pttest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class PTNetworkLayer extends NetworkLayer {
	// TODO: Get these values out from the city network
	// These are temporary values for the 5x5 scenario
	int maxNodekey = 24;
	int maxLinkKey = 79;

	private NetworkLayer cityNet;
	// TODO!!!!: make a Quadtree structure according to Marcel suggestion to
	// optimize the bus stop search

	// TODO!!!!: Create a cost function, this s only the first test
	String length = "5.0";

	// This map stores the children nodes of each father node, necessary to
	// create the transfer between them
	public Map<IdImpl, ArrayList<String>> childrenList = new TreeMap<IdImpl, ArrayList<String>>();

	private List<Id> transferLList;
	private List<Id> transferNList;

	public PTNetworkLayer(NetworkLayer cityNet) {
		super();
		this.cityNet = cityNet;
	}

	public PTNetworkLayer() {
		super();
	}

	public void CreatePTNetwork(List<PTLine> ptLineList) {
		// Read the route of every PTline and adds the corresponding links and
		// nodes
		for (Iterator<PTLine> iterPTLines = ptLineList.iterator(); iterPTLines.hasNext();) {
			PTLine ptLine = iterPTLines.next();
			boolean firstLink = true;
			String idFromNode = "";
			for (Iterator<String> iter = ptLine.strLinksRoute2.iterator(); iter.hasNext();) {
				Link l = this.cityNet.getLink(iter.next());
				idFromNode = AddToSubwayPTN(l, idFromNode, firstLink, ptLine.getId());
				firstLink = false;
			}// for iter
		}// for iterPTLines
		CreateTransferlinks();
	}// CreatePTNetwork

	private void AddPTNodeV(Node original) {
		PTNode ptNode = new PTNode(new IdImpl(original.getId().toString()),	String.valueOf(original.getCoord().getX()), String.valueOf(original.getCoord().getY()), original.getType());
		if (this.nodes.containsKey(original.getId())) {
			throw new IllegalArgumentException(this + "[id=" + original.getId().toString() + " already exists]");
		}
		this.nodes.put(original.getId(), ptNode);
	}

	public String AddToSubwayPTN(Link l, String idFromNode, boolean firstLink,IdImpl IdPTLine) {
		// Create the "Metro underground paths" related to the city network

		// Create FromNode
		if (firstLink) {
			maxNodekey++;
			idFromNode = String.valueOf(maxNodekey);
			AddPTNode(idFromNode, l.getFromNode(), IdPTLine);// FromNode
		}

		// Create ToNode
		maxNodekey++;
		String idToNode = String.valueOf(maxNodekey);
		AddPTNode(idToNode, l.getToNode(), IdPTLine);// ToNode

		// Create the Link
		maxLinkKey++;
		this.createLink(String.valueOf(maxLinkKey), idFromNode, idToNode,String.valueOf(l.getLength()), String.valueOf(l.getFreespeed(0)), String.valueOf(l.getCapacity(0)), String.valueOf(l.getLanesAsInt(0)), l.getOrigId(), l.getType());
		return idToNode;
	}// AddToSub

	private void AddPTNode(String id, Node original, IdImpl IdPTLine) {
		// Creates a underground clone of a node with a different ID
		IdImpl idImpl = new IdImpl(id);
		IdImpl idFather = new IdImpl(original.getId().toString());

		PTNode ptNode = new PTNode(idImpl, String.valueOf(original.getCoord().getX()), String.valueOf(original.getCoord().getY()), original.getType(), idFather, IdPTLine);
		Id i = new IdImpl(id);
		if (this.nodes.containsKey(i)) {
			throw new IllegalArgumentException(this + "[id=" + id + " already exists]");
		}
		this.nodes.put(i, ptNode);

		// updates this list of childrenNodes
		if (!childrenList.containsKey(idFather)) {
			ArrayList<String> ch = new ArrayList<String>();
			childrenList.put(idFather, ch);
		}
		childrenList.get(idFather).add(id);

		idImpl = null;
		idFather = null;
		ptNode = null;
		i = null;

	}// AddPTN

	// To print nodes and his respective children
	public static void showMap(Map map) {
		Iterator iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}
		iter = null;
	}

	private void CreateTransferlinks() {
		// (like stairs between lines in a subway station)
		Iterator it = childrenList.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			List chList1 = (ArrayList) pairs.getValue();
			List chList2 = (ArrayList) pairs.getValue();

			AddPTNodeV(this.cityNet.getNode(pairs.getKey().toString()));

			if (chList1.size() > 1) {
				String n1 = "";
				String n2 = "";
				for (Iterator<String> iter1 = chList1.iterator(); iter1.hasNext();) {
					n1 = iter1.next();
					// walking links with value null between the node father and the clones
					maxLinkKey++;
					this.createLink(String.valueOf(maxLinkKey), n1, pairs.getKey().toString(), "1", "1.0", "1.0", "1", "0","??");
					maxLinkKey++;
					this.createLink(String.valueOf(maxLinkKey), pairs.getKey().toString(), n1, "1", "1.0", "1.0", "1", "0", "??");

					// Create links between children nodes lines
					for (Iterator<String> iter2 = chList2.iterator(); iter2.hasNext();) {
						n2 = iter2.next();
						if (n1 != n2) {
							maxLinkKey++;
							this.createLink(String.valueOf(maxLinkKey), n1, n2,length, "25.185185185185187", "25900.20064", "1", "0", "??");
						}
					}
				}// for iter1
			}// if chlist
		}// while
		it = null;
	}// CreateTransfer

	public PTNode CreateWalkingLinks(Node original, boolean to) {
		this.transferLList = new ArrayList<Id>();
		this.transferNList = new ArrayList<Id>();
		int i = ++maxNodekey;

		IdImpl idImpl = new IdImpl(String.valueOf(i));
		IdImpl idFather= null;
		
		try {
			idFather = new IdImpl(original.getId().toString());
		} catch (NullPointerException e) {
			System.out.println("El nodo no existe" +  e.toString());
			return null;
		}
		
		IdImpl idPTLine = new IdImpl(" ");
		PTNode ptNode = new PTNode(idImpl, String.valueOf(original.getCoord().getX()), String.valueOf(original.getCoord().getY()), original.getType(), idFather, idPTLine);

		if (this.nodes.containsKey(idImpl)) {
			throw new IllegalArgumentException(this + "[id=" + idImpl + " already exists]");
		}
		this.nodes.put(idImpl, ptNode);
		this.transferNList.add(idImpl);

		// link Representing the distance the origin to the next bus stop
		// Buscar el nodo real en la lista de padres de los ptn
		ArrayList uChildren = this.childrenList.get(idFather);

		String idFromNode = "";
		String idToNode = "";

		if (to) {
			idFromNode = ptNode.getId().toString();
		} else {
			idToNode = ptNode.getId().toString();
		}

		for (Iterator<String> iter = uChildren.iterator(); iter.hasNext();) {
			if (to) {
				idToNode = iter.next();
			} else {
				idFromNode = iter.next();
			}
			i = ++maxLinkKey;
			IdImpl IdImplLink = new IdImpl(String.valueOf(i));
			// TODO: for the time being is the cost 1
			// This must not be so!!
			this.createLink(IdImplLink.toString(), idFromNode, idToNode, "1.0",	"25.185185185185187", "25900.20064", "1", "0", "??");
			this.transferLList.add(IdImplLink);
		}
		return ptNode;
	}// CreateTransfer

}// Class

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
 * public void Fill(PTLine ptLine){ 
 * //Read the route of every PTline and adds the corresponding links and nodes 
 * 		for (Iterator <String> iter =  ptLine.strLinksRoute2.iterator(); iter.hasNext();) { 
 * 			String strLink = iter.next(); 
 * 			AddNode(this.cityNet.getLink(strLink).getFromNode()); //FromNode
 * 			AddNode(this.cityNet.getLink(strLink).getToNode());//ToNode
 * 			AddLink(this.cityNet.getLink(strLink)); 
 * 		} 
 * }//Fill
 * 
 * class Children{ 
 * 		String idFather; 
 * 		ArrayList<String> childrenList = new ArrayList <String>(); 
 * 		public Children(String idFather){ 
 * 			this.idFather= idFather; 
 * 		} 
 * }
 * 
 * public void CleanTransfers(){ 
 * 		for (int i = 0; i< transferLList.size(); i++) {
 * 			this.getLinks().remove(transferLList.get(i)); //Link 
 * 			l = this.getLinks().get(transferLList.get(i)); 
 * 			//System.out.println("value of trasferlList:" + transferLList.get(i)); 
 * 		}
 *  	for (int i =0; i < transferNList.size();i++){
 * 			this.nodes.remove(transferNList.get(i)); 
 * 		}
 * }
 */