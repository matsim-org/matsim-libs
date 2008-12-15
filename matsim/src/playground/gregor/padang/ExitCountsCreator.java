/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPlansGeneratorAndNetworkTrimmer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.padang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.evacuation.EvacuationAreaFileReader;
import org.matsim.evacuation.EvacuationAreaLink;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.world.World;
import org.xml.sax.SAXException;

/**
 *@author glaemmel
 */
public class ExitCountsCreator {

	private final static Logger log = Logger.getLogger(ExitCountsCreator.class);

	//evacuation Nodes an Link
	private final static String saveLinkId = "el1";
	private final static String saveNodeAId = "en1";
	private final static String saveNodeBId = "en2";

	//	the positions of the evacuation nodes - for now hard coded
	// Since the real positions of this nodes not really matters
	// and for the moment we are going to evacuate Padang only,
	// the save nodes are located east of the city.
	// Doing so, the visualization of the resulting evacuation network is much clearer in respect of coinciding links.
	private final static String saveAX = "662433";
	private final static String saveAY = "9898853";
	private final static String saveBX = "662433";
	private final static String saveBY = "9898853";

	private HashMap<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();
	private final HashSet<Node> saveNodes = new HashSet<Node>();
	private final HashSet<Node> redundantNodes = new HashSet<Node>();
	private ArrayList<Link> countLink;


	private final NetworkLayer network;


	public ExitCountsCreator(NetworkLayer network, final HashMap<Id, EvacuationAreaLink> evacuationAreaLinks) {
		this.network = network;
		this.evacuationAreaLinks = evacuationAreaLinks;
		this.countLink = new ArrayList<Link>();
	}

	/**
	 * Creates links from all save nodes to the evacuation node A
	 *
	 * @param network
	 */
	private void addExitCounts() {

		this.network.createNode(saveNodeAId, saveAX, saveAY, null);
		this.network.createNode(saveNodeBId, saveBX, saveBY, null);
		for (Node node : this.network.getNodes().values()) {
			String nodeId =  node.getId().toString();
			if (isSaveNode(node) && !nodeId.equals(saveNodeAId) && !nodeId.equals(saveNodeBId)){
				for (Link l : node.getInLinks().values()){
					this.countLink.add(l);
				}

			}
		}
	}

	private void addOptionalLinks(){
		this.countLink.add(this.network.getLink("113963"));
		this.countLink.add(this.network.getLink("13963"));
		this.countLink.add(this.network.getLink("113756"));
		this.countLink.add(this.network.getLink("13756"));
		this.countLink.add(this.network.getLink("113729"));
		this.countLink.add(this.network.getLink("13729"));
		this.countLink.add(this.network.getLink("113662"));
		this.countLink.add(this.network.getLink("13662"));
		this.countLink.add(this.network.getLink("116191"));
		this.countLink.add(this.network.getLink("16191"));
		this.countLink.add(this.network.getLink("115991"));
		this.countLink.add(this.network.getLink("15991"));
		this.countLink.add(this.network.getLink("115242"));
		this.countLink.add(this.network.getLink("15242"));
		this.countLink.add(this.network.getLink("114745"));
		this.countLink.add(this.network.getLink("14745"));

	}


	/**
	 * @param node
	 * @return true if <code>node</node> is outside the evacuation area
	 */
	private boolean isSaveNode(final Node node) {
		return this.saveNodes.contains(node);
	}

	/**
	 * Returns true if <code>node</code> is redundant. A node is
	 * redundant if it is not next to the evacuation area.
	 *
	 * @param node
	 * @return true if <code>node</code> is redundant.
	 */
	private boolean isRedundantNode(final Node node) {
		return this.redundantNodes.contains(node);
	}


	public void run() {


		log.info(" * classifing nodes");
		classifyNodes();
		log.info(" * cleaning up the network");
		cleanUpNetwork();
		log.info(" * creating exit counts");
		addExitCounts();
		addOptionalLinks();
		createCounts();
		log.info("done");
	}

	/**
	 * Classifies the nodes. Nodes that are next to the evacuation area and
	 * reachable from inside the evacuation area will be classified as save
	 * nodes. Other nodes outside the evacuation area will be classified
	 * as redundant nodes.
	 *
	 * @param network
	 */
	private void classifyNodes() {
		/* classes:
		 * 0: default, assume redundant
		 * 1: redundant node
		 * 2: save nodes, can be reached from evacuation area
		 * 3: "normal" nodes within the evacuation area
		 */
		for (Node node : this.network.getNodes().values()) {
			int inCat = 0;
			for (Link link : node.getInLinks().values()) {
				if (this.evacuationAreaLinks.containsKey(link.getId())) {
					if ((inCat == 0) || (inCat == 3)) {
						inCat = 3;
					}	else {
						inCat = 2;
						break;
					}
				} else {
					if (inCat <= 1) {
						inCat = 1;
					} else {
						inCat = 2;
						break;
					}
				}
			}
			switch (inCat) {
				case 2:
					this.saveNodes.add(node);
					break;
				case 3:
					break;
				case 1:
				default:
					this.redundantNodes.add(node);
			}
		}

	}

	/**
	 * Removes all links and nodes outside the evacuation area except the
	 * nodes next to the evacuation area that are reachable from inside the
	 * evacuation area ("save nodes").
	 *
	 * @param network
	 */
	private void cleanUpNetwork() {

		ConcurrentLinkedQueue<Link> l = new ConcurrentLinkedQueue<Link>();
		for (Link link : this.network.getLinks().values()) {
			if (!this.evacuationAreaLinks.containsKey(link.getId())) {
				l.add(link);
			}
		}

		Link link = l.poll();
		while (link != null){
			this.network.removeLink(link);
			link = l.poll();
		}

		ConcurrentLinkedQueue<Node> n = new ConcurrentLinkedQueue<Node>();
		for (Node node : this.network.getNodes().values()) {
			if (isRedundantNode(node)) {
				n.add(node);
			}
		}

		Node node = n.poll();
		while (node != null) {
			this.network.removeNode(node);
			node = n.poll();
		}
		new NetworkCleaner().run(this.network);
	}

	public void createCounts() {
		Counts counts = new Counts();
		Count c;
		counts.setLayer("padang evacuation");
		counts.setDescription("");
		counts.setName("pdg evac");
		counts.setYear(2008);
		QueueLink ql;
		for (Link l : this.countLink) {
			ql = (QueueLink) l;
			System.out.println(ql);

			c = counts.createCount(l.getId(), l.getId().toString());
			if (c == null) {
				System.out.println(ql);
				continue;
			}
			for (int i = 3; i < 6; i++) {
				c.createVolume(i, 0);

			}
		}
		CountsWriter cw = new CountsWriter(counts, "./output/counts.xml");
		cw.write();
		log.info("counts written successfully to: ./output/counts.xml");
	}

	public static void main(String [] args) {
		String configFile = "./configs/evacuationConf.xml";

		Config config = Gbl.createConfig(new String[] {configFile});
		World world = Gbl.createWorld();

		System.out.println("reading network xml file... ");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("./networks/padang_net.xml");
		world.setNetworkLayer(network);
		world.complete();
		System.out.println("done. ");
		HashMap<Id,EvacuationAreaLink> links = new HashMap<Id,EvacuationAreaLink>();
		EvacuationAreaFileReader er = new EvacuationAreaFileReader(links);
		try {
			er.readFile(config.evacuation().getEvacuationAreaFile());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ExitCountsCreator ec = new ExitCountsCreator(network, links);
		ec.run();
	}
}

