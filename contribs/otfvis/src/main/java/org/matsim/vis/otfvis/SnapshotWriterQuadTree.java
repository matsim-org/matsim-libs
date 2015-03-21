package org.matsim.vis.otfvis;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler.Writer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.snapshotwriters.VisVehicle;

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

	public void initQuadTree() {
		for (final Link link : network.getLinks().values()) {
			Coord fromCoord = link.getFromNode().getCoord();
			Point2D.Double from = transform(fromCoord);
			Coord toCoord = link.getToNode().getCoord();
			Point2D.Double to = transform(toCoord);
			double middleEast = (to.getX() + from.getX()) * 0.5;
			double middleNorth = (to.getY() + from.getY()) * 0.5;
			Writer linkWriter = new OTFLinkAgentsHandler.Writer();
			linkWriter.setSrc(new VisLink () {

				@Override
				public VisData getVisData() {
					return new VisData() {

						@Override
						public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
							return Collections.emptyList();
						}
						
					};
				}

				@Override
				public Link getLink() {
					return link;
				}

				@Override
				public Collection<? extends VisVehicle> getAllVehicles() {
					return Collections.emptyList();
				}
				
			});
			this.put(middleEast, middleNorth, linkWriter);
		}
	}

}