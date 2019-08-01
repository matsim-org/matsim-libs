package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.protobuf.Osmformat;
import de.topobyte.osm4j.pbf.seq.BlockParser;
import de.topobyte.osm4j.pbf.seq.PrimParser;
import de.topobyte.osm4j.pbf.util.PbfUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Coord;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Log4j2
class NodesPbfParser extends BlockParser implements OsmHandler {

	private final Map<Long, Integer> nodeIdsOfInterest;

	@Getter
	private final Map<Long, LightOsmNode> nodes = new HashMap<>();
	@Getter
	private int counter = 0;

	@Override
	protected void parse(Osmformat.HeaderBlock block) {
		Osmformat.HeaderBBox bbox = block.getBbox();
		this.handle(PbfUtil.bounds(bbox));
	}

	@Override
	protected void parse(Osmformat.PrimitiveBlock block) throws IOException {

		PrimParser primParser = new PrimParser(block, false);

		for (Osmformat.PrimitiveGroup group : block.getPrimitivegroupList()) {
			if (group.hasDense()) {
				primParser.parseDense(group.getDense(), this);
			} else if (group.getNodesCount() > 0) {
				primParser.parseNodes(group.getNodesList(), this);
			} else if (group.getWaysCount() > 0) {
				// ways are supposed to occur after nodes in an osm file. Therefore stop parsing here
				throw new EOFException("don't want to parse all the stuff");
			}
		}
	}

	@Override
	public void handle(OsmBounds osmBounds) {

	}

	@Override
	public void handle(OsmNode osmNode) {
		counter++;

		if (nodeIdsOfInterest.containsKey(osmNode.getId())) {
			Coord coord = new Coord(osmNode.getLongitude(), osmNode.getLatitude());
			int numberOfWays = nodeIdsOfInterest.get(osmNode.getId());
			nodes.put(osmNode.getId(), new LightOsmNode(osmNode.getId(), numberOfWays, coord));
		}
		if (counter % 1000000 == 0) {
			log.info("Read: " + counter / 1000000 + "M nodes");
		}
	}

	@Override
	public void handle(OsmWay osmWay) {

	}

	@Override
	public void handle(OsmRelation osmRelation) {

	}

	@Override
	public void complete() {

	}
}
