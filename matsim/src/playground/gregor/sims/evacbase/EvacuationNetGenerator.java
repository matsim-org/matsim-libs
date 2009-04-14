package playground.gregor.sims.evacbase;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordImpl;
import org.xml.sax.SAXException;

public class EvacuationNetGenerator {
	private final static Logger log = Logger.getLogger(EvacuationNetGenerator.class);

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
	
	private final Map<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();
	private final HashSet<Node> saveNodes = new HashSet<Node>();
	private final HashSet<Node> redundantNodes = new HashSet<Node>();

	private final Config config;

	private final NetworkLayer network;


	public EvacuationNetGenerator(final NetworkLayer network, Config config) {
		this.network = network;
		this.config = config;
		
		
	}
	
	

	/**
	 * Creates links from all save nodes to the evacuation node A
	 *
	 * @param network
	 */
	private void createEvacuationLinks() {

		this.network.createNode(new IdImpl(saveNodeAId), new CoordImpl(saveAX, saveAY));
		this.network.createNode(new IdImpl(saveNodeBId), new CoordImpl(saveBX, saveBY));

		double capacity = 100000.;
		this.network.createLink(new IdImpl(saveLinkId), this.network.getNode(saveNodeAId), this.network.getNode(saveNodeBId), 10, 100000, capacity, 1);

		int linkId = 1;
		for (Node node : this.network.getNodes().values()) {
			String nodeId =  node.getId().toString();
			if (isSaveNode(node) && !nodeId.equals(saveNodeAId) && !nodeId.equals(saveNodeBId)){
				linkId++;
				String sLinkID = "el" + Integer.toString(linkId);
				this.network.createLink(new IdImpl(sLinkID), this.network.getNode(nodeId), this.network.getNode(saveNodeAId), 10, 100000, capacity, 1);
			}
		}
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
		log.info("generating evacuation net ...");
		log.info(" * reading evacuaton area file");
		readEvacuationAreaFile();
		log.info(" * classifing nodes");
		classifyNodes();
		log.info(" * cleaning up the network");
		cleanUpNetwork();
		log.info(" * creating evacuation links");
		createEvacuationLinks();
		log.info("done.");
	}

	private void readEvacuationAreaFile() {
		try {
			String evacuationAreaLinksFile = this.config.evacuation().getEvacuationAreaFile();
			new EvacuationAreaFileReader(this.evacuationAreaLinks).readFile(evacuationAreaLinksFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
}
