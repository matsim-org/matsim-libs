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

package playground.polettif.boescpa.converters.osm.tools;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import playground.polettif.boescpa.analysis.spatialCutters.SHPFileCutter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides statistics of an OSM-Network.
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
	private final static String[] BIDIRECTIONAL_TYPES = new String[] {"primary", "secondary", "tertiary", "minor"};
	private final static String[] UNIDIRECTIONAL_TYPES = new String[] {"motorway", "motorway_link", "trunk", "trunk_link",
			"primary_link", "secondary_link", "tertiary_link", "unclassified", "residential", "living_street"};

	private OsmXmlParser parser;
	private SHPFileCutter cutter;
	private final Map<Long, Map<String, Long>> histLengthsIn, histLengthsOut;

	private OsmStats(String shpFile) {
		this();
		this.cutter = new SHPFileCutter(shpFile);
	}

	private OsmStats() {
		this.histLengthsIn = new HashMap<>();
		this.histLengthsOut = new HashMap<>();
		this.parser = new OsmXmlParser(histLengthsIn, histLengthsOut,
				TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03"));
		this.cutter = null;
	}

	public static void main(String[] args) {

		OsmStats stats;
		String osmFilePath, outputFilePath;
		if (args.length >= 2 && args.length < 4) {
			osmFilePath = args[0];
			outputFilePath = args[1];
			if (args.length == 2) {
				stats = new OsmStats();
			} else {
				String shpFilePath = args[2];
				stats = new OsmStats(shpFilePath);
			}
		} else {
			throw new RuntimeException("Wrong number of input arguments.");
		}

		stats.getStats(osmFilePath, outputFilePath);
	}

	private void getStats(final String osmFilePath, final String outputFilePath) {
		parser.parse(osmFilePath);
		if (this.cutter == null) {
			writeStats(outputFilePath, this.histLengthsOut);
		} else {
			writeStats(filepathInsert(outputFilePath, "In"), this.histLengthsIn);
			writeStats(filepathInsert(outputFilePath, "Out"), this.histLengthsOut);
		}
	}

	private String filepathInsert(String outputFilePath, String insertion) {
		int posDot = outputFilePath.lastIndexOf('.');
		return outputFilePath.substring(0,posDot) + insertion + outputFilePath.substring(posDot, outputFilePath.length());
	}

	private boolean writeStats(final String outputFilePath, Map<Long, Map<String, Long>> histLengths) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath));
			bw.write("NumberOfLanes;unknown");
			for (long lanes : histLengths.keySet()) {
				if (lanes != 0) {
					bw.write(";" + lanes);
				}
			}
			bw.newLine();
			for (String type : BIDIRECTIONAL_TYPES) {
				bw.write(type);
				for (long lanes : histLengths.keySet()) {
					if (histLengths.get(lanes).containsKey(type)) {
						bw.write(";" + (histLengths.get(lanes).get(type) / 1000));
					} else {
						bw.write(";" + 0);
					}
				}
				bw.newLine();
			}
			for (String type : UNIDIRECTIONAL_TYPES) {
				bw.write(type);
				for (long lanes : histLengths.keySet()) {
					if (histLengths.get(lanes).containsKey(type)) {
						bw.write(";" + (histLengths.get(lanes).get(type) / 1000));
					} else {
						bw.write(";" + 0);
					}
				}
				bw.newLine();
			}
			bw.newLine();
			bw.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private class OsmNode {
		public final long id;
		public final Coord coord;
		public final boolean isInSHPArea;

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
			isInSHPArea = (cutter != null && cutter.spatiallyConsideringCoord(coord));
		}
	}

	private class OsmWay {
		public final long id;
		public final List<Long> nodes = new ArrayList<>(4);
		public final Map<String, String> tags = new HashMap<>(4);

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	private class OsmXmlParser extends MatsimXmlParser {

		private OsmWay currentWay = null;
		private final Map<Long, Map<String, Long>> histLengthIn, histLengthOut;
		private final CoordinateTransformation transform;
		private final Map<Long, OsmNode> nodes = new HashMap<>();
		private final Set<String> unknownLanesTags = new HashSet<>();

		public OsmXmlParser(final Map<Long, Map<String, Long>> histLengthIn,
							final Map<Long, Map<String, Long>> histLengthOut,
							CoordinateTransformation transform) {
			super(false);
			this.transform = transform;
			this.histLengthIn = histLengthIn;
			this.histLengthOut = histLengthOut;
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			if ("node".equals(name)) {
				Long id = Long.valueOf(atts.getValue("id"));
				double lat = Double.parseDouble(atts.getValue("lat"));
				double lon = Double.parseDouble(atts.getValue("lon"));
				this.nodes.put(id, new OsmNode(id, this.transform.transform(new Coord(lon, lat))));
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
					long lengthOut = getLength(numberOfLanes, highwayType, false);
					long lengthIn = getLength(numberOfLanes, highwayType, true);
					List<Long> nodesOfWay = this.currentWay.nodes;
					OsmNode currentNode = nodes.get(nodesOfWay.get(0));
					for (int i = 1; i < nodesOfWay.size(); i++) {
						OsmNode nextNode = nodes.get(nodesOfWay.get(i));
						if (nextNode.isInSHPArea) {
							lengthIn += lengthFactor * (Math.round(CoordUtils.calcEuclideanDistance(currentNode.coord, nextNode.coord)));
						} else {
							lengthOut += lengthFactor * (Math.round(CoordUtils.calcEuclideanDistance(currentNode.coord, nextNode.coord)));
						}
						currentNode = nextNode;
					}
					setLength(numberOfLanes, highwayType, lengthOut, false);
					setLength(numberOfLanes, highwayType, lengthIn, true);
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

		private void setLength(long numberOfLanes, String highwayType, long length, boolean in) {
			Map<Long, Map<String, Long>> histLength;
			if (in) {
				histLength = this.histLengthIn;
			} else {
				histLength = this.histLengthOut;
			}

			if (!histLength.containsKey(numberOfLanes)) {
				histLength.put(numberOfLanes, new HashMap<String, Long>());
			}
			histLength.get(numberOfLanes).put(highwayType, length);
		}

		private long getLength(Long numberOfLanes, String highwayType, boolean in) {
			Map<Long, Map<String, Long>> histLength;
			if (in) {
				histLength = this.histLengthIn;
			} else {
				histLength = this.histLengthOut;
			}

			if (histLength.containsKey(numberOfLanes)) {
				if (histLength.get(numberOfLanes).containsKey(highwayType)) {
					return histLength.get(numberOfLanes).get(highwayType);
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
