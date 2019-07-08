package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.access.OsmHandler;
import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.seq.PbfReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.matsim.api.core.v01.Coord;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Log
class OsmNetworkParser {

	static NodesAndWays parse(Path inputFile, Map<String, LinkProperties> linkProperties) {

		log.info("start reading ways");

		try {
			var waysReader = new PbfReader(inputFile.toFile(), false);
			var waysHandler = new WayHandler(linkProperties);
			waysReader.setHandler(waysHandler);
			waysReader.read();

			log.info("finished reading ways.");
			log.info("Kept " + waysHandler.ways.size() + "/" + waysHandler.counter + "ways");
			log.info("Marked " + waysHandler.nodes.size() + " nodes to be kept");
			log.info("starting to read nodes");

			var nodesReader = new PbfReader(inputFile.toFile(), false);
			var nodesHandler = new NodesHandler(waysHandler.nodes);
			nodesReader.setHandler(nodesHandler);
			nodesReader.read();

			log.info("finished reading nodes");

			return new NodesAndWays(nodesHandler.nodes, waysHandler.ways);
		} catch (FileNotFoundException | OsmInputException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isStreetOfInterest(OsmWay way, Map<String, LinkProperties> linkProperties) {
		for (int i = 0; i < way.getNumberOfTags(); i++) {
			String tag = way.getTag(i).getKey();
			String tagvalue = way.getTag(i).getValue();
			if (tag.equals(OsmTags.HIGHWAY) && linkProperties.containsKey(tagvalue)) return true;
			if (tag.equals(OsmTags.HIGHWAY)) log.info("unknown highway tag: " + tagvalue);
		}
		return false;
	}

	@RequiredArgsConstructor
	private static class WayHandler implements OsmHandler {

		private final Set<OsmWay> ways = new HashSet<>();
		private final Map<Long, Integer> nodes = new HashMap<>();
		private final Map<String, LinkProperties> linkProperties;

		private int counter = 0;

		private static boolean isStreet(OsmWay way) {
			for (int i = 0; i < way.getNumberOfTags(); i++) {
				String tag = way.getTag(i).getKey();
				if (tag.equals(OsmTags.HIGHWAY)) return true;
			}
			return false;
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
				log.info("Read: " + counter + " ways");
			}
		}

		@Override
		public void handle(OsmBounds osmBounds) {
		}

		@Override
		public void handle(OsmNode osmNode) {
		}

		@Override
		public void handle(OsmRelation osmRelation) {
		}

		@Override
		public void complete() {
		}
	}

	@RequiredArgsConstructor
	private static class NodesHandler implements OsmHandler {

		private final Map<Long, Integer> nodeIdsOfInterest;
		private final Map<Long, LightOsmNode> nodes = new HashMap<>();
		private int counter = 0;

		@Override
		public void handle(OsmNode osmNode) {

			counter++;

			if (nodeIdsOfInterest.containsKey(osmNode.getId())) {
				Coord coord = new Coord(osmNode.getLongitude(), osmNode.getLatitude());
				int numberOfWays = nodeIdsOfInterest.get(osmNode.getId());
				nodes.put(osmNode.getId(), new LightOsmNode(osmNode.getId(), numberOfWays, coord));
			}
			if (counter % 100000 == 0) {
				log.info("Read: " + counter + " nodes");
			}
		}

		@Override
		public void handle(OsmBounds osmBounds) {
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
}
