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

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Log4j2
class WaysPbfParser extends BlockParser implements OsmHandler {

	@Getter
	private final Set<OsmWay> ways = new HashSet<>();
	@Getter
	private final Map<Long, Integer> nodes = new HashMap<>();
	private final Map<String, LinkProperties> linkProperties;

	@Getter
	private int counter = 0;

	private static boolean isStreetOfInterest(OsmWay way, Map<String, LinkProperties> linkProperties) {
		for (int i = 0; i < way.getNumberOfTags(); i++) {
			String tag = way.getTag(i).getKey();
			String tagvalue = way.getTag(i).getValue();
			if (tag.equals(OsmTags.HIGHWAY) && linkProperties.containsKey(tagvalue)) return true;
		}
		return false;
	}

	@Override
	protected void parse(Osmformat.HeaderBlock block) {
		Osmformat.HeaderBBox bbox = block.getBbox();
		this.handle(PbfUtil.bounds(bbox));
	}

	@Override
	protected void parse(Osmformat.PrimitiveBlock block) throws IOException {

		PrimParser primParser = new PrimParser(block, false);

		for (Osmformat.PrimitiveGroup group : block.getPrimitivegroupList()) {
			if (group.getWaysCount() > 0) {
				primParser.parseWays(group.getWaysList(), this);
			} else if (group.getRelationsCount() > 0) {
				// relations are supposed to occur after ways in an osm file therefore stop it here
				throw new EOFException("don't want to read all the relations");
			}
		}
	}

	@Override
	public void handle(OsmBounds osmBounds) {

	}

	@Override
	public void handle(OsmNode osmNode) {

	}

	@Override
	public void handle(OsmWay osmWay) {
		counter++;
		if (isStreetOfInterest(osmWay, linkProperties)) {
			ways.add(osmWay);
			for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
				nodes.merge(osmWay.getNodeId(i), 1, Integer::sum);
			}
		}
		if (counter % 100000 == 0) {
			log.info("Read: " + counter / 1000 + "K ways");
		}
	}

	@Override
	public void handle(OsmRelation osmRelation) {

	}

	@Override
	public void complete() {

	}
}
