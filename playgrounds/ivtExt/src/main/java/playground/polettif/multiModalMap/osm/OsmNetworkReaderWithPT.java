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

package playground.polettif.multiModalMap.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.polettif.multiModalMap.osm.core.*;

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

	// todo create lookup table for maxspeed

	/*
	OSM TAGS
	*/

	// general tags
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_RAILWAY = "railway";

	// Tags road network
	private final static String TAG_LANES = "lanes";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";
	private final static String TAG_ACCESS = "access";

	// Tags public transport network
	private static final String TAG_SERVICE = "service";
	private final static String TAG_ROUTE = "route";

	private final static String TAG_PUBLIC_TRANSPORT = "public_transport";
	private final static String TAG_TYPE = "type";


	private static final String MEMBER_ROLE_STOP = "stop";
	private static final String MEMBER_ROLE_STOP_FORWARD = "stop_forward";
	private static final String MEMBER_ROLE_STOP_BACKWARD = "stop_backward";

	// values for highway=*
	private static final String V_MOTORWAY = "motorway";
	private static final String V_MOTORWAY_LINK = "motorway_link";
	private static final String V_TRUNK = "trunk";
	private static final String V_TRUNK_LINK = "trunk_link";
	private static final String V_PRIMARY = "primary";
	private static final String V_PRIMARY_LINK = "primary_link";
	private static final String V_SECONDARY = "secondary";
	private static final String V_TERTIARY = "tertiary";
	private static final String V_MINOR = "minor";
	private static final String V_UNCLASSIFIED = "unclassified";
	private static final String V_RESIDENTIAL = "residential";
	private static final String V_LIVING_STREET = "living_street";
	private static final String V_SERVICE = "service";

	private final static String V_STOP_POSITION = "stop_position";
	private final static String V_BUS = "bus";
	private final static String V_TROLLEYBUS = "trolleybus";


	// values for railway=*
	private static final String V_RAIL = "rail";
	private static final String V_TRAM = "tram";
	private static final String V_LIGHT_RAIL = "light_rail";
	private static final String V_FUNICULAR = "funicular";
	private static final String V_MONORAIL = "monorail";
	private static final String V_SUBWAY = "subway";

	/*
	Maps for nodes, ways and relations
	 */
	private final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	private final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();
	private final Map<Long, OsmRelation> relations = new HashMap<Long, OsmRelation>();

	private final Map<Long, Long> wayIds = new HashMap<Long, Long>();

	/*
	Maps for unknown entities
	 */
	private final Set<String> unknownHighways = new HashSet<>();
	private final Set<String> unknownRailways = new HashSet<>();
	private final Set<String> unknownPTs = new HashSet<>();
	private final Set<String> unknownWays = new HashSet<>();
	private final Set<String> unknownMaxspeedTags = new HashSet<>();
	private final Set<String> unknownLanesTags = new HashSet<>();
	private final CoordinateTransformation transformation; // is applied to nodes in OsmParserHandler
	private long id = 0;

	/*
	Default values
	 */
	private final Map<String, OsmWayDefaults> highwayDefaults = new HashMap<>();
	private final Map<String, OsmWayDefaults> railwayDefaults = new HashMap<>();
	private final TagFilter ptFilter = new TagFilter();

	/*
	Network and Transformation Object
	 */
	private final Network network;

	/*
	Parse Params
	 */
	private boolean keepPaths = false;
	private boolean scaleMaxSpeed = false;
	private final static double MAX_LINKLENGTH = 500.0;


	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 */
	public OsmNetworkReaderWithPT(final Network network, final CoordinateTransformation transformation) {
		this(network, transformation, true);
	}

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param useHighwayDefaults Highway defaults are set to standard values, if true.
	 */
	public OsmNetworkReaderWithPT(final Network network, final CoordinateTransformation transformation, final boolean useHighwayDefaults) {
		this.network = network;
		this.transformation = transformation;

		if (useHighwayDefaults) {
			log.info("Falling back to default values.");

			// Set highway-defaults (and with it the filter...)
			this.setHighwayDefaults(V_MOTORWAY,      2, 120.0/3.6, 1.0, 2000, true);
			this.setHighwayDefaults(V_MOTORWAY_LINK, 1,  80.0/3.6, 1.0, 1500, true);
			this.setHighwayDefaults(V_TRUNK,         1,  80.0/3.6, 1.0, 2000);
			this.setHighwayDefaults(V_TRUNK_LINK,    1,  50.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(V_PRIMARY,       1,  80.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(V_PRIMARY_LINK,  1,  60.0/3.6, 1.0, 1500);
			this.setHighwayDefaults(V_SECONDARY,     1,  60.0/3.6, 1.0, 1000);
			this.setHighwayDefaults(V_TERTIARY,      1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(V_MINOR,         1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(V_UNCLASSIFIED,  1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(V_RESIDENTIAL,   1,  30.0/3.6, 1.0,  600);
			this.setHighwayDefaults(V_LIVING_STREET, 1,  15.0/3.6, 1.0,  300);
//			this.setHighwayDefaults(V_SERVICE, 		 1,  15.0/3.6, 1.0,  200); // TODO service roads are used in zurich for bus-only roads

			// Set railway-defaults (and with it the filter...)
			this.setRailwayDefaults(V_RAIL, 		  1,  80.0/3.6, 1.0,  100);
			this.setRailwayDefaults(V_TRAM, 		  1,  80.0/3.6, 1.0,  100, true);
			this.setRailwayDefaults(V_LIGHT_RAIL, 	  1,  80.0/3.6, 1.0,  100);

			this.setRelationPTFilter(TAG_ROUTE, V_BUS);
			this.setRelationPTFilter(TAG_ROUTE, V_TROLLEYBUS);
			this.setRelationPTFilter(TAG_ROUTE, V_TRAM);
			this.setRelationPTFilter(TAG_ROUTE, V_RAIL);
			this.setRelationPTFilter(TAG_ROUTE, V_LIGHT_RAIL);


			//this.setRailwayDefaults("funicular",	  1,  40.0/3.6, 1.0,  100);
			//this.setRailwayDefaults("light_rail",	  1,  80.0/3.6, 1.0,  100);
			//this.setRailwayDefaults("subway",		  1,  80.0/3.6, 1.0,  100, true);
			// Set pt-defaults (and with it the filter...)
			//this.setPTDefaults("ferry", 	  1, 120.0/3.6, 1.0,  100);
			//this.setPTDefaults("ship",		  1,  80.0/3.6, 1.0,  100);
			//this.setPTDefaults("cable_car",	  1,  80.0/3.6, 1.0,  100);
			// Set default pt-filter:
			//this.setRelationPTFilter("train");
			//this.setRelationPTFilter("rail");
			//this.setRelationPTFilter("railway");
			//this.setRelationPTFilter("light_rail");

			//this.setRelationPTFilter("ship");
			//this.setRelationPTFilter("ferry");
			//this.setRelationPTFilter("cable_car");
			//this.setRelationPTFilter("funicular");
			//this.setRelationPTFilter("funiculair");
			//this.setRelationPTFilter("subway");
		}
	}

	public void setRelationPTFilter(final String key, final String value) {
		this.ptFilter.add(key, value);
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

	public void setRailwayDefaults(final String railwayType,
								   final double lanes,
								   final double freespeed,
								   final double freespeedFactor,
								   final double laneCapacity_vehPerHour) {
		setRailwayDefaults(railwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
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
		this.highwayDefaults.put(highwayType, new OsmWayDefaults(lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
	}
	public void setRailwayDefaults(final String railwayType,
								   final double lanes,
								   final double freespeed,
								   final double freespeedFactor,
								   final double laneCapacity_vehPerHour,
								   final boolean oneway) {
		this.railwayDefaults.put(railwayType, new OsmWayDefaults(lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
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
		parser.addHandler(new OsmParserHandler(this.nodes, this.ways, this.relations, this.wayIds, this.transformation));
		parser.readFile(osmFilename);

		this.convert();

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
		if (this.unknownPTs.size() > 0) {
			log.info("The following PT-types had no defaults set and were thus NOT converted:");
			for (String ptType : this.unknownPTs) {
				log.info("- \"" + ptType + "\"");
			}
		}
		if (this.unknownWays.size() > 0) {
			log.info("The way-types with the following tags had no defaults set and were thus NOT converted:");
			for (String wayType : this.unknownWays) {
				log.info("- \"" + wayType + "\"");
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
			// here checks may be added to set additional ways "way.used = true"
			break;
		}

		// remove unused ways
		Iterator<Map.Entry<Long, OsmWay>> it = this.ways.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, OsmWay> entry = it.next();
			if (!entry.getValue().used) {
				it.remove();
			}
		}

		// check which nodes are used
		for (OsmWay way : this.ways.values()) {
			// first and last are counted twice, so they are kept in all cases
			this.nodes.get(way.nodes.get(0)).ways++;
			this.nodes.get(way.nodes.get(way.nodes.size() - 1)).ways++;

			for (Long nodeId : way.nodes) {
				OsmNode node = this.nodes.get(nodeId);
				node.used = true;
				node.ways++;
			}
		}

		// Clean network:
		if (!this.keepPaths) {
			// marked nodes as unused where only one way leads through
			// but only if this doesn't lead to links longer than MAX_LINKLENGTH
			for (OsmWay way : this.ways.values()) {
				double length = 0.0;
				OsmNode lastNode = this.nodes.get(way.nodes.get(0));
				for (int i = 1; i < way.nodes.size(); i++) {
					OsmNode node = this.nodes.get(way.nodes.get(i));
					if (node.ways > 1) {
						length = 0.0;
						lastNode = node;
					} else if (node.ways == 1) {
						length += CoordUtils.calcEuclideanDistance(lastNode.coord, node.coord);
						if (length <= MAX_LINKLENGTH) {
							node.used = false;
							lastNode = node;
						} else {
							length = 0.0;
							lastNode = node;
						}
					} else {
						log.warn("Way node with less than 1 ways found.");
					}
				}
			}
			// verify we did not mark nodes as unused that build a loop
			for (OsmWay way : this.ways.values()) {
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

		// create the required nodes
		for (OsmNode node : this.nodes.values()) {
			if (node.used) {
				Node nn = this.network.getFactory().createNode(Id.create(node.id, Node.class), node.coord);
				this.network.addNode(nn);
			}
		}

		// create the links
		this.id = 1;
		for (OsmWay way : this.ways.values()) {
			OsmNode fromNode = this.nodes.get(way.nodes.get(0));
			double length = 0.0;
			OsmNode lastToNode = fromNode;
			if (fromNode.used) {
				for (int i = 1, n = way.nodes.size(); i < n; i++) {
					OsmNode toNode = this.nodes.get(way.nodes.get(i));
					if (toNode != lastToNode) {
						length += CoordUtils.calcEuclideanDistance(lastToNode.coord, toNode.coord);
						if (toNode.used) {
							createLink(this.network, way, fromNode, toNode, length);
							fromNode = toNode;
							length = 0.0;
						}
						lastToNode = toNode;
					}
				}
			}
		}


		// todo create MATSim Transit Schedule with available data


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
		OsmWayDefaults defaults;
		if (highway != null) {
			defaults = this.highwayDefaults.get(highway);
			if (defaults == null) {
				this.unknownHighways.add(highway);
				return;
			}
		} else if (railway != null) {
			defaults = this.railwayDefaults.get(railway);
			if (defaults == null) {
				this.unknownRailways.add(railway);
				return;
			}
		}  else {
			this.unknownWays.add(way.tags.values().toString());
			return;
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
		if (highway != null) {
			modes.add("car");
		}

		if (railway != null && railwayDefaults.containsKey(railway)) {
			modes.add(railway);
		}

		if (modes.isEmpty()) {modes.add("unknownStreetType");}

		//	public transport: get relation which this way is part of, then get the relations route=* (-> the mode)
		for (OsmRelation relation : this.relations.values()) {
			for (OsmParser.OsmRelationMember member : relation.members) {
				if ((member.type == OsmParser.OsmRelationMemberType.WAY) && (member.refId == way.id) && OsmNetworkReaderWithPT.this.ptFilter.matches(relation.tags)) {
					String mode = relation.tags.get(TAG_ROUTE);
					// mark that it is a link used by any pt:
					if (mode == null) {
						break;
					} else {
						if(mode.equals(V_TROLLEYBUS)) {
							mode = V_BUS;
						}
						modes.add(mode);
					}
					break;
				}
			}
		}

		// only create link, if both nodes were found, node could be null, since nodes outside a layer were dropped
		Id<Node> fromId = Id.create(fromNode.id, Node.class);
		Id<Node> toId = Id.create(toNode.id, Node.class);
		if(network.getNodes().get(fromId) != null && network.getNodes().get(toId) != null){
			String origId = Long.toString(way.id);

			if (!onewayReverse) {
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(fromId), network.getNodes().get(toId));
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
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(toId), network.getNodes().get(fromId));
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

	/**
	 * Containers for OSM nodes, ways and relations
	 */
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

	private static class OsmWayDefaults {

		public final double lanes;
		public final double freespeed;
		public final double freespeedFactor;
		public final double laneCapacity;
		public final boolean oneway;

		public OsmWayDefaults(final double lanes, final double freespeed, final double freespeedFactor, final double laneCapacity, final boolean oneway) {
			this.lanes = lanes;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
	}

	/**
	 * Parser/Handler for OSM data
	 */
	private class OsmParserHandler implements OsmNodeHandler, OsmWayHandler, OsmRelationHandler {

		private final Map<Long, OsmNode> nodes;
		private final Map<Long, OsmWay> ways;
		private final Map<Long, OsmRelation> relations;

		private final Map<Long, Long> wayIds;
		private final CoordinateTransformation transformation;

		public OsmParserHandler(final Map<Long, OsmNode> nodes, final Map<Long, OsmWay> ways,
							  final Map<Long, OsmRelation> relations,
							  final Map<Long, Long> wayIds, CoordinateTransformation transformation) {
			this.nodes = nodes;
			this.ways = ways;
			this.relations = relations;
			this.wayIds = wayIds;
			this.transformation = transformation;
		}

		@Override
		public void handleRelation(OsmParser.OsmRelation relation) {
			OsmRelation currentRelation = new OsmRelation(relation);

			// only use relations with a tag specified in ptFilter
			if (OsmNetworkReaderWithPT.this.ptFilter.matches(currentRelation.tags)) {
				this.relations.put(currentRelation.id, currentRelation);
			}
		}

		@Override
		public void handleNode(OsmParser.OsmNode node) {
			this.nodes.put(node.id, new OsmNode(node.id, transformation.transform(node.coord)));
		}

		@Override
		public void handleWay(OsmParser.OsmWay way) {
			OsmWay currentWay = new OsmWay(way);
			if (!currentWay.nodes.isEmpty()) {
				// only take ways which are highway or railway
				OsmWayDefaults osmHighwayDefaults = OsmNetworkReaderWithPT.this.highwayDefaults.get(currentWay.tags.get(TAG_HIGHWAY));
				OsmWayDefaults osmRailwayDefaults = OsmNetworkReaderWithPT.this.railwayDefaults.get(currentWay.tags.get(TAG_RAILWAY));

				// filter out not usable rails (with tags service=*)
				if(currentWay.tags.containsKey(TAG_SERVICE)) {
					osmRailwayDefaults = null;
				}

				if (osmHighwayDefaults != null || osmRailwayDefaults != null) {
					currentWay.used = true;
				}
				this.ways.put(currentWay.id, currentWay);
			}
		}
	}

}

