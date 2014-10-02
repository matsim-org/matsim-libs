package playground.dgrether.analysis.simsimanalyser;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

public class NetworkInverter {

	final private static Logger log = Logger.getLogger(NetworkInverter.class);

	private Network originalNetwork;

	private NetworkImpl invertedNetwork = null;

	public NetworkInverter(Network originalNet) {
		this.originalNetwork = originalNet;
	}

	public Network getInvertedNetwork() {
		if (this.invertedNetwork == null){
			invertNetwork();
		}
		return this.invertedNetwork;
	}

	private void invertNetwork(){
		this.invertedNetwork = NetworkImpl.createNetwork();
		int numberOfNodesGenerated = 0;
		int numberOfLinksGenerated = 0;

		for (Link link : this.originalNetwork.getLinks().values()) {
			this.invertedNetwork.createAndAddNode(Id.create(link.getId(), Node.class), link.getCoord());
			numberOfNodesGenerated++;
		}

		for (Node node : this.originalNetwork.getNodes().values()) {
			for (Link inLink : node.getInLinks().values()) {
				for (Link outLink : node.getOutLinks().values()) {
						numberOfLinksGenerated = this.createInvertedLink(inLink, outLink, numberOfLinksGenerated);
				}
			}
		}

		log.info("Generated " + numberOfNodesGenerated + " Nodes and " + numberOfLinksGenerated + " Links");
	}

	private int createInvertedLink(Link inLink, Link outLink, int numberOfLinksGenerated){
		Link link = this.invertedNetwork.createAndAddLink(new IdImpl(inLink.getId().toString() + "zzz" + outLink.getId().toString()), // start counting link ids with 1 instead of 0
				this.invertedNetwork.getNodes().get(inLink.getId()), this.invertedNetwork.getNodes().get(outLink.getId()),
				outLink.getLength(),
				outLink.getFreespeed(),
				outLink.getCapacity(),
				outLink.getNumberOfLanes());
		((LinkImpl) link).setType(((LinkImpl) outLink).getType());
		return numberOfLinksGenerated + 1;
	}

	public List<Link> convertInvertedNodesToLinks(List<Node> nodes) {
		List<Link> ret = new ArrayList<Link>(nodes.size());
		for (Node n : nodes){
			ret.add(this.originalNetwork.getLinks().get(n.getId()));
		}
		return ret;
	}

}
