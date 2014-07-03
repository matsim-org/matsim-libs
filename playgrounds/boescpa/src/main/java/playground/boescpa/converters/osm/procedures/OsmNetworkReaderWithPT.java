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

package playground.boescpa.converters.osm.procedures;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;
import playground.scnadine.converters.osmCore.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
	private final static String[] ALL_TAGS = new String[] {TAG_LANES, TAG_HIGHWAY, TAG_MAXSPEED, TAG_JUNCTION, TAG_ONEWAY};

	private final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	private final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();
	private final Map<Long, OsmRelation> relations = new HashMap<Long, OsmRelation>();

	/* package */final Set<Long> wayIds = new HashSet<Long>();
	/* package */final Set<Long> nodeIds = new HashSet<Long>();
	/* package */final Set<Long> stopNodeIds = new HashSet<Long>();

	private final Set<String> unknownHighways = new HashSet<String>();
	private final Set<String> unknownMaxspeedTags = new HashSet<String>();
	private final Set<String> unknownLanesTags = new HashSet<String>();
	private long id = 0;
	/*package*/ final Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final Network network;
	private final CoordinateTransformation transform;
	private boolean keepPaths = false;
	private boolean scaleMaxSpeed = false;

	private boolean slowButLowMemory = false;

	/*package*/ final List<OsmFilter> hierarchyLayers = new ArrayList<OsmFilter>();

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
			this.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
			this.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			this.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
			this.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
			this.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);
		}
	}

	/**
	 * Parses the given osm file and creates a MATSim network from the data.
	 *
	 * @param osmFilename
	 * @throws UncheckedIOException
	 */
	public void parse(final String osmFilename) throws UncheckedIOException {
		if(this.hierarchyLayers.isEmpty()){
			log.warn("No hierarchy layer specified. Will convert every highway specified by setHighwayDefaults.");
		}

		OsmParser parser = new OsmParser();
		parser.addHandler(new OsmXmlParser(this.nodes, this.ways, this.relations,
				this.transform, this.wayIds, this.nodeIds, this.stopNodeIds));
		parser.readFile(osmFilename);
		/*OsmXmlParser parser;
		if (this.slowButLowMemory) {
			log.info("parsing osm file first time: identifying nodes used by ways");
			parser = new OsmXmlParser(this.nodes, this.ways, this.transform);
			parser.enableOptimization(1);
			parser.parse(osmFilename);
			log.info("parsing osm file second time: loading required nodes and ways");
			parser.enableOptimization(2);
			parser.parse(osmFilename);
			log.info("done loading data");
		} else {
			parser = new OsmXmlParser(this.nodes, this.ways, this.transform);
			parser.parse(osmFilename);
		}*/

		convert();

		log.info("= conversion statistics: ==========================");
		/*log.info("osm: # nodes read:       " + parser.nodeCounter.getCounter());
		log.info("osm: # ways read:        " + parser.wayCounter.getCounter());*/
		log.info("MATSim: # nodes created: " + this.network.getNodes().size());
		log.info("MATSim: # links created: " + this.network.getLinks().size());

		if (this.unknownHighways.size() > 0) {
			log.info("The following highway-types had no defaults set and were thus NOT converted:");
			for (String highwayType : this.unknownHighways) {
				log.info("- \"" + highwayType + "\"");
			}
		}
		log.info("= end of conversion statistics ====================");
	}

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links, assuming it is no oneway road.
	 *
	 * @param hierarchy The hierarchy layer the highway appears.
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 *
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway">http://wiki.openstreetmap.org/wiki/Map_Features#Highway</a>
	 */
	public void setHighwayDefaults(final int hierarchy , final String highwayType, final double lanes, final double freespeed, final double freespeedFactor, final double laneCapacity_vehPerHour) {
		setHighwayDefaults(hierarchy, highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
	}

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links.
	 *
	 * @param hierarchy The hierarchy layer the highway appears in.
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 * @param oneway <code>true</code> to say that this road is a oneway road
	 */
	public void setHighwayDefaults(final int hierarchy, final String highwayType, final double lanes, final double freespeed,
								   final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway) {
		this.highwayDefaults.put(highwayType, new OsmHighwayDefaults(hierarchy, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
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
	 * {@link #setHighwayDefaults(int, String, double, double, double, double) defaults}.
	 */
	public void setScaleMaxSpeed(final boolean scaleMaxSpeed) {
		this.scaleMaxSpeed = scaleMaxSpeed;
	}

	/**
	 * Defines a new hierarchy layer by specifying a rectangle and the hierarchy level to which highways will be converted.
	 *
	 * @param coordNWLat The latitude of the north western corner of the rectangle.
	 * @param coordNWLon The longitude of the north western corner of the rectangle.
	 * @param coordSELat The latitude of the south eastern corner of the rectangle.
	 * @param coordSELon The longitude of the south eastern corner of the rectangle.
	 * @param hierarchy Layer specifying the hierarchy of the layers starting with 1 as the top layer.
	 */
	public void setHierarchyLayer(final double coordNWLat, final double coordNWLon, final double coordSELat, final double coordSELon, final int hierarchy) {
		this.hierarchyLayers.add(new OsmFilter(this.transform.transform(new CoordImpl(coordNWLon, coordNWLat)), this.transform.transform(new CoordImpl(coordSELon, coordSELat)), hierarchy));
	}

	/**
	 * By default, this converter caches a lot of data internally to speed up the network generation.
	 * This can lead to OutOfMemoryExceptions when converting huge osm files. By enabling this
	 * memory optimization, the converter tries to reduce its memory usage, but will run slower.
	 *
	 * @param memoryEnabled
	 */
	public void setMemoryOptimization(final boolean memoryEnabled) {
		this.slowButLowMemory = memoryEnabled;
	}

	private void convert() {
		if (this.network instanceof NetworkImpl) {
			((NetworkImpl) this.network).setCapacityPeriod(3600);
		}

		Iterator<Map.Entry<Long, OsmWay>> it = this.ways.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, OsmWay> entry = it.next();
			for (Long nodeId : entry.getValue().nodes) {
				if (this.nodes.get(nodeId) == null) {
					it.remove();
					break;
				}
			}
		}

		// check which nodes are used
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get(TAG_HIGHWAY);
			String railway = way.tags.get(TAG_RAILWAY);
			if ((highway != null) && (this.highwayDefaults.containsKey(highway))) {
				// check to which level a way belongs
				way.hierarchy = this.highwayDefaults.get(highway).hierarchy;

				// first and last are counted twice, so they are kept in all cases
				this.nodes.get(way.nodes.get(0)).ways++;
				this.nodes.get(way.nodes.get(way.nodes.size()-1)).ways++;

				for (Long nodeId : way.nodes) {
					OsmNode node = this.nodes.get(nodeId);
					if (this.hierarchyLayers.isEmpty()) {
						node.used = true;
						node.ways++;
					} else {
						for (OsmFilter osmFilter : this.hierarchyLayers) {
							if(osmFilter.coordInFilter(node.coord, way.hierarchy)){
								node.used = true;
								node.ways++;
								break;
							}
						}
					}
				}
			} else if (railway != null) {
				// first and last are counted twice, so they are kept in all cases
				this.nodes.get(way.nodes.get(0)).ways++;
				this.nodes.get(way.nodes.get(way.nodes.size()-1)).ways++;

				for (Long nodeId : way.nodes) {
					OsmNode node = this.nodes.get(nodeId);
					node.used = true;
					node.ways++;
				}
			}
		}

		if (!this.keepPaths) {
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
							/* We detected a loop between to "real" nodes.
							 * Set some nodes between the start/end-loop-node to "used" again.
							 * But don't set all of them to "used", as we still want to do some network-thinning.
							 * I decided to use sqrt(.)-many nodes in between...
							 */
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
		}

		// create the required nodes
		for (OsmNode node : this.nodes.values()) {
			if (node.used || nodeIds.contains(node.id)) {
				node.used = true;
				Node nn = this.network.getFactory().createNode(new IdImpl(node.id), node.coord);
				this.network.addNode(nn);
			}
		}

		// create the links
		// TODO-boescpa Make sure the links are properly tagged!!
		this.id = 1;
		for (OsmWay way : this.ways.values()) {
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
								if (this.hierarchyLayers.isEmpty()) {
									createLink(this.network, way, fromNode, toNode, length);
								} else {
									for (OsmFilter osmFilter : this.hierarchyLayers) {
										if (osmFilter.coordInFilter(fromNode.coord, way.hierarchy)) {
											createLink(this.network, way, fromNode, toNode, length);
											break;
										}
										if (osmFilter.coordInFilter(toNode.coord, way.hierarchy)) {
											createLink(this.network, way, fromNode, toNode, length);
											break;
										}
									}
								}
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

		// free up memory
		this.nodes.clear();
		this.ways.clear();
	}

	private void createLink(final Network network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length) {
		double nofLanes;
		double laneCapacity;
		double freespeed;
		double freespeedFactor;
		boolean oneway;
		boolean onewayReverse = false;

		String highway = way.tags.get(TAG_HIGHWAY);
		String railway = way.tags.get(TAG_RAILWAY);

		if (highway != null) {
			// Create road links:

			// load defaults
			OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
			if (defaults == null) {
				this.unknownHighways.add(highway);
				return;
			}

			nofLanes = defaults.lanes;
			laneCapacity = defaults.laneCapacity;
			freespeed = defaults.freespeed;
			freespeedFactor = defaults.freespeedFactor;
			oneway = defaults.oneway;
		} else if (railway != null) {
			// Create rail links:

			nofLanes = 1.;
			laneCapacity = 1.;
			freespeed = 1.;
			freespeedFactor = 1.;
			oneway = false;
		} else {
			// Create public transport links:

			nofLanes = 1.;
			laneCapacity = 1.;
			freespeed = 1.;
			freespeedFactor = 1.;
			oneway = false;
		}

		// check if there are tags that overwrite defaults
		// - check tag "junction"
		if ("roundabout".equals(way.tags.get(TAG_JUNCTION))) {
			// if "junction" is not set in tags, get() returns null and equals() evaluates to false
			oneway = true;
		}

		// check tag "oneway"
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

		// In case trunks, primary and secondary roads are marked as oneway,
		// the default number of lanes should be two instead of one.
		if (highway != null) {
			if (highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary")) {
				if (oneway && nofLanes == 1.0) {
					nofLanes = 2.0;
				}
			}
		}

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

		// check tag "lanes"
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

		// create the link(s)
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

	private static class OsmFilter {
		private final Coord coordNW;
		private final Coord coordSE;
		private final int hierarchy;

		public OsmFilter(final Coord coordNW, final Coord coordSE, final int hierarchy) {
			this.coordNW = coordNW;
			this.coordSE = coordSE;
			this.hierarchy = hierarchy;
		}

		public boolean coordInFilter(final Coord coord, final int hierarchyLevel){
			if(this.hierarchy < hierarchyLevel){
				return false;
			}

			return ((this.coordNW.getX() < coord.getX() && coord.getX() < this.coordSE.getX()) &&
					(this.coordNW.getY() > coord.getY() && coord.getY() > this.coordSE.getY()));
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

		public int hierarchy = -1;

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

		public final int hierarchy;
		public final double lanes;
		public final double freespeed;
		public final double freespeedFactor;
		public final double laneCapacity;
		public final boolean oneway;

		public OsmHighwayDefaults(final int hierarchy, final double lanes, final double freespeed, final double freespeedFactor, final double laneCapacity, final boolean oneway) {
			this.hierarchy = hierarchy;
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
		private final TagFilter filter;

		/* package */final Set<String> unhandledRouteTypes = new HashSet<String>();

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

			this.filter = new TagFilter();
			this.filter.add("route", "train");
			this.filter.add("route", "rail");
			this.filter.add("route", "railway");
			this.filter.add("route", "light_rail");
			this.filter.add("route", "bus");
			this.filter.add("route", "trolleybus");
			this.filter.add("route", "tram");
			this.filter.add("route", "ship");
			this.filter.add("route", "ferry");
			this.filter.add("route", "cable_car");
			this.filter.add("route", "funicular");
			this.filter.add("route", "funiculair");
			this.filter.add("route", "subway");
		}

		@Override
		public void handleRelation(OsmParser.OsmRelation relation) {
			OsmRelation currentRelation = new OsmRelation(relation);
			if (this.filter.matches(currentRelation.tags)) {
				this.relations.put(currentRelation.id, currentRelation);
				for (OsmParser.OsmRelationMember member : currentRelation.members) {
					if (member.type == OsmParser.OsmRelationMemberType.WAY) {
						this.wayIds.add(member.refId);
					} else if (member.type == OsmParser.OsmRelationMemberType.NODE) {
						this.nodeIds.add(member.refId);
						if (member.role.contains("stop")) {
							this.stopNodeIds.add(member.refId);
						}
					}
					// TODO: deal with relation members that are relations
				}
			} else {
				if (currentRelation.tags.containsKey("route")) {
					if (this.unhandledRouteTypes
							.add(currentRelation.tags.get("route"))) {
						log.info("route-type " + currentRelation.tags.get("route")
								+ " not handled.");
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
				/*boolean used = false;
				OsmHighwayDefaults osmHighwayDefaults = OsmNetworkReaderWithPT.this.highwayDefaults.get(currentWay.tags.get(TAG_HIGHWAY));
				if (osmHighwayDefaults != null) {
					int hierarchy = osmHighwayDefaults.hierarchy;
					currentWay.hierarchy = hierarchy;
					if (OsmNetworkReaderWithPT.this.hierarchyLayers.isEmpty()) {
						used = true;
					} else {
						for (OsmFilter osmFilter : OsmNetworkReaderWithPT.this.hierarchyLayers) {
							for (Long nodeId : currentWay.nodes) {
								OsmNode node = this.nodes.get(nodeId);
								if (node != null && osmFilter.coordInFilter(node.coord, currentWay.hierarchy)) {
									used = true;
									break;
								}
							}
						}
					}
				}
				if (used || (currentWay.tags.get(TAG_RAILWAY) != null)) {
					this.ways.put(currentWay.id, currentWay);
				}*/
				this.ways.put(currentWay.id, currentWay);
			}
		}
	}
	/*private class OsmXmlParser extends MatsimXmlParser {

		private OsmWay currentWay = null;
		private final Map<Long, OsmNode> nodes;
		private final Map<Long, OsmWay> ways;
		*//*package*//* final Counter nodeCounter = new Counter("node ");
		*//*package*//* final Counter wayCounter = new Counter("way ");
		private final CoordinateTransformation transform;
		private boolean loadNodes = true;
		private boolean loadWays = true;
		private boolean mergeNodes = false;
		private boolean collectNodes = false;

		public OsmXmlParser(final Map<Long, OsmNode> nodes, final Map<Long, OsmWay> ways, final CoordinateTransformation transform) {
			super();
			this.nodes = nodes;
			this.ways = ways;
			this.transform = transform;
			this.setValidating(false);
		}

		public void enableOptimization(final int step) {
			this.loadNodes = false;
			this.loadWays = false;
			this.collectNodes = false;
			this.mergeNodes = false;
			if (step == 1) {
				this.collectNodes = true;
			} else if (step == 2) {
				this.mergeNodes = true;
				this.loadWays = true;
			}
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			if ("node".equals(name)) {
				if (this.loadNodes) {
					Long id = Long.valueOf(atts.getValue("id"));
					double lat = Double.parseDouble(atts.getValue("lat"));
					double lon = Double.parseDouble(atts.getValue("lon"));
					this.nodes.put(id, new OsmNode(id, this.transform.transform(new CoordImpl(lon, lat))));
					this.nodeCounter.incCounter();
				} else if (this.mergeNodes) {
					OsmNode node = this.nodes.get(Long.valueOf(atts.getValue("id")));
					if (node != null) {
						double lat = Double.parseDouble(atts.getValue("lat"));
						double lon = Double.parseDouble(atts.getValue("lon"));
						Coord c = this.transform.transform(new CoordImpl(lon, lat));
						node.coord.setXY(c.getX(), c.getY());
						this.nodeCounter.incCounter();
					}
				}
			} else if ("way".equals(name)) {
				this.currentWay = new OsmWay(Long.parseLong(atts.getValue("id")));
			} else if ("nd".equals(name)) {
				if (this.currentWay != null) {
					this.currentWay.nodes.add(Long.parseLong(atts.getValue("ref")));
				}
			} else if ("tag".equals(name)) {
				if (this.currentWay != null) {
					String key = StringCache.get(atts.getValue("k"));
					for (String tag : ALL_TAGS) {
						if (tag.equals(key)) {
							this.currentWay.tags.put(key, StringCache.get(atts.getValue("v")));
							break;
						}
					}
				}
			}
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			if ("way".equals(name)) {
				if (!this.currentWay.nodes.isEmpty()) {
					boolean used = false;
					OsmHighwayDefaults osmHighwayDefaults = OsmNetworkReaderWithPT.this.highwayDefaults.get(this.currentWay.tags.get(TAG_HIGHWAY));
					if (osmHighwayDefaults != null) {
						int hierarchy = osmHighwayDefaults.hierarchy;
						this.currentWay.hierarchy = hierarchy;
						if (OsmNetworkReaderWithPT.this.hierarchyLayers.isEmpty()) {
							used = true;
						}
						if (this.collectNodes) {
							used = true;
						} else {
							for (OsmFilter osmFilter : OsmNetworkReaderWithPT.this.hierarchyLayers) {
								for (Long nodeId : this.currentWay.nodes) {
									OsmNode node = this.nodes.get(nodeId);
									if(node != null && osmFilter.coordInFilter(node.coord, this.currentWay.hierarchy)){
										used = true;
										break;
									}
								}
								if (used) {
									break;
								}
							}
						}
					}
					if (used) {
						if (this.collectNodes) {
							for (long id : this.currentWay.nodes) {
								this.nodes.put(id, new OsmNode(id, new CoordImpl(0, 0)));
							}
						} else if (this.loadWays) {
							this.ways.put(this.currentWay.id, this.currentWay);
							this.wayCounter.incCounter();
						}
					}
				}
				this.currentWay = null;
			}
		}

	}*/

	private static class StringCache {

		private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<String, String>(10000);

		/**
		 * Returns the cached version of the given String. If the strings was
		 * not yet in the cache, it is added and returned as well.
		 *
		 * @param string
		 * @return cached version of string
		 */
		public static String get(final String string) {
			String s = cache.putIfAbsent(string, string);
			if (s == null) {
				return string;
			}
			return s;
		}
	}

}

