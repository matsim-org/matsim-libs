/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import playground.boescpa.lib.tools.scenarioAnalyzer.spatialEventCutters.SHPFileCutter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Returns statistics of the provided OSM-Network.
 *
 * @author boescpa
 */
public class OsmStats {

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";
	private final static String[] ALL_TAGS = new String[] {TAG_LANES, TAG_HIGHWAY, TAG_MAXSPEED, TAG_JUNCTION, TAG_ONEWAY};
	private final static String[] BIDIRECTIONAL_TYPES = new String[] {"primary", "secondary", "tertiary", "minor", "unclassified", "residential", "living_street"};
	private final static String[] UNIDIRECTIONAL_TYPES = new String[] {"motorway", "motorway_link", "trunk", "trunk_link", "primary_link", "secondary_link", "tertiary_link"};

	private OsmXmlParser parser;

	private OsmStats(Map<Long, Map<String, Long>> histLengths) {
		this.parser = new OsmXmlParser(histLengths, TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus"));
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			throw new RuntimeException("Wrong number of input arguments.");
		}

		Map<Long, Map<String, Long>> histLengths = new HashMap<>();
		OsmStats stats = new OsmStats(histLengths);
		stats.getStats(args[0]);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));
			bw.write("NumberOfLanes;unknown");
			int numberOfLanes = histLengths.keySet().size();
			for (int i = 1; i < numberOfLanes; i++) {
				bw.write(";" + i);
			}
			bw.newLine();
			for (String type : BIDIRECTIONAL_TYPES) {
				bw.write(type);
				for (long i = 0; i < numberOfLanes; i++) {
					if (histLengths.get(i).containsKey(type)) {
						bw.write(";" + (histLengths.get(i).get(type)/1000));
					} else {
						bw.write(";" + 0);
					}
				}
				bw.newLine();
			}
			for (String type : UNIDIRECTIONAL_TYPES) {
				bw.write(type);
				for (long i = 0; i < numberOfLanes; i++) {
					if (histLengths.get(i).containsKey(type)) {
						bw.write(";" + (histLengths.get(i).get(type)/1000));
					} else {
						bw.write(";" + 0);
					}
				}
				bw.newLine();
			}
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void getStats(final String osmFilename) {
		parser.parse(osmFilename);
	}

	private static class OsmNode {
		public final long id;
		public final Coord coord;

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
		}
	}

	private static class OsmWay {
		public final long id;
		public final List<Long> nodes = new ArrayList<>(4);
		public final Map<String, String> tags = new HashMap<>(4);

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	private class OsmXmlParser extends MatsimXmlParser {

		private OsmWay currentWay = null;
		private final Map<Long, Map<String, Long>> histLength;
		private final CoordinateTransformation transform;
		private final Map<Long, OsmNode> nodes = new HashMap<>();
		private final Set<String> unknownLanesTags = new HashSet<>();

		public OsmXmlParser(final Map<Long, Map<String, Long>> histLength, CoordinateTransformation transform) {
			super(false);
			this.transform = transform;
			this.histLength = histLength;
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			if ("node".equals(name)) {
				Long id = Long.valueOf(atts.getValue("id"));
				double lat = Double.parseDouble(atts.getValue("lat"));
				double lon = Double.parseDouble(atts.getValue("lon"));
				this.nodes.put(id, new OsmNode(id, this.transform.transform(new CoordImpl(lon, lat))));
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
				String highwayType = this.currentWay.tags.get(TAG_HIGHWAY);
				if (!this.currentWay.nodes.isEmpty() && considerHighwayType(highwayType)) {
					String lanesTag = this.currentWay.tags.get(TAG_LANES);
					long numberOfLanes = 0;
					if (lanesTag != null) {
						try {
							double tmp = Double.parseDouble(lanesTag.substring(0, 1));
							if (tmp > 0) {
								numberOfLanes = Math.round(tmp);
							}
						} catch (Exception e) {
							if (!this.unknownLanesTags.contains(lanesTag)) {
								this.unknownLanesTags.add(lanesTag);
								System.out.println("Could not parse lanes tag:" + e.getMessage() + ". Ignoring it.");
							}
						}
					}
					// make unidirectional
					int lengthFactor = 1;
					if (!(isUnidirectional(highwayType)
							|| (this.currentWay.tags.containsKey(TAG_ONEWAY) && this.currentWay.tags.get(TAG_ONEWAY).equals("true"))
							|| numberOfLanes == 1)) {
						// make unidirectional -> double length and half lanes
						lengthFactor = 2;
						numberOfLanes = numberOfLanes/2;
					}
					// add length:
					long length = getLength(numberOfLanes, highwayType);
					List<Long> nodesOfWay = this.currentWay.nodes;
					OsmNode currentNode = nodes.get(nodesOfWay.get(0));
					for (int i = 1; i < nodesOfWay.size(); i++) {
						OsmNode nextNode = nodes.get(nodesOfWay.get(i));
						length += lengthFactor*(Math.round(CoordUtils.calcDistance(currentNode.coord, nextNode.coord)));
						currentNode = nextNode;
					}
					setLength(numberOfLanes, highwayType, length);
				}
				this.currentWay = null;
			}
		}

		private boolean isUnidirectional(String highwayType) {
			if (highwayType != null) {
				for (String type : UNIDIRECTIONAL_TYPES) {
					if (type.equals(highwayType)) return true;
				}
			}
			return false;
		}

		private boolean considerHighwayType(String highwayType) {
			if (highwayType != null) {
				for (String type : BIDIRECTIONAL_TYPES) {
					if (type.equals(highwayType)) return true;
				}
			}
			return isUnidirectional(highwayType);
		}

		private void setLength(long numberOfLanes, String highwayType, long length) {
			if (!this.histLength.containsKey(numberOfLanes)) {
				this.histLength.put(numberOfLanes, new HashMap<String, Long>());
			}
			this.histLength.get(numberOfLanes).put(highwayType, length);
		}

		private long getLength(Long numberOfLanes, String highwayType) {
			if (this.histLength.containsKey(numberOfLanes)) {
				if (this.histLength.get(numberOfLanes).containsKey(highwayType)) {
					return this.histLength.get(numberOfLanes).get(highwayType);
				}
			}
			return 0;
		}

	}

	private static class StringCache {

		private static ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(10000);

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
