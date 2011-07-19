package playground.mzilske.osm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

public class NetworkSink implements SinkSource {

	private static Logger log = Logger.getLogger(NetworkSink.class);
	private final Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final Network network;
	private final CoordinateTransformation transform;
	private boolean scaleMaxSpeed = false;
	private Sink sink;



	public NetworkSink(Network network2, CoordinateTransformation transform) {
		super();
		this.network = network2;
		this.transform = transform;
	}

	public void setHighwayDefaults(final int hierarchy, final String highwayType, final double lanes, final double freespeed,
			final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway) {
		this.highwayDefaults.put(highwayType, new OsmHighwayDefaults(lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
	}

	private void readLink(Way entry) {
		List<WayNode> wayNodes = entry.getWayNodes();
		if (wayNodes.size() < 2) {

		} else {
			WayNode fromNode = wayNodes.get(0);
			for (int i = 1, n = wayNodes.size(); i < n; i++) {
				WayNode toNode = wayNodes.get(i);
				org.matsim.api.core.v01.network.Node fromMatsimNode = network.getNodes().get(new IdImpl(fromNode.getNodeId()));
				if (fromMatsimNode == null) {
					throw new RuntimeException("Missing node: "+fromNode.getNodeId());
				}
				org.matsim.api.core.v01.network.Node toMatsimNode = network.getNodes().get(new IdImpl(toNode.getNodeId()));
				if (toMatsimNode == null) {
					throw new RuntimeException("Missing node: "+toNode.getNodeId());
				}
				double length = CoordUtils.calcDistance(fromMatsimNode.getCoord(), toMatsimNode.getCoord());
				createLink((NetworkImpl) this.network, entry, fromNode, toNode, length);
				fromNode = toNode;
			}
		}
	}


	private void readNode(Node osmNode) {
		CoordImpl osmCoord = new CoordImpl(osmNode.getLongitude(), osmNode.getLatitude());
		Coord transformedCoord = transform.transform(osmCoord);
		org.matsim.api.core.v01.network.Node node = network.getFactory().createNode(new IdImpl(osmNode.getId()), transformedCoord);
		network.addNode(node);
		osmNode.getTags().add(new Tag("matsim:node-id", node.getId().toString()));
	}


	@Override
	public void release() {
		sink.release();
	}

	private void createLink(final NetworkImpl network, final Way way, final WayNode fromNode, final WayNode toNode, final double length) {
		TagCollectionImpl tagCollection = new TagCollectionImpl(way.getTags());
		Map<String, String> tags = tagCollection.buildMap();
		String highway = tags.get("highway");

		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		if (defaults == null) {
			defaults = this.highwayDefaults.get("tertiary");
		}
		String origId = Long.toString(way.getId());
		double nofLanes = defaults.lanes;
		double laneCapacity = defaults.laneCapacity;
		double freespeed = defaults.freespeed;
		double freespeedFactor = defaults.freespeedFactor;
		boolean oneway = defaults.oneway;
		boolean onewayReverse = false;

		if (tags.containsKey("oneway")) {
			String onewayTag = tags.get("oneway");
			if ("yes".equals(onewayTag)) {
				oneway = true;
			} 
		}

		if (tags.containsKey("maxspeed")) {
			try {
				double maxspeed = Double.parseDouble(tags.get("maxspeed"));
				if (maxspeed < freespeed) {
					// freespeed doesn't always mean it's the maximum speed allowed.
					// thus only correct freespeed if maxspeed is lower than freespeed.
					freespeed = maxspeed;
				}
			} catch (NumberFormatException e) {
				log.warn("Could not parse freespeed tag:" + e.getMessage() + ". Ignoring it.");
			}
		}

		// check tag "lanes"
		if (tags.containsKey("lanes")) {
			try {
				if(Double.parseDouble(tags.get("lanes")) > 0){
					nofLanes = Double.parseDouble(tags.get("lanes"));
				}
			} catch (Exception e) {
				log.warn("Could not parse lanes tag:" + e.getMessage() + ". Ignoring it.");
			}
		}

		// create the link(s)
		double capacity = nofLanes * laneCapacity;

		if (this.scaleMaxSpeed) {
			freespeed = freespeed * freespeedFactor;
		}

		// only create link, if both nodes were found, node could be null, since nodes outside a layer were dropped
		long fromNodeNumber = fromNode.getNodeId();
		long toNodeNumber = toNode.getNodeId();
		if (!onewayReverse) {
			IdImpl id = new IdImpl(Long.toString(way.getId())+"_"+Long.toString(fromNodeNumber)+"_"+Long.toString(toNodeNumber));
			if (!network.getLinks().containsKey(id)) {
				Link l = network.createAndAddLink(id, network.getNodes().get(new IdImpl(fromNodeNumber)), network.getNodes().get(new IdImpl(toNodeNumber)), length, freespeed, capacity, nofLanes);
				((LinkImpl) l).setOrigId(origId);
				tagWayForward(way, l);
			} else {
				log.warn("Duplicate link: " + id);
			}
		}
		if (!oneway) {
			IdImpl id = new IdImpl(Long.toString(way.getId())+"_"+Long.toString(fromNodeNumber)+"_"+Long.toString(toNodeNumber)+"_R");
			if (!network.getLinks().containsKey(id)) {
				Link l = network.createAndAddLink(id, network.getNodes().get(new IdImpl(toNodeNumber)), network.getNodes().get(new IdImpl(fromNodeNumber)), length, freespeed, capacity, nofLanes);
				((LinkImpl) l).setOrigId(origId);
				tagWayBackward(way, l);
			} else {
				log.warn("Duplicate link: " + id);
			}
		}
	}

	private void tagWayBackward(Way way, Link l) {
		way.getTags().add(new Tag("matsim:backward:link-id:" + l.getId().toString(), l.getId().toString()));
	}

	private void tagWayForward(Way way, Link l) {
		way.getTags().add(new Tag("matsim:forward:link-id:" + l.getId().toString(), l.getId().toString()));
	}

	private static class OsmHighwayDefaults {
		public final double lanes;
		public final double freespeed;
		public final double freespeedFactor;
		public final double laneCapacity;
		public final boolean oneway;

		public OsmHighwayDefaults(final double lanes, final double freespeed, final double freespeedFactor, final double laneCapacity, final boolean oneway) {
			this.lanes = lanes;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
	}

	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(new EntityProcessor() {

			@Override
			public void process(BoundContainer bound) {
				sink.process(bound);
			}

			@Override
			public void process(NodeContainer node) {
				readNode(node.getEntity());
				sink.process(node);
			}

			@Override
			public void process(RelationContainer relation) {
				sink.process(relation);
			}

			@Override
			public void process(WayContainer way) {
				readLink(way.getEntity());
				sink.process(way);
			}

		});
	}

	@Override
	public void complete() {
		System.out.println(network.getNodes().size());
		System.out.println(network.getLinks().size());
		sink.complete();
	}

	@Override
	public void setSink(Sink sink) {
		this.sink = sink;
	}

}
