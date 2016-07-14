package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;


public class Network {
	
	private TreeMap<Id, Link> selectedLinks = new TreeMap<Id, Link>();
	private LinkedList<Node> uniqueNodes;
	private NetworkImpl network;
	private final static Logger log = Logger.getLogger(Network.class);

	public static void main(String[] args) {
		Network network = new Network();
		network.run();
	}
	
	private void run() {
		new File(Config.getOutputFolder()).mkdir();
		network = Config.getNetwork();
		this.selectedLinks = new TreeMap<Id, Link>();
		for (Link link : network.getLinks().values()) {
			if (Config.isInsideSNetworkArea(link.getFromNode().getCoord()) || 
					Config.isInsideSNetworkArea(link.getToNode().getCoord())) {
				selectedLinks.put(link.getId(), link);
			}
		}	
		if (Config.baseFolder.contains("tele")) {
			this.correctManually();
		}
		
		this.getUniqueNodes();
		this.linkBack();
		
				
		writeOutLinksToKML(Config.getOutputFolder() + "links.kml", this.selectedLinks);
		writeOutNodesToKML(Config.getOutputFolder() + "nodes.kml",  this.uniqueNodes);		
		writeOutLinks(Config.getOutputFolder() + "links.xml", this.selectedLinks);
		writeOutNodes(Config.getOutputFolder() +  "nodes.xml", this.uniqueNodes);
		log.info("network files written ...");
	}
		
	private void linkBack() {
		for (Node node : this.uniqueNodes) {
			if (node.getOutLinks().isEmpty()) {
				if (!node.getInLinks().isEmpty()) {
					Link firstInLink = this.generateReturnLink((Link)node.getInLinks().values().toArray()[0], "out");
					this.selectedLinks.put(firstInLink.getId(), firstInLink);
					log.info("generated in-link " + firstInLink.getId());
				}	
				else {
					log.info("we have a problem with node: (no in-links)" + node.getId());
				}
			}			
			if (node.getInLinks().isEmpty()) {
				if (!node.getOutLinks().isEmpty()) {
					Link firstOutLink = this.generateReturnLink((Link)node.getOutLinks().values().toArray()[0], "in");
					this.selectedLinks.put(firstOutLink.getId(), firstOutLink);
					log.info("generated out-link " + firstOutLink.getId());
				}
				else {
					log.info("we have a problem with node: (no out-links)" + node.getId());
				}
			}
		}
	}
	
	private Link generateReturnLink(Link link, String inout) {
		LinkFactoryImpl linkFactory = new LinkFactoryImpl();
		
		Link returnLink = linkFactory.createLink(Id.create(link.getId().toString() + inout +"_back", Link.class), link.getToNode(), link.getFromNode(), network, 
				link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
		
		return returnLink;
	}
	
	private void correctManually() {
		// jelmoli
		String links2Remove[] = {
				"17560001595533FT",
				"17560001595533TF",
				"17560001595534FT",
				"17560001595510FT",
				"17560001595510TF",
				"17560001595536FT",
				"17560001595511FT",
				"17560001595511TF",
				"17560001687929FT",
				"17560001687929TF-dl",
				"17560001687929TF",
				"17560001595518FT",
				"17560001595518TF",
				"17560001687929FT-dl",
				"17560001595528FT-dl",
				"17560001595528TF",
				"17560001595517FT",
				"17560001595517TF",
				"17560001595528TF-dl",
				"17560001595528FT"
		};		
		for (String idstr:links2Remove) {
			Id<Link> id = Id.create(idstr, Link.class);
			this.selectedLinks.remove(id);
		}
		
		// add 4 jelmoli links
		LinkFactoryImpl linkFactory = new LinkFactoryImpl();
		Id<Link> id1 = Id.create("jelmoli-1", Link.class);
		Link jLink1 = linkFactory.createLink(
				id1, 
				this.network.getNodes().get(Id.create("17560200460795", Node.class)),
				this.network.getNodes().get(Id.create("17560200463426", Node.class)), 
				network, 
				1.0, 50.0 / 3.6, 1000.0, 1);
		this.selectedLinks.put(id1, jLink1);
		
		Id<Link> id2 = Id.create("jelmoli-2", Link.class);
		Link jLink2 = linkFactory.createLink(
				id2, 
				this.network.getNodes().get(Id.create("17560200463426", Node.class)),
				this.network.getNodes().get(Id.create("17560200460795", Node.class)), 
				network, 
				1.0, 50.0 / 3.6, 1000.0, 1);
		this.selectedLinks.put(id2, jLink2);
		
		Id<Link> id3 = Id.create("jelmoli-3", Link.class);
		Link jLink3 = linkFactory.createLink(
				id3, 
				this.network.getNodes().get(Id.create("17560200454496", Node.class)),
				this.network.getNodes().get(Id.create("17560200463426", Node.class)), 
				network, 
				1.0, 50.0 / 3.6, 1000.0, 1);
		this.selectedLinks.put(id3, jLink3);
		
		Id<Link> id4 = Id.create("jelmoli-4", Link.class);
		Link jLink4 = linkFactory.createLink(
				id4, 
				this.network.getNodes().get(Id.create("17560200463426", Node.class)),
				this.network.getNodes().get(Id.create("17560200454496", Node.class)), 
				network, 
				1.0, 50.0 / 3.6, 1000.0, 1);
		this.selectedLinks.put(id4, jLink4);
		
		Id<Link> id5 = Id.create("sihlbruecke-loop", Link.class);
		Link jLink5 = linkFactory.createLink(
				id5, 
				this.network.getNodes().get(Id.create("17560200133695-3", Node.class)),
				this.network.getNodes().get(Id.create("17560200133695-2", Node.class)), 
				network, 
				1.0, 50.0 / 3.6, 1000.0, 1);
		this.selectedLinks.put(id5, jLink5);				
	}
	
	private void getUniqueNodes() {
		this.uniqueNodes = getUniqueNodes(selectedLinks);
	}
		
	private static void writeOutNodesToKML(String path, LinkedList<Node> selectedNodes){
		BasicPointVisualizer bpv=new BasicPointVisualizer();	
		for (Node node: selectedNodes){
			bpv.addPointCoordinate(node.getCoord(), node.getId().toString(), Color.RED);
		}		
		bpv.write(path);	
	}
	
	private static void writeOutLinksToKML(String path, TreeMap<Id, Link> selectedLinks){
		BasicPointVisualizer bpv=new BasicPointVisualizer();	
		for (Link link: selectedLinks.values()){
			bpv.addPointCoordinate(link.getCoord(), link.getId().toString(), Color.RED);
		}		
		bpv.write(path);		
	}

	private LinkedList<Node> getUniqueNodes(TreeMap<Id, Link> selectedLinks) {
		for (Node node: this.network.getNodes().values()){
			node.getInLinks().clear();
			node.getOutLinks().clear();
		}
				
		LinkedList<Node> selectedNodes = new LinkedList<Node>();		
		for (Link link : selectedLinks.values()) {
			if (!selectedNodes.contains(link.getFromNode())) {
				selectedNodes.add(link.getFromNode());
				
				if (!link.getFromNode().getOutLinks().containsKey(link.getId())) {
					link.getFromNode().addOutLink(link);
				}
			}
			if (!selectedNodes.contains(link.getToNode())) {
				selectedNodes.add(link.getToNode());
				
				if (!link.getToNode().getInLinks().containsKey(link.getId())) {
					link.getToNode().addInLink(link);
				}
			}
		}
		return selectedNodes;
	}
	
	private static void writeOutNodes(String fileName, LinkedList<Node> selectedNodes) {
		ArrayList<String> list = new ArrayList<String>();

		list.add("<nodes>");
		for (Node node : selectedNodes) {
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append("\t<node>\n");
				stringBuffer.append("\t\t<id>" + node.getId() + "</id>\n");
				stringBuffer.append("\t\t<x>" + node.getCoord().getX() + "</x>\n");
				stringBuffer.append("\t\t<y>" + node.getCoord().getY() + "</y>\n");
				stringBuffer.append("\t</node>\n");
				list.add(stringBuffer.toString());
		}
		list.add("</nodes>\n");
		GeneralLib.writeList(list, fileName);
	}
	

	private static void writeOutLinks(String fileName, TreeMap<Id, Link> selectedLinks) {
		ArrayList<String> list = new ArrayList<String>();

		list.add("<links>");
		for (Link link : selectedLinks.values()) {		
			for (int ln = 0; ln < link.getNumberOfLanes(); ln++) {
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append("\t<link>\n");
				
				String prefix = "";
				if (ln > 0) {
					prefix = "_" + ln;
				}				
				stringBuffer.append("\t\t<id>" + link.getId() + prefix + "</id>\n");
							
				stringBuffer.append("\t\t<fromNode>" + link.getFromNode().getId() + "</fromNode>\n");
				stringBuffer.append("\t\t<toNode>" + link.getToNode().getId() + "</toNode>\n");
				stringBuffer.append("\t</link>\n");	
				list.add(stringBuffer.toString());
			}
		}
		list.add("</links>\n");
		GeneralLib.writeList(list, fileName);
	}
}
