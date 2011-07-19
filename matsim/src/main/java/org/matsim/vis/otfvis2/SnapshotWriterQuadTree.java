package org.matsim.vis.otfvis2;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;

public class SnapshotWriterQuadTree extends OTFServerQuadTree {

	private static final long serialVersionUID = 1L;

	private transient Network network;
	
	public SnapshotWriterQuadTree(Network network) {
		super(network);
		this.network = network;
	}

	@Override
	public void initQuadTree(OTFConnectionManager connect) {
		initQuadTree();
	}

	void initQuadTree() {
		for (Link link : network.getLinks().values()) {
			double middleEast = (link.getToNode().getCoord().getX() + link.getFromNode().getCoord().getX()) * 0.5 - this.minEasting;
			double middleNorth = (link.getToNode().getCoord().getY() + link.getFromNode().getCoord().getY()) * 0.5 - this.minNorthing;
			LinkHandler.Writer linkWriter = new LinkHandler.Writer();
			linkWriter.setSrc(link);
			this.put(middleEast, middleNorth, linkWriter);
		}
	}

}