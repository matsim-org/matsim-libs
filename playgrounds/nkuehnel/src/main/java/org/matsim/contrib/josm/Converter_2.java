package org.matsim.contrib.josm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.josm.OsmConvertDefaults.OsmHighwayDefaults;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class Converter_2 {
	private final static Logger log = Logger.getLogger(Converter.class);
	
	private Map<Way, List<Link>> way2Links = new HashMap<Way, List<Link>>();
	private Map<Link, WaySegment> link2Segment = new HashMap<Link, WaySegment>();
	private boolean scaleMaxSpeed = false;
	
	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";
	
	private final Set<String> unknownMaxspeedTags = new HashSet<String>();
	private final Set<String> unknownLanesTags = new HashSet<String>();
	
	private final Set<String> unknownHighways = new HashSet<String>();
	
	Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	Network network;
	private OsmDataLayer layer;

	private int id=0;
	
	public Converter_2(OsmDataLayer layer, Network network) {
		highwayDefaults = OsmConvertDefaults.getDefaults();
		this.layer = layer;
		this.network = network;
	}
	
	public void convert() {
		Map<Way, List<WaySegment>> splitWays = new HashMap<Way, List<WaySegment>>();
		List<Node> nodes = new ArrayList<Node>();
		
		for(Way way: layer.data.getWays()) {
			if (way.hasKey("highway")) {
				if (highwayDefaults.containsKey(way.getKeys().get("highway"))) {
					List<WaySegment> segments = new ArrayList<WaySegment>();
					for(Node node: way.getNodes()) {
						if(!nodes.contains(node)) {
							nodes.add(node);
						}
					}
					for(int i=0; i<way.getNodesCount()-1; i++) {
						segments.add(new WaySegment(way, i));
					}
					System.out.println("Segments: "+segments.size());
					splitWays.put(way, segments);
				}
			}
		}
		
		for (Node node : nodes) {
			double lat = node.getCoor().lat();
			double lon = node.getCoor().lon();
			org.matsim.api.core.v01.network.Node nn = this.network.getFactory().createNode(
						new IdImpl(node.getUniqueId()), new CoordImpl(lon, lat));
			this.network.addNode(nn);
		}
		
		for (Entry<Way, List<WaySegment>> entry: splitWays.entrySet()) {
			for(WaySegment segment: entry.getValue()) {
				Coord first = new CoordImpl(segment.getFirstNode().getCoor().lon(), segment.getFirstNode().getCoor().lat());
				Coord second = new CoordImpl(segment.getSecondNode().getCoor().lon(), segment.getSecondNode().getCoor().lat());
				Double length = OsmConvertDefaults.calculateWGS84Length(
						first, second);
				createLink(this.network, segment,
						segment.getFirstNode(), segment.getSecondNode(), length);
			}
			this.id++;
		}
		Layer2Network converted = new Layer2Network(layer, network, way2Links, link2Segment);
	}
	
	
	
	private void createLink(final Network network, final WaySegment segment,
			final Node fromNode, final Node toNode, final double length) {
		String highway = segment.way.getKeys().get(TAG_HIGHWAY);

		// load defaults
		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		if (defaults == null) {
			this.unknownHighways.add(highway);
			return;
		}

		double nofLanes = defaults.lanes;
		double laneCapacity = defaults.laneCapacity;
		double freespeed = defaults.freespeed;
		double freespeedFactor = defaults.freespeedFactor;
		boolean oneway = defaults.oneway;
		boolean onewayReverse = false;

		// check if there are tags that overwrite defaults
		// - check tag "junction"
		if ("roundabout".equals(segment.way.getKeys().get(TAG_JUNCTION))) {
			// if "junction" is not set in tags, get() returns null and equals()
			// evaluates to false
			oneway = true;
		}

		// check tag "oneway"
		String onewayTag = segment.way.getKeys().get(TAG_ONEWAY);
		if (onewayTag != null) {
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
		if (highway.equalsIgnoreCase("trunk")
				|| highway.equalsIgnoreCase("primary")
				|| highway.equalsIgnoreCase("secondary")) {
			if (oneway && nofLanes == 1.0) {
				nofLanes = 2.0;
			}
		}

		String maxspeedTag = segment.way.getKeys().get(TAG_MAXSPEED);
		if (maxspeedTag != null) {
			try {
				freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert
				// km/h to
				// m/s
			} catch (NumberFormatException e) {
				if (!this.unknownMaxspeedTags.contains(maxspeedTag)) {
					this.unknownMaxspeedTags.add(maxspeedTag);
					log.warn("Could not parse maxspeed tag:" + e.getMessage()
							+ ". Ignoring it.");
				}
			}
		}

		// check tag "lanes"
		String lanesTag = segment.way.getKeys().get(TAG_LANES);
		if (lanesTag != null) {
			try {
				double tmp = Double.parseDouble(lanesTag);
				if (tmp > 0) {
					nofLanes = tmp;
				}
			} catch (Exception e) {
				if (!this.unknownLanesTags.contains(lanesTag)) {
					this.unknownLanesTags.add(lanesTag);
					log.warn("Could not parse lanes tag:" + e.getMessage()
							+ ". Ignoring it.");
				}
			}
		}

		// create the link(s)
		double capacity = nofLanes * laneCapacity;

		if (this.scaleMaxSpeed) {
			freespeed = freespeed * freespeedFactor;
		}

		// only create link, if both nodes were found, node could be null, since
		// nodes outside a layer were dropped
		Id fromId = new IdImpl(fromNode.getUniqueId());
		Id toId = new IdImpl(toNode.getUniqueId());
		if (network.getNodes().get(fromId) != null
				&& network.getNodes().get(toId) != null) {
			String origId = Long.toString(segment.way.getUniqueId())+"_"+segment.lowerIndex;
			List<Link> links = new ArrayList<Link>();
			if (!onewayReverse) {
				Link l = network.getFactory().createLink(new IdImpl(this.id+"_"+segment.lowerIndex),
						network.getNodes().get(fromId),
						network.getNodes().get(toId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				links.add(l);
				link2Segment.put(l, segment);
				System.out.println(l.toString());
			}
			if (!oneway) {
				Link l = network.getFactory().createLink(new IdImpl(this.id+"_"+segment.lowerIndex+"_r"),
						network.getNodes().get(toId),
						network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId+"_r");
				}
				network.addLink(l);
				links.add(l);
				link2Segment.put(l, segment);
				System.out.println(l.toString());
			}
			if(way2Links.containsKey(segment.way)) {
				way2Links.get(segment.way).addAll(links);
			} else {
				way2Links.put(segment.way, links);
			}
		}
	}
}

