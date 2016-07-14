package patryk.utils;

import java.util.ArrayList;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class LinksRemover {

	private Network network;
	private ArrayList<Link> linksToRemove;
	private ArrayList<Node> affectedNodes;
	private ArrayList<Node> nodesToRemove;
	
	public LinksRemover(Network network) {
		this.network = network;
		this.linksToRemove = new ArrayList<>();
		this.affectedNodes = new ArrayList<>();
		this.nodesToRemove = new ArrayList<>();
	}

	public void run() {
		
		for(Link link : network.getLinks().values()) {
			double freeSpeed = link.getFreespeed();
			double capacity = link.getCapacity();
			Set<String> modes = link.getAllowedModes();
			Node fromNode = link.getFromNode();
			Node toNode = link.getToNode();
						
			// Activities should not be placed on motorways (high freespeed) 
			// inactive links (cap=0 or freeSpeed=0),
			// or links where cars cannot drive
			if (freeSpeed > 27 || capacity < 1 || freeSpeed < 1 || !(modes.contains("car"))) {
				linksToRemove.add(link);
				if(toNode != null) {
					affectedNodes.add(toNode);
				}
				if (fromNode != null) {
					affectedNodes.add(fromNode);
				}
			}
		}
		
		removeLinks();
		checkNodes();
		removeNodes();
	}
	
	private void removeLinks() {
		for(int i = 0; i < linksToRemove.size(); i++) {
			network.removeLink(linksToRemove.get(i).getId());
			System.out.println("Link " + linksToRemove.get(i).getId().toString() + " removed");
		}
	}
	
	private void checkNodes() {
		for (Node n : network.getNodes().values()) {
			if (n.getInLinks().isEmpty() && n.getOutLinks().isEmpty()) {
				nodesToRemove.add(n);
			}
		}
	}
	
	private void removeNodes() {
		for(int i = 0; i < nodesToRemove.size(); i++) {
			network.removeNode(nodesToRemove.get(i).getId());
			System.out.println("Node " + nodesToRemove.get(i).getId().toString() + " removed");
		}
	}

}
