package org.matsim.vis.otfvis2;

import java.awt.geom.Point2D;

import org.matsim.api.core.v01.Coord;
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
			Coord fromCoord = link.getFromNode().getCoord();
			Point2D.Double from = transform(fromCoord);
			Coord toCoord = link.getToNode().getCoord();
			Point2D.Double to = transform(toCoord);
			double middleEast = (to.getX() + from.getX()) * 0.5;
			double middleNorth = (to.getY() + from.getY()) * 0.5;
			LinkHandler.Writer linkWriter = new LinkHandler.Writer();
			linkWriter.setSrc(link);
			this.put(middleEast, middleNorth, linkWriter);
		}
	}

}