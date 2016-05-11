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

package playground.polettif.publicTransitMapping.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.polettif.publicTransitMapping.osm.core.*;
import playground.polettif.publicTransitMapping.osm.lib.OsmTag;
import playground.polettif.publicTransitMapping.osm.lib.OsmValue;

import java.util.*;

/**
 * org/matsim/core/utils/io/OsmNetworkReader.java extended with the functionality
 * to recognize and tag public transport.
 * <p/>
 * Streaming with org.xml.sax.InputSource had to be removed because of privacy issues.
 *
 * @author boescpa
 */
public class OsmNetworkReaderWithPT {

	private final static Logger log = Logger.getLogger(OsmNetworkReaderWithPT.class);

	/*
	OSM TAGS
	*/

	// general tags
	private static final String MEMBER_ROLE_STOP = "stop";
	private static final String MEMBER_ROLE_STOP_FORWARD = "stop_forward";
	private static final String MEMBER_ROLE_STOP_BACKWARD = "stop_backward";


	/*
	Maps for nodes, ways and relations
	 */
	private Map<Long, OsmParser.OsmNode> nodes;
	private Map<Long, OsmParser.OsmWay> ways;
	private Map<Long, OsmParser.OsmRelation> relations;
	private Map<Long, Set<Long>> relationMembers = new HashMap<>();

	private final Map<Long, Long> wayIds = new HashMap<>();

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
	private double maxLinkLength = 500.0;
	// todo doc if a maxspeed tag looks like "50; 80" or similar, uses the first two digits
	private boolean guessFreeSpeed = true;


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
	 * @param network            An empty network where the converted OSM data will be stored.
	 * @param useHighwayDefaults Highway defaults are set to standard values, if true.
	 */
	public OsmNetworkReaderWithPT(final Network network, final CoordinateTransformation transformation, final boolean useHighwayDefaults) {
		this.network = network;
		this.transformation = transformation;

		if(useHighwayDefaults) {
			log.info("Falling back to default values.");

			// Set highway-defaults (and with it the filter...)
			this.setHighwayDefaults(OsmValue.MOTORWAY, 2, 120.0 / 3.6, 1.0, 2000, true);
			this.setHighwayDefaults(OsmValue.MOTORWAY_LINK, 1, 80.0 / 3.6, 1.0, 1500, true);
			this.setHighwayDefaults(OsmValue.TRUNK, 1, 80.0 / 3.6, 1.0, 2000);
			this.setHighwayDefaults(OsmValue.TRUNK_LINK, 1, 50.0 / 3.6, 1.0, 1500);
			this.setHighwayDefaults(OsmValue.PRIMARY, 1, 80.0 / 3.6, 1.0, 1500);
			this.setHighwayDefaults(OsmValue.PRIMARY_LINK, 1, 60.0 / 3.6, 1.0, 1500);
			this.setHighwayDefaults(OsmValue.SECONDARY, 1, 60.0 / 3.6, 1.0, 1000);
			this.setHighwayDefaults(OsmValue.TERTIARY, 1, 50.0 / 3.6, 1.0, 600);
			this.setHighwayDefaults(OsmValue.MINOR, 1, 40.0 / 3.6, 1.0, 600);
			this.setHighwayDefaults(OsmValue.UNCLASSIFIED, 1, 50.0 / 3.6, 1.0, 600);
			this.setHighwayDefaults(OsmValue.RESIDENTIAL, 1, 30.0 / 3.6, 1.0, 600);
			this.setHighwayDefaults(OsmValue.LIVING_STREET, 1, 15.0 / 3.6, 1.0, 300);
//			this.setHighwayDefaults(OsmValue.SERVICE, 1, 15.0 / 3.6, 1.0, 200);

			// Set railway-defaults (and with it the filter...)
			this.setRailwayDefaults(OsmValue.RAIL, 1, 160.0 / 3.6, 1.0, 100);
			this.setRailwayDefaults(OsmValue.TRAM, 1, 40.0 / 3.6, 1.0, 100, true);
			this.setRailwayDefaults(OsmValue.LIGHT_RAIL, 1, 80.0 / 3.6, 1.0, 100);
		}
	}

	public void setMaxLinkLength(double maxLinkLength) {
		this.maxLinkLength = maxLinkLength;
	}

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links, assuming it is no oneway road.
	 *
	 * @param highwayType             The type of highway these defaults are for.
	 * @param lanes                   number of lanes on that road type
	 * @param freespeed               the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor         the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway">http://wiki.openstreetmap.org/wiki/Map_Features#Highway</a>
	 */
	public void setHighwayDefaults(final String highwayType, final double lanes, final double freespeed, final double freespeedFactor, final double laneCapacity_vehPerHour) {
		setHighwayDefaults(highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
	}

	public void setRailwayDefaults(final String railwayType, final double lanes, final double freespeed,
								   final double freespeedFactor, final double laneCapacity_vehPerHour) {
		setRailwayDefaults(railwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
	}


	/**
	 * Sets defaults for converting OSM highway paths into MATSim links.
	 *
	 * @param highwayType             The type of highway these defaults are for.
	 * @param lanes                   number of lanes on that road type
	 * @param freespeed               the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor         the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 * @param oneway                  <code>true</code> to say that this road is a oneway road
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
	 * <p/>
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
	 * <p/>
	 * Defaults to <code>false</code>.
	 *
	 * @param scaleMaxSpeed <code>true</code> to scale the speed limit down by the value specified by the
	 *                      {@link #setHighwayDefaults(String, double, double, double, double) defaults}.
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

		TagFilter parserWayFilter = new TagFilter();
		parserWayFilter.add(OsmTag.HIGHWAY);
		parserWayFilter.add(OsmTag.RAILWAY);

		TagFilter parserRelationFilter = new TagFilter();
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.BUS);
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.TROLLEYBUS);
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.RAIL);
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.TRAM);
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.LIGHT_RAIL);
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.FUNICULAR);
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.MONORAIL);
		parserRelationFilter.add(OsmTag.ROUTE, OsmValue.SUBWAY);

		OsmParser parser = new OsmParser(transformation);
		OsmParserHandler handler = new OsmParserHandler();
		handler.addFilter(null, parserWayFilter, parserRelationFilter);
		parser.addHandler(handler);
		parser.readFile(osmFilename);

		this.ways = handler.getWays();
		this.nodes = handler.getNodes();
		this.relations = handler.getRelations();

		this.convert();

		log.info("= conversion statistics: ==========================");
		log.info("MATSim: # nodes created: " + this.network.getNodes().size());
		log.info("MATSim: # links created: " + this.network.getLinks().size());

		if(this.unknownHighways.size() > 0) {
			log.info("The following highway-types had no defaults set and were thus NOT converted:");
			for(String highwayType : this.unknownHighways) {
				log.info("- \"" + highwayType + "\"");
			}
		}
		if(this.unknownRailways.size() > 0) {
			log.info("The following railway-types had no defaults set and were thus NOT converted:");
			for(String railwayType : this.unknownRailways) {
				log.info("- \"" + railwayType + "\"");
			}
		}
		if(this.unknownPTs.size() > 0) {
			log.info("The following PT-types had no defaults set and were thus NOT converted:");
			for(String ptType : this.unknownPTs) {
				log.info("- \"" + ptType + "\"");
			}
		}
		if(this.unknownWays.size() > 0) {
			log.info("The way-types with the following tags had no defaults set and were thus NOT converted:");
			for(String wayType : this.unknownWays) {
				log.info("- \"" + wayType + "\"");
			}
		}
		log.info("= end of conversion statistics ====================");
	}

	/**
	 * Converts the parsed osm data to MATSim nodes and links.
	 */
	private void convert() {
		if(this.network instanceof NetworkImpl) {
			((NetworkImpl) this.network).setCapacityPeriod(3600);
		}

		// store of which relation a way is part of
		for(OsmParser.OsmRelation relation : relations.values()) {
			for(OsmParser.OsmRelationMember member : relation.members) {
				MapUtils.getSet(member.refId, relationMembers).add(relation.id);
			}
		}

		TagFilter serviceRailTracksFilter = new TagFilter();
		serviceRailTracksFilter.add(OsmTag.SERVICE);

		// remove unusable ways
		for(OsmParser.OsmWay way : ways.values()) {
			// remove service railways
			if(way.tags.containsKey(OsmTag.RAILWAY) && way.tags.containsKey(OsmTag.SERVICE)) {
				way.used = false;
			}
			// remove service roads without a transit route on them
			if(!highwayDefaults.containsKey(way.tags.get(OsmTag.HIGHWAY)) && !relationMembers.containsKey(way.id) && !way.tags.containsKey(OsmTag.RAILWAY)) {
				way.used = false;
			}
		}

		// remove unused ways
		Iterator<Map.Entry<Long, OsmParser.OsmWay>> it = ways.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<Long, OsmParser.OsmWay> entry = it.next();
			if(!entry.getValue().used) {
				it.remove();
			}
		}

		// check which nodes are used
		for(OsmParser.OsmWay way : this.ways.values()) {
			// first and last are counted twice, so they are kept in all cases
			this.nodes.get(way.nodes.get(0)).ways++;
			this.nodes.get(way.nodes.get(way.nodes.size() - 1)).ways++;

			for(Long nodeId : way.nodes) {
				OsmParser.OsmNode node = this.nodes.get(nodeId);
				node.used = true;
				node.ways++;
			}
		}

		// Clean network:
		if(!this.keepPaths) {
			// marked nodes as unused where only one way leads through
			// but only if this doesn't lead to links longer than MAX_LINKLENGTH
			for(OsmParser.OsmWay way : this.ways.values()) {

				double length = 0.0;
				OsmParser.OsmNode lastNode = this.nodes.get(way.nodes.get(0));
				for(int i = 1; i < way.nodes.size(); i++) {
					OsmParser.OsmNode node = this.nodes.get(way.nodes.get(i));
					if(node.ways > 1) {
						length = 0.0;
						lastNode = node;
					} else if(node.ways == 1) {
						length += CoordUtils.calcEuclideanDistance(lastNode.coord, node.coord);
						if(length <= maxLinkLength) {
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
			for(OsmParser.OsmWay way : this.ways.values()) {
				int prevRealNodeIndex = 0;
				OsmParser.OsmNode prevRealNode = this.nodes.get(way.nodes.get(prevRealNodeIndex));

				for(int i = 1; i < way.nodes.size(); i++) {
					OsmParser.OsmNode node = this.nodes.get(way.nodes.get(i));
					if(node.used) {
						if(prevRealNode == node) {
						/* We detected a loop between two "real" nodes.
						 * Set some nodes between the start/end-loop-node to "used" again.
						 * But don't set all of them to "used", as we still want to do some network-thinning.
						 * I decided to use sqrt(.)-many nodes in between...
						 */
							double increment = Math.sqrt(i - prevRealNodeIndex);
							double nextNodeToKeep = prevRealNodeIndex + increment;
							for(double j = nextNodeToKeep; j < i; j += increment) {
								int index = (int) Math.floor(j);
								OsmParser.OsmNode intermediaryNode = this.nodes.get(way.nodes.get(index));
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
		for(OsmParser.OsmNode node : this.nodes.values()) {
			if(node.used) {
				Node nn = this.network.getFactory().createNode(Id.create(node.id, Node.class), node.coord);
				this.network.addNode(nn);
			}
		}

		// create the links
		this.id = 1;
		for(OsmParser.OsmWay way : this.ways.values()) {
			OsmParser.OsmNode fromNode = this.nodes.get(way.nodes.get(0));
			double length = 0.0;
			OsmParser.OsmNode lastToNode = fromNode;
			if(fromNode.used) {
				for(int i = 1, n = way.nodes.size(); i < n; i++) {
					OsmParser.OsmNode toNode = this.nodes.get(way.nodes.get(i));
					if(toNode != lastToNode) {
						length += CoordUtils.calcEuclideanDistance(lastToNode.coord, toNode.coord);
						if(toNode.used) {
							createLink(this.network, way, fromNode, toNode, length);
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
		this.relations.clear();
	}

	/**
	 * Creates a MATSim link from osm data
	 */
	private void createLink(final Network network, final OsmParser.OsmWay way, final OsmParser.OsmNode fromNode, final OsmParser.OsmNode toNode, final double length) {
		double nofLanes;
		double laneCapacity;
		double freespeed;
		double freespeedFactor;
		boolean oneway;
		boolean onewayReverse = false;
		boolean busOnlyLink = false;

		// load defaults
		String highway = way.tags.get(OsmTag.HIGHWAY);
		String railway = way.tags.get(OsmTag.RAILWAY);
		OsmWayDefaults defaults;
		if(highway != null) {
			defaults = this.highwayDefaults.get(highway);
			if(defaults == null) {
				// check if bus route is on link todo bus lane conditions as param?
				if(way.tags.containsKey(OsmTag.PSV)) {
					busOnlyLink = true;
					defaults = highwayDefaults.get(OsmValue.UNCLASSIFIED);
				} else {
					this.unknownHighways.add(highway);
					return;
				}
			}
		} else if(railway != null) {
			defaults = this.railwayDefaults.get(railway);
			if(defaults == null) {
				this.unknownRailways.add(railway);
				return;
			}
		} else {
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
		if("roundabout".equals(way.tags.get(OsmTag.JUNCTION))) {
			// if "junction" is not set in tags, get() returns null and equals() evaluates to false
			oneway = true;
		}
		// - check tag "oneway"
		String onewayTag = way.tags.get(OsmTag.ONEWAY);
		if(onewayTag != null) {
			if("yes".equals(onewayTag)) {
				oneway = true;
			} else if("true".equals(onewayTag)) {
				oneway = true;
			} else if("1".equals(onewayTag)) {
				oneway = true;
			} else if("-1".equals(onewayTag)) {
				onewayReverse = true;
				oneway = false;
			} else if("no".equals(onewayTag)) {
				oneway = false; // may be used to overwrite defaults
			}
		}
		// - check tag "oneway" with trunks, primary and secondary roads
		// 		(if they are marked as such, the default number of lanes should be two instead of one)
		if(highway != null) {
			if(highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary")) {
				if(oneway && nofLanes == 1.0) {
					nofLanes = 2.0;
				}
			}
		}
		// - ckeck tag "maxspeed"
		String maxspeedTag = way.tags.get(OsmTag.MAXSPEED);
		if(maxspeedTag != null) {
			try {
				freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert km/h to m/s
			} catch (NumberFormatException e) {
				boolean message = true;
				if(guessFreeSpeed) {
					try {
						message = false;
						freespeed = Double.parseDouble(maxspeedTag.substring(0, 2)) / 3.6;
					} catch (NumberFormatException e1) {
						message = true;
					}
				}
				if(!this.unknownMaxspeedTags.contains(maxspeedTag) && message) {
					this.unknownMaxspeedTags.add(maxspeedTag);
					log.warn("Could not parse maxspeed tag: " + e.getMessage() + " (way " + way.id + ") Ignoring it.");
				}
			}
		}
		// - check tag "lanes"
		String lanesTag = way.tags.get(OsmTag.LANES);
		if(lanesTag != null) {
			try {
				double tmp = Double.parseDouble(lanesTag);
				if(tmp > 0) {
					nofLanes = tmp;
				}
			} catch (Exception e) {
				if(!this.unknownLanesTags.contains(lanesTag)) {
					this.unknownLanesTags.add(lanesTag);
					log.warn("Could not parse lanes tag: " + e.getMessage() + ". Ignoring it.");
				}
			}
		}

		// define the links' capacity and freespeed
		double capacity = nofLanes * laneCapacity;
		if(this.scaleMaxSpeed) {
			freespeed = freespeed * freespeedFactor;
		}

		// define modes allowed on link(s)
		//	basic type:
		Set<String> modes = new HashSet<>();
		if(!busOnlyLink && highway != null) {
			modes.add(TransportMode.car);
		}
		if(busOnlyLink) {
			modes.add("bus");
			modes.add(TransportMode.pt);
		}

		if(railway != null && railwayDefaults.containsKey(railway)) {
			modes.add(railway);
		}

		if(modes.isEmpty()) {
			modes.add("unknownStreetType");
		}

		//	public transport: get relation which this way is part of, then get the relations route=* (-> the mode)
		Set<Long> containingRelations = relationMembers.get(way.id);
		if(containingRelations != null) {
			for(Long containingRelationId : containingRelations) {
				OsmParser.OsmRelation rel = relations.get(containingRelationId);
				String mode = rel.tags.get(OsmTag.ROUTE);
				if(mode != null) {
					if(mode.equals(OsmValue.TROLLEYBUS)) {
						mode = OsmValue.BUS;
					}
					modes.add(mode);
				}
			}
		}

		// only create link, if both nodes were found, node could be null, since nodes outside a layer were dropped
		Id<Node> fromId = Id.create(fromNode.id, Node.class);
		Id<Node> toId = Id.create(toNode.id, Node.class);
		if(network.getNodes().get(fromId) != null && network.getNodes().get(toId) != null) {
			String origId = Long.toString(way.id);

			if(!onewayReverse) {
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(fromId), network.getNodes().get(toId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(modes);
				if(l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				this.id++;
			}
			if(!oneway) {
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(toId), network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(modes);
				if(l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				this.id++;
			}
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
}

