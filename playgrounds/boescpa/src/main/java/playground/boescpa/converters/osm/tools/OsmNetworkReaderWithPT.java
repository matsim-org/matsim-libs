/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.scnadine.converters.osmCore.*;

import java.util.*;

/**
 * org/matsim/core/utils/io/OsmNetworkReader.java extended with the functionality
 * to recognize and tag public transport.
 *
 * Streaming with org.xml.sax.InputSource had to be removed because of privacy issues.
 *
 * @author boescpa
 */
public class OsmNetworkReaderWithPT {

	private final static Logger log = Logger.getLogger(OsmNetworkReaderWithPT.class);

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_RAILWAY = "railway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";

	private final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	private final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();
	private final Map<Long, OsmRelation> relations = new HashMap<Long, OsmRelation>();

	private final Set<Long> wayIds = new HashSet<Long>();
	private final Set<Long> nodeIds = new HashSet<Long>();
	private final Set<Long> stopNodeIds = new HashSet<Long>();

	private final Set<String> unknownHighways = new HashSet<String>();
	private final Set<String> unknownRailways = new HashSet<String>();
	private final Set<String> unknownMaxspeedTags = new HashSet<String>();
	private final Set<String> unknownLanesTags = new HashSet<String>();
	private long id = 0;
	private final Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final Map<String, OsmHighwayDefaults> railwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final TagFilter ptFilter = new TagFilter();
	private final Network network;
	private final CoordinateTransformation transform;
	private boolean keepPaths = false;
	private boolean scaleMaxSpeed = false;

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 */
	public OsmNetworkReaderWithPT(final Network network, final CoordinateTransformation transformation) {
		this(network, transformation, true);
	}

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 * @param useHighwayDefaults Highway defaults are set to standard values, if true.
	 */
	public OsmNetworkReaderWithPT(final Network network, final CoordinateTransformation transformation, final boolean useHighwayDefaults) {
		this.network = network;
		this.transform = transformation;

		if (useHighwayDefaults) {
			log.info("Falling back to default values.");
			this.setHighwayDefaults("motorway",      2, 120.0/3.6, 1.0, 2000, true);
			this.setHighwayDefaults("motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			this.setHighwayDefaults("trunk",         1,  80.0/3.6, 1.0, 2000);
			this.setHighwayDefaults("trunk_link",    1,  50.0/3.6, 1.0, 1500);
			this.setHighwayDefaults("primary",       1,  80.0/3.6, 1.0, 1500);
			this.setHighwayDefaults("primary_link",  1,  60.0/3.6, 1.0, 1500);
			this.setHighwayDefaults("secondary",     1,  60.0/3.6, 1.0, 1000);
			this.setHighwayDefaults("tertiary",      1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults("minor",         1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults("unclassified",  1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults("residential",   1,  30.0/3.6, 1.0,  600);
			this.setHighwayDefaults("living_street", 1,  15.0/3.6, 1.0,  300);
		}

		this.ptFilter.add("route", "train");
		this.ptFilter.add("route", "rail");
		this.ptFilter.add("route", "railway");
		this.ptFilter.add("route", "light_rail");
		this.ptFilter.add("route", "bus");
		this.ptFilter.add("route", "trolleybus");
		this.ptFilter.add("route", "tram");
		this.ptFilter.add("route", "ship");
		this.ptFilter.add("route", "ferry");
		this.ptFilter.add("route", "cable_car");
		this.ptFilter.add("route", "funicular");
		this.ptFilter.add("route", "funiculair");
		this.ptFilter.add("route", "subway");
	}

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links, assuming it is no oneway road.
	 *
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 *
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway">http://wiki.openstreetmap.org/wiki/Map_Features#Highway</a>
	 */
	public void setHighwayDefaults(final String highwayType, final double lanes, final double freespeed, final double freespeedFactor, final double laneCapacity_vehPerHour) {
		setHighwayDefaults(highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
	}

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links.
	 *
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 * @param oneway <code>true</code> to say that this road is a oneway road
	 */
	public void setHighwayDefaults(final String highwayType, final double lanes, final double freespeed,
								   final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway) {
		this.highwayDefaults.put(highwayType, new OsmHighwayDefaults(lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
	}

	public void setRailwayDefaults(final String railwayType, final double lanes, final double freespeed,
								   final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway) {
		this.railwayDefaults.put(railwayType, new OsmHighwayDefaults(lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
	}

	/**
	 * Sets whether the detailed geometry of the roads should be retained in the conversion or not.
	 * Keeping the detailed paths results in a much higher number of nodes and links in the resulting MATSim network.
	 * Not keeping the detailed paths removes all nodes where only one road passes through, thus only real intersections
	 * or branchings are kept as nodes. This reduces the number of nodes and links in the network, but can in some rare
	 * cases generate extremely long links (e.g. for motorways with only a few ramps every few kilometers).
	 *
	 * Defaults to <code>false</code>.
	 *
	 * @param keepPaths <code>true</code> to keep all details of the OSM roads
	 */
	public void setKeepPaths(final boolean keepPaths) {
		this.keepPaths = keepPaths;
	}

	/**
	 * In case the speed limit allowed does not represent the speed a vehicle can actually realize, e.g. by constrains of
	 * traffic lights not explicitly modeled, a kind of "average simulated speed" can be used.
	 *
	 * Defaults to <code>false</code>.
	 *
	 * @param scaleMaxSpeed <code>true</code> to scale the speed limit down by the value specified by the
	 * {@link #setHighwayDefaults(String, double, double, double, double) defaults}.
	 */
	public void setScaleMaxSpeed(final boolean scaleMaxSpeed) {
		this.scaleMaxSpeed = scaleMaxSpeed;
	}

	/**
	 * Parses the given osm file and creates a MATSim network from the data.
	 *
	 * @param osmFilename
	 * @throws UncheckedIOException
	 */
	public void parse(final String osmFilename) throws UncheckedIOException {

		OsmParser parser = new OsmParser();
		parser.addHandler(new OsmXmlParser(this.nodes, this.ways, this.relations,
				this.transform, this.wayIds, this.nodeIds, this.stopNodeIds));
		parser.readFile(osmFilename);

		convert();

		log.info("= conversion statistics: ==========================");
		log.info("MATSim: # nodes created: " + this.network.getNodes().size());
		log.info("MATSim: # links created: " + this.network.getLinks().size());

		if (this.unknownHighways.size() > 0) {
			log.info("The following highway-types had no defaults set and were thus NOT converted:");
			for (String highwayType : this.unknownHighways) {
				log.info("- \"" + highwayType + "\"");
			}
		}
		if (this.unknownRailways.size() > 0) {
			log.info("The following railway-types had no defaults set and were thus NOT converted:");
			for (String railwayType : this.unknownRailways) {
				log.info("- \"" + railwayType + "\"");
			}
		}
		log.info("= end of conversion statistics ====================");
	}

	private void convert() {
		if (this.network instanceof NetworkImpl) {
			((NetworkImpl) this.network).setCapacityPeriod(3600);
		}

		// check which ways are used
		for (OsmWay way : this.ways.values()) {
			// check which ways are used in pt-lines (ways for street and rail network had the flag already set earlier)
			if (this.wayIds.contains(way.id)) {
				way.used = true;
			}
			// check we have no ways with zero nodes
			if (way.nodes.isEmpty()) {
				way.used = false;
			}
		}

		// check which nodes are used
		for (OsmWay way : this.ways.values()) {
			if (way.used) {
				// first and last are counted twice, so they are kept in all cases
				this.nodes.get(way.nodes.get(0)).ways++;
				this.nodes.get(way.nodes.get(way.nodes.size() - 1)).ways++;

				for (Long nodeId : way.nodes) {
					OsmNode node = this.nodes.get(nodeId);
					node.used = true;
					node.ways++;
				}
			}
		}

		// TODO-boescpa Reactivate as soon as all the rest works properly...
		/*if (!this.keepPaths) {
			// marked nodes as unused where only one way leads through
			for (OsmNode node : this.nodes.values()) {
				if (node.ways == 1) {
					node.used = false;
				}
			}
			// verify we did not mark nodes as unused that build a loop
			for (OsmWay way : this.ways.values()) {
				String highway = way.tags.get(TAG_HIGHWAY);
				String railway = way.tags.get(TAG_RAILWAY);
				if (((highway != null) && (this.highwayDefaults.containsKey(highway))) || railway != null) {
					int prevRealNodeIndex = 0;
					OsmNode prevRealNode = this.nodes.get(way.nodes.get(prevRealNodeIndex));

					for (int i = 1; i < way.nodes.size(); i++) {
						OsmNode node = this.nodes.get(way.nodes.get(i));
						if (node.used) {
							if (prevRealNode == node) {
							*//* We detected a loop between to "real" nodes.
							 * Set some nodes between the start/end-loop-node to "used" again.
							 * But don't set all of them to "used", as we still want to do some network-thinning.
							 * I decided to use sqrt(.)-many nodes in between...
							 *//*
								double increment = Math.sqrt(i - prevRealNodeIndex);
								double nextNodeToKeep = prevRealNodeIndex + increment;
								for (double j = nextNodeToKeep; j < i; j += increment) {
									int index = (int) Math.floor(j);
									OsmNode intermediaryNode = this.nodes.get(way.nodes.get(index));
									intermediaryNode.used = true;
								}
							}
							prevRealNodeIndex = i;
							prevRealNode = node;
						}
					}
				}
			}
		}*/

		// create the required nodes
		for (OsmNode node : this.nodes.values()) {
			if (node.used) { // boescpa0_07-07-2014 had here "|| nodeIds.contains(node.id)" and in the block "node.used = true;" to include all possible pt-nodes. Delete if no problem...
				Node nn = this.network.getFactory().createNode(new IdImpl(node.id), node.coord);
				this.network.addNode(nn);
			}
		}

		// create the links
		// TODO-boescpa Make sure the links are properly tagged!!
		this.id = 1;
		for (OsmWay way : this.ways.values()) {
			if (way.used) {
				OsmNode fromNode = this.nodes.get(way.nodes.get(0));
				double length = 0.0;
				OsmNode lastToNode = fromNode;
				if (fromNode.used) {
					for (int i = 1, n = way.nodes.size(); i < n; i++) {
						OsmNode toNode = this.nodes.get(way.nodes.get(i));
						if (toNode != lastToNode) {
							length += CoordUtils.calcDistance(lastToNode.coord, toNode.coord);
							if (toNode.used) {
								String highway = way.tags.get(TAG_HIGHWAY);
								if (highway != null) {
									createLink(this.network, way, fromNode, toNode, length);
								} else {
									createLink(this.network, way, fromNode, toNode, length);
								}

								fromNode = toNode;
								length = 0.0;
							}
							lastToNode = toNode;
						}
					}
				}
			}
		}

		// free up memory
		this.nodes.clear();
		this.ways.clear();
		this.relations.clear();
	}

	private void createLink(final Network network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length) {
		double nofLanes;
		double laneCapacity;
		double freespeed;
		double freespeedFactor;
		boolean oneway;
		boolean onewayReverse = false;

		// load defaults
		String highway = way.tags.get(TAG_HIGHWAY);
		String railway = way.tags.get(TAG_RAILWAY);
		OsmHighwayDefaults defaults = null;
		if (highway != null) {
			defaults = this.highwayDefaults.get(highway);
			if (defaults == null) {
				this.unknownHighways.add(highway);
				return;
			}
		}
		if (railway != null) {
			defaults = this.railwayDefaults.get(railway);
			if (defaults == null) {
				this.unknownRailways.add(railway);
				return;
			}
		}
		if (defaults == null) { // this means this link entered by a pt-line
			defaults = new OsmHighwayDefaults(1.0, 1.0, 1.0, 1.0, true);
		}
		nofLanes = defaults.lanes;
		laneCapacity = defaults.laneCapacity;
		freespeed = defaults.freespeed;
		freespeedFactor = defaults.freespeedFactor;
		oneway = defaults.oneway;

		// check if there are tags that overwrite defaults
		// - check tag "junction"
		if ("roundabout".equals(way.tags.get(TAG_JUNCTION))) {
			// if "junction" is not set in tags, get() returns null and equals() evaluates to false
			oneway = true;
		}
		// - check tag "oneway"
		String onewayTag = way.tags.get(TAG_ONEWAY);
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
		// - check tag "oneway" with trunks, primary and secondary roads
		// 		(if they are marked as such, the default number of lanes should be two instead of one)
		if (highway != null) {
			if (highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary")) {
				if (oneway && nofLanes == 1.0) {
					nofLanes = 2.0;
				}
			}
		}
		// - ckeck tag "maxspeed"
		String maxspeedTag = way.tags.get(TAG_MAXSPEED);
		if (maxspeedTag != null) {
			try {
				freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert km/h to m/s
			} catch (NumberFormatException e) {
				if (!this.unknownMaxspeedTags.contains(maxspeedTag)) {
					this.unknownMaxspeedTags.add(maxspeedTag);
					log.warn("Could not parse maxspeed tag:" + e.getMessage() + ". Ignoring it.");
				}
			}
		}
		// - check tag "lanes"
		String lanesTag = way.tags.get(TAG_LANES);
		if (lanesTag != null) {
			try {
				double tmp = Double.parseDouble(lanesTag);
				if (tmp > 0) {
					nofLanes = tmp;
				}
			} catch (Exception e) {
				if (!this.unknownLanesTags.contains(lanesTag)) {
					this.unknownLanesTags.add(lanesTag);
					log.warn("Could not parse lanes tag:" + e.getMessage() + ". Ignoring it.");
				}
			}
		}

		// define the links' capacity and freespeed
		double capacity = nofLanes * laneCapacity;
		if (this.scaleMaxSpeed) {
			freespeed = freespeed * freespeedFactor;
		}

		// define modes allowed on link(s)
		//	basic type:
		Set<String> modes = new HashSet<String>();
		if (highway != null) {modes.add("street");}
		if (railway != null) {modes.add("rail");}
		if (modes.isEmpty()) {modes.add("unknownStreetType");}
		//	public transport:
		for (OsmRelation relation : this.relations.values()) {
			for (OsmParser.OsmRelationMember member : relation.members) {
				if ((member.type == OsmParser.OsmRelationMemberType.WAY) && (member.refId == way.id)) {
					modes.add(relation.tags.get("name"));
					break;
				}
			}
		}

		// only create link, if both nodes were found, node could be null, since nodes outside a layer were dropped
		Id fromId = new IdImpl(fromNode.id);
		Id toId = new IdImpl(toNode.id);
		if(network.getNodes().get(fromId) != null && network.getNodes().get(toId) != null){
			String origId = Long.toString(way.id);

			if (!onewayReverse) {
				Link l = network.getFactory().createLink(new IdImpl(this.id), network.getNodes().get(fromId), network.getNodes().get(toId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(modes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				this.id++;
			}
			if (!oneway) {
				Link l = network.getFactory().createLink(new IdImpl(this.id), network.getNodes().get(toId), network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(modes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				this.id++;
			}

		}
	}

	private static class OsmNode {
		public final long id;
		public final Coord coord;
		public final Map<String, String> tags;

		public boolean used = false;
		public int ways = 0;

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
			tags = new HashMap<String, String>(5, 0.9f);
		}
		public OsmNode(OsmParser.OsmNode node) {
			this.id = node.id;
			this.coord = node.coord;
			this.tags = node.tags;
		}
	}

	private static class OsmWay {
		public final long id;
		public final List<Long> nodes;
		public final Map<String, String> tags;

		public boolean used = false;

		public OsmWay(final long id) {
			this.id = id;
			nodes = new ArrayList<Long>(6);
			tags = new HashMap<String, String>(5, 0.9f);
		}
		public OsmWay(OsmParser.OsmWay way) {
			this.id = way.id;
			this.nodes = way.nodes;
			this.tags = way.tags;
		}
	}

	private static class OsmRelation {
		public final long id;
		public final List<OsmParser.OsmRelationMember> members;
		public final Map<String, String> tags;

		public OsmRelation(long id) {
			this.id = id;
			members = new ArrayList<OsmParser.OsmRelationMember>(8);
			tags = new HashMap<String, String>(5, 0.9f);
		}
		public OsmRelation(OsmParser.OsmRelation relation) {
			this.id = relation.id;
			this.members = relation.members;
			this.tags = relation.tags;
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

	private class OsmXmlParser implements OsmNodeHandler, OsmWayHandler, OsmRelationHandler {

		private final Map<Long, OsmNode> nodes;
		private final Map<Long, OsmWay> ways;
		private final Map<Long, OsmRelation> relations;

		private final Set<Long> wayIds;
		private final Set<Long> nodeIds;
		private final Set<Long> stopNodeIds;

		private final CoordinateTransformation transform;

		final Set<String> unhandledRouteTypes = new HashSet<String>();

		public OsmXmlParser(final Map<Long, OsmNode> nodes, final Map<Long, OsmWay> ways,
							final Map<Long, OsmRelation> relations, final CoordinateTransformation transform,
							final Set<Long> wayIds, final Set<Long> nodeIds, final Set<Long> stopNodeIds) {
			this.nodes = nodes;
			this.ways = ways;
			this.relations = relations;
			this.transform = transform;
			this.wayIds = wayIds;
			this.nodeIds = nodeIds;
			this.stopNodeIds = stopNodeIds;
		}

		@Override
		public void handleRelation(OsmParser.OsmRelation relation) {
			OsmRelation currentRelation = new OsmRelation(relation);
			if (OsmNetworkReaderWithPT.this.ptFilter.matches(currentRelation.tags)) {
				this.relations.put(currentRelation.id, currentRelation);
				for (OsmParser.OsmRelationMember member : currentRelation.members) {
					if (member.type == OsmParser.OsmRelationMemberType.WAY) {
						this.wayIds.add(member.refId);
					} else if (member.type == OsmParser.OsmRelationMemberType.NODE) {
						this.nodeIds.add(member.refId);
						if (member.role.contains("stop")) {
							this.stopNodeIds.add(member.refId);
						}
					} else if (member.type == OsmParser.OsmRelationMemberType.RELATION) {
						// TODO: deal with relation members that are relations
					}
				}
			} else {
				if (currentRelation.tags.containsKey("route")) {
					if (this.unhandledRouteTypes.add(currentRelation.tags.get("route"))) {
						log.info("route-type " + currentRelation.tags.get("route") + " not handled.");
					}
				}
			}
		}

		@Override
		public void handleNode(OsmParser.OsmNode node) {
			this.nodes.put(node.id, new OsmNode(node.id, this.transform.transform(node.coord)));
		}

		@Override
		public void handleWay(OsmParser.OsmWay way) {
			OsmWay currentWay = new OsmWay(way);
			if (!currentWay.nodes.isEmpty()) {
				OsmHighwayDefaults osmHighwayDefaults = OsmNetworkReaderWithPT.this.highwayDefaults.get(currentWay.tags.get(TAG_HIGHWAY));
				OsmHighwayDefaults osmRailwayDefaults = OsmNetworkReaderWithPT.this.railwayDefaults.get(currentWay.tags.get(TAG_RAILWAY));
				if (osmHighwayDefaults != null || osmRailwayDefaults != null) {
					currentWay.used = true;
				}
				this.ways.put(currentWay.id, currentWay);
			}
		}
	}

}

