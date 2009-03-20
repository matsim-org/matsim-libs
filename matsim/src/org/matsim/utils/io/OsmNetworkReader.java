/* *********************************************************************** *
 * project: org.matsim.*
 * OSMReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.utils.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.counts.Count;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.CoordUtils;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.misc.Counter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Reads in an OSM-File, exported from <a href="http://openstreetmap.org/" target="_blank">OpenStreetMap</a>,
 * and extracts information about roads to generate a MATSim-Network.
 *
 * OSM-Files can be obtained:
 * <ul>
 * <li>by <a href="http://openstreetmap.org/export" target="_blank">exporting</a> the data directly from OpenStreetMap.
 * This works only for smaller regions.</li>
 * <li>by <a href="http://wiki.openstreetmap.org/wiki/Getting_Data" target="_blank">downloading</a> the requested
 * data from OpenStreetMap.
 * <li>by extracting the corresponding data from <a href="" target="_blank">Planet.osm</a>. Planet.osm contains
 * the <em>complete</em> data from OpenStreetMap and is huge! Thus, you must extract a subset of data to be able
 * to process it.  For some countries, there are
 * <a href="http://wiki.openstreetmap.org/wiki/Planet.osm#Extracts" target="_blank">Extracts</a> available containing
 * only the data of a single country.</li>
 * </ul>
 *
 * OpenStreetMap only contains limited information regarding traffic flow in the streets. The most valuable attribute
 * of OSM data is a <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway" target="_blank">categorization</a>
 * of "ways". This reader allows to set {@link #setHighwayDefaults(String, double, double, double) defaults} how
 * those categories should be interpreted to create a network with suitable attributes for traffic simulation.
 * For the most common highway-types, some basic defaults are loaded automatically (see code), but they can be
 * overwritten if desired. If the optional attributes <code>lanes</code> and <code>oneway</code> are set in the
 * osm data, they overwrite the default values.
 *
 * @author mrieser
 */
public class OsmNetworkReader {

	private final static Logger log = Logger.getLogger(OsmNetworkReader.class);

	private final Map<String, OsmNode> nodes = new HashMap<String, OsmNode>();
	private final Map<String, OsmWay> ways = new HashMap<String, OsmWay>();
	private final Set<String> unknownHighways = new HashSet<String>();
	private long id = 0;
	private final Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<String, OsmHighwayDefaults>();
	private final NetworkLayer network;
	/*package*/ final CoordinateTransformation transform;
	private boolean keepPaths = false;

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 */
	public OsmNetworkReader(final NetworkLayer network, final CoordinateTransformation transformation) {
		this.network = network;
		this.transform = transformation;

		this.setHighwayDefaults("motorway",      2, 120.0/3.6, 2000, true);
		this.setHighwayDefaults("motorway_link", 1,  80.0/3.6, 1500, true);
		this.setHighwayDefaults("trunk",         1,  80.0/3.6, 2000);
		this.setHighwayDefaults("trunk_link",    1,  50.0/3.6, 1500);
		this.setHighwayDefaults("primary",       1,  80.0/3.6, 1500);
		this.setHighwayDefaults("primary_link",  1,  60.0/3.6, 1500);
		this.setHighwayDefaults("secondary",     1,  60.0/3.6, 1000);
		this.setHighwayDefaults("tertiary",      1,  45.0/3.6,  600);
		this.setHighwayDefaults("minor",         1,  45.0/3.6,  600);
		this.setHighwayDefaults("unclassified",  1,  45.0/3.6,  600);
		this.setHighwayDefaults("residential",   1,  30.0/3.6,  600);
		this.setHighwayDefaults("living_street", 1,  15.0/3.6,  300);
	}

	/**
	 * Parses the given osm file and creates a MATSim network from the data.
	 *
	 * @param osmFilename
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void parse(final String osmFilename) throws SAXException, ParserConfigurationException, IOException {
		OsmXmlParser parser = new OsmXmlParser(this.nodes, this.ways, this.transform);
		parser.parse(osmFilename);
		convert();
		log.info("= conversion statistics: ==========================");
		log.info("osm: # nodes read:       " + parser.nodeCounter.getCounter());
		log.info("osm: # ways read:        " + parser.wayCounter.getCounter());
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
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 *
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway">http://wiki.openstreetmap.org/wiki/Map_Features#Highway</a>
	 */
	public void setHighwayDefaults(final String highwayType, final double lanes, final double freespeed, final double laneCapacity_vehPerHour) {
		setHighwayDefaults(highwayType, lanes, freespeed, laneCapacity_vehPerHour, false);
	}

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links.
	 *
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanes number of lanes on that road type
	 * @param freespeed the free speed vehicles can drive on that road type
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 * @param oneway <code>true</code> to say that this road is a oneway road
	 */
	public void setHighwayDefaults(final String highwayType, final double lanes, final double freespeed,
			final double laneCapacity_vehPerHour, final boolean oneway) {
		this.highwayDefaults.put(highwayType, new OsmHighwayDefaults(lanes, freespeed, laneCapacity_vehPerHour, oneway));
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

	private void convert() {
		this.network.setCapacityPeriod(3600);

		// check which nodes are used
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get("highway");
			if (highway != null) {
				if (this.highwayDefaults.containsKey(highway)) {
					// first and last are counted twice, so they are kept in all cases
					this.nodes.get(way.nodes.get(0)).ways++;
					this.nodes.get(way.nodes.get(way.nodes.size()-1)).ways++;

					for (String nodeId : way.nodes) {
						OsmNode node = this.nodes.get(nodeId);
						node.used = true;
						node.ways++;
					}
				}
			}
		}

		if (!this.keepPaths) {
			// marked nodes as unused where only one way leads through
			for (OsmNode node : this.nodes.values()) {
				if ((node.ways == 1) && (!this.keepPaths)) {
					node.used = false;
				}
			}
			// verify we did not mark nodes as unused that build a loop
			for (OsmWay way : this.ways.values()) {
				String highway = way.tags.get("highway");
				if (highway != null) {
					if (this.highwayDefaults.containsKey(highway)) {
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

		}

		// create the required nodes
		for (OsmNode node : this.nodes.values()) {
			if (node.used) {
				this.network.createNode(node.id, node.coord);
			}
		}

		// create the links
		this.id = 1;
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get("highway");
			if (highway != null) {
				OsmNode fromNode = this.nodes.get(way.nodes.get(0));
				double length = 0.0;
				OsmNode lastToNode = fromNode;
				if (fromNode.used) {
					for (int i = 1, n = way.nodes.size(); i < n; i++) {
						OsmNode toNode = this.nodes.get(way.nodes.get(i));
						if (toNode != lastToNode) {
							length += CoordUtils.calcDistance(lastToNode.coord, toNode.coord);
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
		}
	}

	private void createLink(final NetworkLayer network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length) {
		String highway = way.tags.get("highway");

		// load defaults
		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		if (defaults == null) {
			this.unknownHighways.add(highway);
			return;
		}

		String origId = Long.toString(way.id);
		double nofLanes = defaults.lanes;
		double laneCapacity = defaults.laneCapacity;
		double freespeed = defaults.freespeed;
		boolean oneway = defaults.oneway;
		boolean onewayReverse = false;

		// check if there are tags that overwrite defaults
		// - check tag "junction"
		if (way.tags.containsKey("junction")) {
			if ("roundabout".equals(way.tags.get("junction"))) {
				oneway = true;
			}
		}

		// check tag "oneway"
		if (way.tags.containsKey("oneway")) {
			String onewayTag = way.tags.get("oneway");
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
		
		if (way.tags.containsKey("maxspeed")) {
			Double maxspeed = Double.valueOf(way.tags.get("maxspeed"));
			if (maxspeed < freespeed) {
				// freespeed doesn't always mean it's the maximum speed allowed.
				// thus only correct freespeed if maxspeed is lower than freespeed.
				freespeed = maxspeed;
			}
		}

		// create the link(s)
		double capacity = nofLanes * laneCapacity;

		if (!onewayReverse) {
			Link l = network.createLink(new IdImpl(this.id), network.getNode(fromNode.id), network.getNode(toNode.id), length, freespeed, capacity, nofLanes);
			l.setOrigId(origId);
			this.id++;
		}
		if (!oneway) {
			Link l = network.createLink(new IdImpl(this.id), network.getNode(toNode.id), network.getNode(fromNode.id), length, freespeed, capacity, nofLanes);
			l.setOrigId(origId);
			this.id++;
		}
	}

	private static class OsmNode {
		public final Id id;
		public final double lat;
		public final double lon;
		public boolean used = false;
		public int ways = 0;
		public final Coord coord;

		public OsmNode(final Id id, final Coord coord, final double lat, final double lon) {
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.coord = coord;
		}
	}

	private static class OsmWay {
		public final long id;
		public final List<String> nodes = new ArrayList<String>();
		public final Map<String, String> tags = new HashMap<String, String>();
		public Count count = null;
		public OsmNode countFromNode = null;

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	private static class OsmHighwayDefaults {

		public final double lanes;
		public final double freespeed;
		public final double laneCapacity;
		public final boolean oneway;

		public OsmHighwayDefaults(final double lanes, final double freespeed, final double laneCapacity, final boolean oneway) {
			this.lanes = lanes;
			this.freespeed = freespeed;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
	}

	private static class OsmXmlParser extends MatsimXmlParser {

		private OsmWay currentWay = null;
		private final Map<String, OsmNode> nodes;
		private final Map<String, OsmWay> ways;
		/*package*/ final Counter nodeCounter = new Counter("node ");
		/*package*/ final Counter wayCounter = new Counter("way ");
		private final CoordinateTransformation transform;

		public OsmXmlParser(final Map<String, OsmNode> nodes, final Map<String, OsmWay> ways, final CoordinateTransformation transform) {
			super();
			this.nodes = nodes;
			this.ways = ways;
			this.transform = transform;
			this.setValidating(false);
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			if ("node".equals(name)) {
				Id id = new IdImpl(atts.getValue("id"));
				double lat = Double.parseDouble(atts.getValue("lat"));
				double lon = Double.parseDouble(atts.getValue("lon"));
				this.nodes.put(atts.getValue("id"), new OsmNode(id, this.transform.transform(new CoordImpl(lon, lat)), lat, lon));
				this.nodeCounter.incCounter();
			} else if ("way".equals(name)) {
				this.currentWay = new OsmWay(Long.parseLong(atts.getValue("id")));
				this.ways.put(atts.getValue("id"), this.currentWay);
				this.wayCounter.incCounter();
			} else if ("nd".equals(name)) {
				if (this.currentWay != null) {
					this.currentWay.nodes.add(atts.getValue("ref"));
				}
			} else if ("tag".equals(name)) {
				if (this.currentWay != null) {
					this.currentWay.tags.put(atts.getValue("k"), atts.getValue("v"));
				}
			}
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			if ("way".equals(name)) {
				this.currentWay = null;
			}
		}

	}

}
