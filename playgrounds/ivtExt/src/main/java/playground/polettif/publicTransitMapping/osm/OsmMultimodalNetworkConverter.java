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
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.polettif.publicTransitMapping.config.OsmConverterConfigGroup;
import playground.polettif.publicTransitMapping.osm.lib.OsmParser;
import playground.polettif.publicTransitMapping.osm.lib.OsmParserHandler;
import playground.polettif.publicTransitMapping.osm.lib.TagFilter;
import playground.polettif.publicTransitMapping.osm.lib.OsmTag;
import playground.polettif.publicTransitMapping.osm.lib.OsmValue;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.*;

/**
 * Implemenation of a network converter. Modified version from {@link org.matsim.core.utils.io.OsmNetworkReader}
 * Uses a config file ({@link OsmConverterConfigGroup}) to store conversion parameters and default
 * values.
 *
 * @author polettif
 */
public class OsmMultimodalNetworkConverter extends Osm2MultimodalNetwork {

	private final static Logger log = Logger.getLogger(OsmMultimodalNetworkConverter.class);

	/**
	 *  Maps for nodes, ways and relations
	 */
	private Map<Long, OsmParser.OsmNode> nodes;
	private Map<Long, OsmParser.OsmWay> ways;
	private Map<Long, OsmParser.OsmRelation> relations;
	private Map<Long, Set<Long>> relationMembers = new HashMap<>();
	private final Map<Long, Long> wayIds = new HashMap<>();

	private Map<String, OsmConverterConfigGroup.OsmWayParams> highwayParams = new HashMap<>();
	private Map<String, OsmConverterConfigGroup.OsmWayParams> railwayParams = new HashMap<>();

	/**
	 *  Maps for unknown entities
	 */
	private final Set<String> unknownHighways = new HashSet<>();
	private final Set<String> unknownRailways = new HashSet<>();
	private final Set<String> unknownPTs = new HashSet<>();
	private final Set<String> unknownWays = new HashSet<>();
	private final Set<String> unknownMaxspeedTags = new HashSet<>();
	private final Set<String> unknownLanesTags = new HashSet<>();
	private long id = 0;

	/**
	 * Constructor reading config from file.
	 */
	public OsmMultimodalNetworkConverter(final String osmConverterConfigFile) {
		super(osmConverterConfigFile);
	}

	public OsmMultimodalNetworkConverter(OsmConverterConfigGroup configGroup) {
		super(configGroup);
	}

	/**
	 * Converts the osm file specified in the config and writes
	 * the network to a file (also defined in config).
	 */
	@Override
	public void run() {
		convert();
		writeNetwork();
	}

	/**
	 * Only converts the osm file, does not write the network to a file.
	 */
	@Override
	public void convert() {
		readWayParams();
		parse();
		convertToNetwork();
		cleanNetwork();
	}

	/**
	 * reads the params from the config to different containers.
	 */
	private void readWayParams() {
		for(ConfigGroup e : config.getParameterSets(OsmConverterConfigGroup.OsmWayParams.SET_NAME)) {
			OsmConverterConfigGroup.OsmWayParams w = (OsmConverterConfigGroup.OsmWayParams) e;
			if(w.getOsmKey().equals(OsmTag.HIGHWAY)) {
				highwayParams.put(w.getOsmValue(), w);
			} else if(w.getOsmKey().equals(OsmTag.RAILWAY)) {
				railwayParams.put(w.getOsmValue(), w);
			}
		}
	}

	/**
	 * Parses the osm file and creates a MATSim network from the data.
	 * @throws UncheckedIOException
	 */
	private void parse() throws UncheckedIOException {
		TagFilter parserWayFilter = new TagFilter();
		parserWayFilter.add(OsmTag.HIGHWAY);
		parserWayFilter.add(OsmTag.RAILWAY);
		parserWayFilter.addException(OsmTag.SERVICE);

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
		parser.readFile(this.config.getOsmFile());

		this.ways = handler.getWays();
		this.nodes = handler.getNodes();
		this.relations = handler.getRelations();
	}

	/**
	 * Converts the parsed osm data to MATSim nodes and links.
	 */
	private void convertToNetwork() {
		if(this.network instanceof Network) {
			((Network) this.network).setCapacityPeriod(3600);
		}

		// store of which relation a way is part of
		for(OsmParser.OsmRelation relation : this.relations.values()) {
			for(OsmParser.OsmRelationMember member : relation.members) {
				MapUtils.getSet(member.refId, relationMembers).add(relation.id);
			}
		}

		TagFilter serviceRailTracksFilter = new TagFilter();
		serviceRailTracksFilter.add(OsmTag.SERVICE);

		// remove unusable ways
		for(OsmParser.OsmWay way : ways.values()) {
			if(!highwayParams.containsKey(way.tags.get(OsmTag.HIGHWAY)) && !railwayParams.containsKey(way.tags.get(OsmTag.RAILWAY)) && !relationMembers.containsKey(way.id)) {
				way.used = false;
			} else if(!this.nodes.containsKey(way.nodes.get(0)) || !this.nodes.containsKey(way.nodes.get(way.nodes.size() - 1))) {
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
			if(this.nodes.containsKey(way.nodes.get(0)) && this.nodes.containsKey(way.nodes.get(way.nodes.size() - 1))) {
				// first and last are counted twice, so they are kept in all cases
				this.nodes.get(way.nodes.get(0)).ways++;
				this.nodes.get(way.nodes.get(way.nodes.size() - 1)).ways++;
			}

			for(Long nodeId : way.nodes) {
				OsmParser.OsmNode node = this.nodes.get(nodeId);
				node.used = true;
				node.ways++;
			}
		}

		// Clean network:
		if(!config.getKeepPaths()) {
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
						if(length <= config.getMaxLinkLength()) {
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
		OsmConverterConfigGroup.OsmWayParams wayValues;
		if(highway != null) {
			wayValues = this.highwayParams.get(highway);
			if(wayValues == null) {
				// check if bus route is on link
				if(way.tags.containsKey(OsmTag.PSV)) {
					busOnlyLink = true;
					wayValues = highwayParams.get(OsmValue.UNCLASSIFIED);
				} else {
					this.unknownHighways.add(highway);
					return;
				}
			}
		} else if(railway != null) {
			wayValues = this.railwayParams.get(railway);
			if(wayValues == null) {
				this.unknownRailways.add(railway);
				return;
			}
		} else {
			this.unknownWays.add(way.tags.values().toString());
			return;
		}
		nofLanes = wayValues.getLanes();
		laneCapacity = wayValues.getLaneCapacity();
		freespeed = wayValues.getFreespeed();
		freespeedFactor = wayValues.getFreespeedFactor();
		oneway = wayValues.getOneway();

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
				if(config.getGuessFreeSpeed()) {
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
		if(config.getScaleMaxSpeed()) {
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

		if(railway != null && railwayParams.containsKey(railway)) {
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
				if(l instanceof Link) {
					final String id1 = origId;
					NetworkUtils.setOrigId( ((Link) l), id1 ) ;
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
				if(l instanceof Link) {
					final String id1 = origId;
					NetworkUtils.setOrigId( ((Link) l), id1 ) ;
				}
				network.addLink(l);
				this.id++;
			}
		}
	}

	/**
	 * Runs the network cleaner on the street network.
	 */
	private void cleanNetwork() {
		Set<String> roadModes = CollectionUtils.stringToSet("car,bus");
		Network roadNetwork = NetworkTools.filterNetworkByLinkMode(network, roadModes);
		Network restNetwork = NetworkTools.filterNetworkExceptLinkMode(network, roadModes);
		new NetworkCleaner().run(roadNetwork);
		NetworkTools.integrateNetwork(roadNetwork, restNetwork);
		this.network = roadNetwork;
	}

	private void writeNetwork() {
		NetworkTools.writeNetwork(this.network, this.config.getOutputNetworkFile());
	}

}

