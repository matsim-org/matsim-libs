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

package playground.marcel.osm2matsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.matsim.basic.v01.IdImpl;
import org.matsim.counts.Count;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class OSMReader extends MatsimXmlParser {

	private final Map<String, OSMNode> nodes = new HashMap<String, OSMNode>();
	private final Map<String, OSMWay> ways = new HashMap<String, OSMWay>();
	private final List<String> ignored = new ArrayList<String>();
	private OSMWay currentWay = null;
	private long id = 0;

	public OSMReader() {
		super();
		this.setValidating(false);
		this.ignored.add("footway");
		this.ignored.add("pedestrian");
		this.ignored.add("cycleway");
		this.ignored.add("service");
		this.ignored.add("steps");
		this.ignored.add("track");
		this.ignored.add("bridleway");

		this.ignored.add("footway; cycleway");
		this.ignored.add("footway; unclassified");
		this.ignored.add("trackj");
		this.ignored.add("pedestrian ");
		this.ignored.add("footway;steps");
		this.ignored.add("pedestrian; residential");
		this.ignored.add(" track");
		this.ignored.add("truck");
		this.ignored.add("unsurfaced");
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ("node".equals(name)) {
			this.nodes.put(atts.getValue("id"), new OSMNode(Long.parseLong(atts.getValue("id")), Double.parseDouble(atts.getValue("lat")), Double.parseDouble(atts.getValue("lon"))));
		} else if ("way".equals(name)) {
			this.currentWay = new OSMWay(Long.parseLong(atts.getValue("id")));
			this.ways.put(atts.getValue("id"), this.currentWay);
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

	public NetworkLayer convert() {

		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod(3600);

		// check which nodes are used
		for (OSMWay way : this.ways.values()) {
			String highway = way.tags.get("highway");
			if (highway != null) {
				if (!this.ignored.contains(highway)) {
					// first and last are counted twice, so they are kept in all cases
					this.nodes.get(way.nodes.get(0)).ways++;
					this.nodes.get(way.nodes.get(way.nodes.size()-1)).ways++;

					for (String id : way.nodes) {
						OSMNode node = this.nodes.get(id);
						node.used = true;
						node.ways++;
					}
				}
			}
		}

		// create the required nodes
		for (OSMNode node : this.nodes.values()) {
			if (node.ways == 1) {
				node.used = false;
			}
			if (node.used) {
				network.createNode(new IdImpl(node.id), node.coord);
			}
		}

		// create the links
		this.id = 1;
		for (OSMWay way : this.ways.values()) {
			String highway = way.tags.get("highway");
			if (highway != null) {
				if (!this.ignored.contains(highway)) {
					boolean oneway = way.tags.containsKey("oneway") && "yes".equals(way.tags.get("oneway"));
					OSMNode fromNode = this.nodes.get(way.nodes.get(0));
					double length = 0.0;
					OSMNode lastToNode = fromNode;
					if (fromNode.used) {
						for (int i = 1, n = way.nodes.size(); i < n; i++) {
							OSMNode toNode = this.nodes.get(way.nodes.get(i));
							length += lastToNode.coord.calcDistance(toNode.coord);
							if (toNode.used) {
								createLink(network, way, fromNode, toNode, oneway, length);
								fromNode = toNode;
								length = 0.0;
							}
							lastToNode = toNode;
						}
					}
				}
			}
		}
		return network;
	}

	private void createLink(final NetworkLayer network, final OSMWay way, final OSMNode fromNode, final OSMNode toNode, final boolean oneway, final double length) {
		String highway = way.tags.get("highway");

		String fromId = Long.toString(fromNode.id);
		String toId = Long.toString(toNode.id);
		String len = Double.toString(length);
		double freespeed = 13.3;
		int capacity = 600;
		int nofLanes = 1;
		String origId = Long.toString(way.id);

		if ("motorway".equals(highway)) {
			nofLanes = 2;
			capacity = 4000;
			freespeed = 120.0/3.6;
		} else if ("motorway_link".equals(highway)) {
			capacity = 1500;
			freespeed = 80.0/3.6;
		} else if ("trunk".equals(highway)) {
			capacity = 2000;
			freespeed = 80.0/3.6;
		} else if ("trunk_link".equals(highway)) {
			capacity = 1500;
			freespeed = 60.0/3.6;
		} else if ("primary".equals(highway)) {
			capacity = 1500;
			freespeed = 80.0/3.6;
		} else if ("primary_link".equals(highway)) {
			capacity = 1500;
			freespeed = 60.0/3.6;
		} else if ("secondary".equals(highway)) {
			capacity = 1000;
			freespeed = 60.0/3.6;
		} else if ("tertiary".equals(highway) || "minor".equals(highway)) {
			capacity = 600;
			freespeed = 45.0/3.6;
		} else if ("unclassified".equals(highway)) {
			capacity = 600;
			freespeed = 45.0/3.6;
		} else if ("residential".equals(highway)) {
			capacity = 600;
			freespeed = 35.0/3.6;
		} else if ("living_street".equals(highway)) {
			capacity = 300;
			freespeed = 20.0/3.6;
		} else {
			System.out.println("unknown kind of highway: " + highway);
			return;
			// later: show warning about unknown highway-type
		}

		Link l = network.createLink(new IdImpl(this.id), network.getNode(new IdImpl(fromNode.id)), network.getNode(new IdImpl(toNode.id)), length, freespeed, capacity, nofLanes);
		l.setOrigId(origId);
		this.id++;
		if (!oneway) {
			l = network.createLink(new IdImpl(this.id), network.getNode(new IdImpl(toNode.id)), network.getNode(new IdImpl(fromNode.id)), length, freespeed, capacity, nofLanes);
			l.setOrigId(origId);
			this.id++;
		}
	}

	private static class OSMNode {
		public final long id;
		public final double lat;
		public final double lon;
		public boolean used = false;
		public int ways = 0;
		public final Coord coord;
		public static final WGS84toCH1903LV03 transform = new WGS84toCH1903LV03();

		public OSMNode(final long id, final double lat, final double lon) {
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.coord = transform.transform(new CoordImpl(lon, lat));
		}
	}

	private static class OSMWay {
		public final long id;
		public final List<String> nodes = new ArrayList<String>();
		public final Map<String, String> tags = new HashMap<String, String>();
		public Count count = null;
		public OSMNode countFromNode = null;

		public OSMWay(final long id) {
			this.id = id;
		}
	}
}
