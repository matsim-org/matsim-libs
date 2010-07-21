package playground.mzilske.osm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
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
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class NetworkWriterSink implements Sink {

	private static Logger log = Logger.getLogger(NetworkWriterSink.class);
	private Scenario scenario;
	private final Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final Network network;
	private final CoordinateTransformation transform;
	private boolean scaleMaxSpeed = false;



	public NetworkWriterSink(CoordinateTransformation transform) {
		super();
		this.scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		this.transform = transform;
		this.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		this.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		this.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000, false);
		this.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500, false);
		this.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500, false);
		this.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500, false);
		this.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000, false);
		this.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600, false);
		this.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600, false);
		this.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600, false);
		this.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600, false);
		this.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300, false);
	}

	public void setHighwayDefaults(final int hierarchy, final String highwayType, final double lanes, final double freespeed,
			final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway) {
		this.highwayDefaults.put(highwayType, new OsmHighwayDefaults(lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
	}
	
	public Network getNetwork() {
		return network;
	}

	private void readLink(Way entry) {
		List<WayNode> wayNodes = entry.getWayNodes();
		if (wayNodes.size() < 2) {

		} else {
			WayNode fromNode = wayNodes.get(0);
			for (int i = 1, n = wayNodes.size(); i < n; i++) {
				WayNode toNode = wayNodes.get(i);
				double length = CoordUtils.calcDistance(scenario.getNetwork().getNodes().get(new IdImpl(fromNode.getNodeId())).getCoord(), scenario.getNetwork().getNodes().get(new IdImpl(toNode.getNodeId())).getCoord());
				createLink((NetworkLayer) this.network, entry, fromNode, toNode, length);
				fromNode = toNode;
			}
		}
	}


	private void readNode(Node osmNode) {
		CoordImpl osmCoord = new CoordImpl(osmNode.getLongitude(), osmNode.getLatitude());
		Coord transformedCoord = transform.transform(osmCoord);
		network.addNode(network.getFactory().createNode(new IdImpl(osmNode.getId()), transformedCoord));
	}


	@Override
	public void release() {
		// TODO Auto-generated method stub
	}

	private void createLink(final NetworkLayer network, final Way way, final WayNode fromNode, final WayNode toNode, final double length) {
		TagCollectionImpl tagCollection = new TagCollectionImpl(way.getTags());
		Map<String, String> tags = tagCollection.buildMap();
		String highway = tags.get("highway");

		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		if (defaults != null) {

			String origId = Long.toString(way.getId());
			double nofLanes = defaults.lanes;
			double laneCapacity = defaults.laneCapacity;
			double freespeed = defaults.freespeed;
			double freespeedFactor = defaults.freespeedFactor;
			boolean oneway = defaults.oneway;
			boolean onewayReverse = false;

			// check if there are tags that overwrite defaults
			// - check tag "junction"
			if ("roundabout".equals(tags.get("junction"))) {
				// if "junction" is not set in tags, get() returns null and equals() evaluates to false
				oneway = true;
			}

			// check tag "oneway"
			if (tags.containsKey("oneway")) {
				String onewayTag = tags.get("oneway");
				if ("yes".equals(onewayTag)) {
					oneway = true;
				} else if ("true".equals(onewayTag)) {
					oneway = true;
				} else if ("1".equals(onewayTag)) {
					oneway = true;
				} else if ("-1".equals(onewayTag)) {
					onewayReverse = true;
					oneway = false;
				} else if ("no".equals(onewayTag)) {
					oneway = false; // may be used to overwrite defaults
				}
			}

			// In case trunks, primary and secondary roads are marked as oneway,
			// the default number of lanes should be two instead of one.
			if(highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary")){
				if(oneway && nofLanes == 1.0){
					nofLanes = 2.0;
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
				Link l = network.createAndAddLink(new IdImpl(Long.toString(way.getId())+"_"+Long.toString(fromNodeNumber)+"_"+Long.toString(toNodeNumber)), network.getNodes().get(new IdImpl(fromNodeNumber)), network.getNodes().get(new IdImpl(toNodeNumber)), length, freespeed, capacity, nofLanes);
				((LinkImpl) l).setOrigId(origId);
			}
			if (!oneway) {
				Link l = network.createAndAddLink(new IdImpl(Long.toString(way.getId())+"_"+Long.toString(fromNodeNumber)+"_"+Long.toString(toNodeNumber)+"_R"), network.getNodes().get(new IdImpl(toNodeNumber)), network.getNodes().get(new IdImpl(fromNodeNumber)), length, freespeed, capacity, nofLanes);
				((LinkImpl) l).setOrigId(origId);
			}
		}
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

			}

			@Override
			public void process(NodeContainer node) {
				readNode(node.getEntity());
			}

			@Override
			public void process(RelationContainer relation) {

			}

			@Override
			public void process(WayContainer way) {
				readLink(way.getEntity());
			}

		});
	}

	@Override
	public void complete() {
		System.out.println(network.getNodes().size());
		System.out.println(network.getLinks().size());
	}

}
