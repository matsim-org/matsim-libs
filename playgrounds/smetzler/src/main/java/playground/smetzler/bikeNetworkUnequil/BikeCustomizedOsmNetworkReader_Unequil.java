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

package playground.smetzler.bikeNetworkUnequil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import playground.smetzler.parseElevationData.ParseEleDataFromGeoTiff;

/**
 * Reads in an OSM-File, exported from <a href="http://openstreetmap.org/" target="_blank">OpenStreetMap</a>,
 * and extracts information about roads to generate a MATSim-Network.
 * 
 This is an customized version of org.matsim.core.utils.io.OsmNetworkReader, which 
 * parses for the following additional tags:
 * 
 * \\TODO exchange the following tags for bike
 * <code>matsim_transport_mode</code> (expects on of {vehicular,pedestrian})
 * <code>matsim_freespeed</code> (freespeed in m/s)
 * <code>matsim_flowcapacity</code> (flow capacity in 1/s)
 * <code>matsim_lanes</code> (number of lanes)
 * <code>matsim_min_width</code> (the minimal width of a way in m)
 * 
 * \\TODO the following comment should be similar like this but adapted exactly to the things-to-note in context of bike
 * Note: This is experimental customized version of the org.matsim.core.utils.io.OsmNetworkReader and should only be used by persons how now what they do.
 * The additional tags and the interpretation of the tags may change without notice, so don't rely on them!
 * 
 *
 * OSM-Files can be obtained:
 * <ul>
 * <li>by <a href="http://openstreetmap.org/export" target="_blank">exporting</a> the data directly from OpenStreetMap.
 * This works only for smaller regions.</li>
 * <li>by <a href="http://wiki.openstreetmap.org/wiki/Getting_Data" target="_blank">downloading</a> the requested
 * data from OpenStreetMap.
 * <li>by extracting the corresponding data from <a href="http://planet.openstreetmap.org/" target="_blank">Planet.osm</a>. Planet.osm contains
 * the <em>complete</em> data from OpenStreetMap and is huge! Thus, you must extract a subset of data to be able
 * to process it.  For some countries, there are
 * <a href="http://wiki.openstreetmap.org/wiki/Planet.osm#Extracts" target="_blank">Extracts</a> available containing
 * only the data of a single country.</li>
 * </ul>
 *
 * OpenStreetMap only contains limited information regarding traffic flow in the streets. The most valuable attribute
 * of OSM data is a <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway" target="_blank">categorization</a>
 * of "ways". This reader allows to set {@link #setHighwayDefaults(int, String, double, double, double, double) defaults} how
 * those categories should be interpreted to create a network with suitable attributes for traffic simulation.
 * For the most common highway-types, some basic defaults can be loaded automatically (see code), but they can be
 * overwritten if desired. If the optional attributes <code>lanes</code> and <code>oneway</code> are set in the
 * osm data, they overwrite the default values. Using {@link #setHierarchyLayer(double, double, double, double, int) hierarchy layers},
 * multiple overlapping areas can be specified, each with a different denseness, e.g. one only containing motorways,
 * a second one containing every link down to footways.
 *
 * @author mrieser, aneumann
 */
public class BikeCustomizedOsmNetworkReader_Unequil implements MatsimSomeReader {

	private final static Logger log = Logger.getLogger(BikeCustomizedOsmNetworkReader_Unequil.class);

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
	private final static String TAG_ONEWAY = "oneway";
	private final static String TAG_ACCESS = "access";
	// new
	private final static String TAG_CYCLEWAYTYPE= "cycleway";
	private final static String TAG_SURFACE = "surface";
	private final static String TAG_SMOOTHNESS = "smoothness";
	private final static String TAG_BICYCLE = "bicycle";
	private final static String TAG_NOONEWAYBIKE = "oneway:bicycle";
	
	//only for unequil
	private final static String TAG_SLOPE= "slope";


	//	private final static String TAG_SIGNAL = "traffic_signal";



	private ObjectAttributes bikeAttributes = new ObjectAttributes();
	private int countCyclewaytype = 0;
	private int countSurface = 0;
	private int countSmoothness = 0;
	private int countSignalLinks = 0;
	boolean firsttimeParseGeoTiff = true;

	Set<String> modesB = new HashSet<String>();

	List<Long> signalNodes = new ArrayList<Long>();
	//	List<Long> crossingNodes = new ArrayList<Long>();
	List<Long> monitorNodes = new ArrayList<Long>();


	Long currentNodeID = null;
	//

	private final static String[] ALL_TAGS = new String[] {TAG_LANES, TAG_HIGHWAY, TAG_MAXSPEED, TAG_JUNCTION, TAG_ONEWAY, TAG_ACCESS, TAG_CYCLEWAYTYPE, TAG_SURFACE, TAG_SMOOTHNESS, TAG_BICYCLE, TAG_NOONEWAYBIKE, TAG_SLOPE};

	private final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	private final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();
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

	private boolean constructingBikeNetwork = true;
	private boolean constructingCarNetwork = true;

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 */
	public BikeCustomizedOsmNetworkReader_Unequil(final Network network, final CoordinateTransformation transformation) {
		this(network, transformation, true);
	}

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 * @param useHighwayDefaults Highway defaults are set to standard values, if true.
	 */
	public BikeCustomizedOsmNetworkReader_Unequil(final Network network, final CoordinateTransformation transformation, final boolean useHighwayDefaults) {
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
			this.setHighwayDefaults(4, "secondary_link",1,  60.0/3.6, 1.0, 1000);
			this.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(5, "tertiary_link", 1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);

			//for cycleways
			//highway=    ( http://wiki.openstreetmap.org/wiki/Key:highway )
			this.setHighwayDefaults(7, "track",			 1,  10.0/3.6, 1.0,  50);	
			this.setHighwayDefaults(7, "cycleway",		 1,  10.0/3.6, 1.0,  50);

			this.setHighwayDefaults(8, "service",		 1,  10.0/3.6, 1.0,  50);	
			this.setHighwayDefaults(8, "path", 		   	 1,  10.0/3.6, 1.0,  50); // if bicycle=yes or designated
			this.setHighwayDefaults(8, "pedestrian", 	 1,  10.0/3.6, 1.0,  50); // if bicycle=yes, otherwise just pushing the bike (=slow) //TODO is this already implemented?
			this.setHighwayDefaults(8, "footway", 		 1,  10.0/3.6, 1.0,  50); // if bicycle=yes			
			//	this.setHighwayDefaults(9, "steps", 		 1,   2.0/3.6, 1.0,  50);

			///what cyclewaytypes do exist on osm? - lane, track, shared_busway
		
		}
	}

	/**
	 * Parses the given osm file and creates a MATSim network from the data.
	 *
	 * @param osmFilename
	 * @throws UncheckedIOException
	 */
	public void parse(final String osmFilename) {
		parse(osmFilename, null);
	}

	/**
	 * Parses the given input stream and creates a MATSim network from the data.
	 *
	 * @param stream
	 * @throws UncheckedIOException
	 */
	public void parse(final InputStream stream) throws UncheckedIOException {
		parse(null, stream);
	}

	/**
	 * Either osmFilename or stream must be <code>null</code>, but not both.
	 *
	 * @param osmFilename
	 * @param stream
	 * @throws UncheckedIOException
	 */
	private void parse(final String osmFilename, final InputStream stream) throws UncheckedIOException {
		if(this.hierarchyLayers.isEmpty()){
			log.warn("No hierarchy layer specified. Will convert every highway specified by setHighwayDefaults.");
		}

		OsmXmlParser parser = null;
		if (this.slowButLowMemory) {
			log.info("parsing osm file first time: identifying nodes used by ways");
			parser = new OsmXmlParser(this.nodes, this.ways, this.transform);
			parser.enableOptimization(1);
			if (stream != null) {
				parser.parse(new InputSource(stream));
			} else {
				parser.readFile(osmFilename);
			}
			log.info("parsing osm file second time: loading required nodes and ways");
			parser.enableOptimization(2);
			if (stream != null) {
				parser.parse(new InputSource(stream));
			} else {
				parser.readFile(osmFilename);
			}
			log.info("done loading data");
		} else {
			parser = new OsmXmlParser(this.nodes, this.ways, this.transform);
			if (stream != null) {
				parser.parse(new InputSource(stream));
			} else {
				parser.readFile(osmFilename);
			}
		}
		convert();
		log.info("= conversion statistics: ==========================");
		log.info("osm: # nodes read:       " + parser.nodeCounter.getCounter());
		log.info("osm: # ways read:        " + parser.wayCounter.getCounter());
		log.info("MATSim: # nodes created: " + this.network.getNodes().size());
		log.info("MATSim: # links created: " + this.network.getLinks().size());
		//new
		log.info("BikeObjectAttributs for cyclewaytype created: " + countCyclewaytype);
		log.info("BikeObjectAttributs for surface created: " + countSurface);
		log.info("BikeObjectAttributs for smoothness created: " + countSmoothness);

		log.info("No of Signalnodes: " + signalNodes.size());
		log.info("BikeObjectAttributs for signalLink created: " + countSignalLinks);

		log.info("No of Pegelnodes: " + monitorNodes.size());

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
	 * @param lanesPerDirection number of lanes on that road type <em>in each direction</em>
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 *
	 * @see <a href="http://wiki.openstreetmap.org/wiki/Map_Features#Highway">http://wiki.openstreetmap.org/wiki/Map_Features#Highway</a>
	 */
	public void setHighwayDefaults(final int hierarchy , final String highwayType, final double lanesPerDirection, final double freespeed, final double freespeedFactor, final double laneCapacity_vehPerHour) {
		setHighwayDefaults(hierarchy, highwayType, lanesPerDirection, freespeed, freespeedFactor, laneCapacity_vehPerHour, false);
	}

	/**
	 * Sets defaults for converting OSM highway paths into MATSim links.
	 *
	 * @param hierarchy The hierarchy layer the highway appears in.
	 * @param highwayType The type of highway these defaults are for.
	 * @param lanesPerDirection number of lanes on that road type <em>in each direction</em>
	 * @param freespeed the free speed vehicles can drive on that road type [meters/second]
	 * @param freespeedFactor the factor the freespeed is scaled
	 * @param laneCapacity_vehPerHour the capacity per lane [veh/h]
	 * @param oneway <code>true</code> to say that this road is a oneway road
	 */
	public void setHighwayDefaults(final int hierarchy, final String highwayType, final double lanesPerDirection, final double freespeed,
			final double freespeedFactor, final double laneCapacity_vehPerHour, final boolean oneway) {
		this.highwayDefaults.put(highwayType, new OsmHighwayDefaults(hierarchy, lanesPerDirection, freespeed, freespeedFactor, laneCapacity_vehPerHour, oneway));
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
		this.hierarchyLayers.add(new OsmFilter(this.transform.transform(new Coord(coordNWLon, coordNWLat)), this.transform.transform(new Coord(coordSELon, coordSELat)), hierarchy));
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
		if (this.network instanceof Network) {
			((Network) this.network).setCapacityPeriod(3600);
		}

		Iterator<Entry<Long, OsmWay>> it = this.ways.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Long, OsmWay> entry = it.next();
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
			}
		}

		if (!this.keepPaths) {
			// marked nodes as unused where only one way leads through
			for (OsmNode node : this.nodes.values()) {
				if (node.ways == 1) {
				//	node.used = false; //// TODO achtung, modifikation fuer equil
					node.used = true;
				}
			}
			// verify we did not mark nodes as unused that build a loop
			for (OsmWay way : this.ways.values()) {
				String highway = way.tags.get(TAG_HIGHWAY);
				if ((highway != null) && (this.highwayDefaults.containsKey(highway))) {
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
			if (node.used) {
				Node nn = this.network.getFactory().createNode(Id.create(node.id, Node.class), node.coord);
				this.network.addNode(nn);
			}
		}

		// create the links
		this.id = 1;
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get(TAG_HIGHWAY);
			if (highway != null) {
				OsmNode fromNode = this.nodes.get(way.nodes.get(0));
				double length = 0.0;
				OsmNode lastToNode = fromNode;
				if (fromNode.used) {
					for (int i = 1, n = way.nodes.size(); i < n; i++) {
						OsmNode toNode = this.nodes.get(way.nodes.get(i));
						if (toNode != lastToNode) {
							length += CoordUtils.calcEuclideanDistance(lastToNode.coord, toNode.coord);
							if (toNode.used) {

								if(this.hierarchyLayers.isEmpty()) {
									createLink(this.network, way, fromNode, toNode, length);
								} else {
									for (OsmFilter osmFilter : this.hierarchyLayers) {
										if(osmFilter.coordInFilter(fromNode.coord, way.hierarchy)){
											createLink(this.network, way, fromNode, toNode, length);
											break;
										}
										if(osmFilter.coordInFilter(toNode.coord, way.hierarchy)){
											createLink(this.network, way, fromNode, toNode, length);
											break;
										}
									}
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
	}

	private void createLink(final Network network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length) {
		String highway = way.tags.get(TAG_HIGHWAY);

		if ("no".equals(way.tags.get(TAG_ACCESS))) {
			return;
		}

		// load defaults
		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway);
		if (defaults == null) {
			this.unknownHighways.add(highway);
			return;
		}

		double nofLanes = defaults.lanesPerDirection;
		double laneCapacity = defaults.laneCapacity;
		double freespeed = defaults.freespeed;
		double freespeedFactor = defaults.freespeedFactor;
		boolean oneway = defaults.oneway;
		boolean onewayReverse = false;
		boolean onewayReverseBikeallowed = false;


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
			else {
				log.warn("Could not interpret oneway tag:" + onewayTag + ". Ignoring it.");
			}
		}

		// check tag "onewayTagBike" (Wenn eine Einbahnstraße für der Radverkehr geoeffnet ist)
		String noonewayTagBike = way.tags.get(TAG_NOONEWAYBIKE);
		if (noonewayTagBike != null) {
			//System.out.println("onewayTagBike");
			if ("no".equals(noonewayTagBike)) {
				onewayReverseBikeallowed = true;
			} 
			else {
				onewayReverseBikeallowed = false;
				log.warn("Could not interpret oneway tag:" + onewayTag + ". Ignoring it.");
			}
		}

		// In case trunks, primary and secondary roads are marked as oneway,
		// the default number of lanes should be two instead of one.
		if(highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary")){
			if((oneway || onewayReverse) && nofLanes == 1.0){
				nofLanes = 2.0;
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
				double totalNofLanes = Double.parseDouble(lanesTag);
				if (totalNofLanes > 0) {
					nofLanes = totalNofLanes;

					//By default, the OSM lanes tag specifies the total number of lanes in both directions.
					//So if the road is not oneway (onewayReverse), let's distribute them between both directions
					//michalm, jan'16
					if (!oneway && !onewayReverse) {
						nofLanes /= 2.;
					}
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

		//String bicycleTag = way.tags.get(TAG_BICYCLE);
		// only create link, if both nodes were found, node could be null, since nodes outside a layer were dropped
		Id<Node> fromId = Id.create(fromNode.id, Node.class);
		Id<Node> toId = Id.create(toNode.id, Node.class);
		if(network.getNodes().get(fromId) != null && network.getNodes().get(toId) != null){
			String origId = Long.toString(way.id);



			///
			/////////BIKE AND CAR //////
			////hin
			if (!onewayReverse ) {
				if (constructingBikeNetwork) {
					Link bikel = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(fromId), network.getNodes().get(toId));
					if (  !((defaults.hierarchy == 1) || (defaults.hierarchy == 2)) //&&  //autobahnen raus und schnellstarassen
//							((defaults.hierarchy != 8) || 
//							(defaults.hierarchy == 8) && (bicycleTag!= null && (bicycleTag.equals("yes") || bicycleTag.equals("designated"))))
							){ 
						bikel.setLength(length);
						bikel.setFreespeed(getBikeFreespeed(way, fromNode, toNode, length, true, this.id));
						bikel.setCapacity(capacity);
						bikel.setNumberOfLanes(nofLanes);
						modesB.add("bike");
						bikel.setAllowedModes(modesB);
						if (bikel instanceof Link) {
							final String id1 = origId;
							NetworkUtils.setOrigId( ((Link) bikel), id1 ) ;
						}
						network.addLink(bikel);
						bikeLinkAtts(way, fromNode, toNode, length, true, this.id);
				
					}	
				}
			
				if (constructingCarNetwork) {
					if (defaults.hierarchy == 6 || defaults.hierarchy == 7 || defaults.hierarchy == 8) {
						//do nothing
					}else{
						Link l = network.getFactory().createLink(Id.create(this.id+"_car", Link.class), network.getNodes().get(fromId), network.getNodes().get(toId));
						l.setLength(length);
						l.setFreespeed(freespeed);
						l.setCapacity(capacity);
						l.setNumberOfLanes(nofLanes);
						if (l instanceof Link) {
							final String id1 = origId;
							NetworkUtils.setOrigId( ((Link) l), id1 ) ;
						}
						network.addLink(l);
					}	
				}
				
				this.id++;
			}
			
			//rueck
			if (!oneway || onewayReverseBikeallowed) {

				if (constructingBikeNetwork) {
					Link bikel = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(toId), network.getNodes().get(fromId));
//					if (  !((defaults.hierarchy == 1) || (defaults.hierarchy == 2)) && ( //autobahnen raus und schnellstarassen
//							(defaults.hierarchy != 8) || 
//							(defaults.hierarchy == 8) && (bicycleTag!= null && (bicycleTag.equals("yes") || bicycleTag.equals("designated")))
//							)){ 
					if (  !((defaults.hierarchy == 1) || (defaults.hierarchy == 2))  //autobahnen raus und schnellstarassen
							){ 
						bikel.setLength(length);
						bikel.setFreespeed(getBikeFreespeed(way, fromNode, toNode, length, false, this.id));
						bikel.setCapacity(capacity);
						bikel.setNumberOfLanes(nofLanes);
						modesB.add("bike");
						bikel.setAllowedModes(modesB);
						if (bikel instanceof Link) {
							final String id1 = origId;
							NetworkUtils.setOrigId( ((Link) bikel), id1 ) ;
						}
						network.addLink(bikel);
						bikeLinkAtts(way, fromNode, toNode, length, false, this.id);
				
					}
				}
			
				if (constructingCarNetwork) {
					//autolink nur wenn straße groß genug
					if (defaults.hierarchy == 6 || defaults.hierarchy == 7 || defaults.hierarchy == 8) {
						//do nothing
					}else{
						Link l = network.getFactory().createLink(Id.create(this.id+"_car", Link.class), network.getNodes().get(toId), network.getNodes().get(fromId));
						l.setLength(length);
						l.setFreespeed(freespeed);
						l.setCapacity(capacity);
						l.setNumberOfLanes(nofLanes);
						if (l instanceof Link) {
							final String id1 = origId;
							NetworkUtils.setOrigId( ((Link) l), id1 ) ;
						}
						network.addLink(l);
					}	
				}
				this.id++;
			}
			
		}
	}

	// good example for setting parameters for bike-routing:
	// https://github.com/graphhopper/graphhopper/blob/master/core/src/main/java/com/graphhopper/routing/util/BikeCommonFlagEncoder.java
	private double getBikeFreespeed(final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length, boolean hinweg, long matsimID) {

		double bike_freespeed_highway = 0;
		double bike_freespeed_surface = 0;
		
		String bicycleTag2 = way.tags.get(TAG_BICYCLE);


		
		/// HIGHWAY
		String highwayTag = way.tags.get(TAG_HIGHWAY);
		if (highwayTag != null) {
			switch (highwayTag){
			case "cycleway": 			bike_freespeed_highway= 18; break;

			case "path":			//TODO doesnt work yeT!!!!!!!!!!!!!	
			if (bicycleTag2 != null) {	
				if (bicycleTag2.equals("yes") || bicycleTag2.equals("designated")) {		
					bike_freespeed_highway=  15; break;}
				else 
					bike_freespeed_highway=  12; break;}
			else
				{bike_freespeed_highway=  12; break;}
			case "footway": 
				if (bicycleTag2 != null) {	
					if (bicycleTag2.equals("yes") || bicycleTag2.equals("designated")) {		
						bike_freespeed_highway=  15; break;}
					else 
						bike_freespeed_highway=  8; break;}
				else
					{bike_freespeed_highway=  8; break;}
			case "pedestrian":
				if (bicycleTag2 != null) {	
					if (bicycleTag2.equals("yes") || bicycleTag2.equals("designated")) {		
						bike_freespeed_highway=  15; break;}
					else 
						bike_freespeed_highway=  8; break;}
				else
					{bike_freespeed_highway=  8; break;}
			case "track": 				bike_freespeed_highway= 12; break; 
			case "service": 			bike_freespeed_highway= 14; break; 
			case "residential":			bike_freespeed_highway= 18; break;
			case "minor":				bike_freespeed_highway= 16; break;

			case "unclassified":		bike_freespeed_highway= 16; break;  // if no other highway applies
			case "road": 				bike_freespeed_highway= 12; break;  // unknown road

//			case "trunk": 				bike_freespeed_highway= 18; break;  // shouldnt be used by bikes anyways
//			case "trunk_link":			bike_freespeed_highway= 18; break; 	// shouldnt be used by bikes anyways
			case "primary": 			bike_freespeed_highway= 18; break; 
			case "primary_link":		bike_freespeed_highway= 18; break; 
			case "secondary":			bike_freespeed_highway= 18; break; 
			case "secondary_link":		bike_freespeed_highway= 18; break; 
			case "tertiary": 			bike_freespeed_highway= 18; break;	 
			case "tertiary_link":		bike_freespeed_highway= 18; break; 
			case "living_street":		bike_freespeed_highway= 14; break;
		//	case "steps":				bike_freespeed_highway=  2; break; //should steps be added??
			default: 					bike_freespeed_highway=  14; log.info(highwayTag + " highwayTag not recognized");
			}
		}
		else {
			bike_freespeed_highway= 14;
			log.info("no highway info");
		}
		//		TODO http://wiki.openstreetmap.org/wiki/DE:Key:tracktype		
		//		TrackTypeSpeed("grade1", 18); // paved
		//      TrackTypeSpeed("grade2", 12); // now unpaved ...
		//      TrackTypeSpeed("grade3", 8);
		//      TrackTypeSpeed("grade4", 6);
		//      TrackTypeSpeed("grade5", 4); // like sand/grass   

		// 	TODO may be useful to combine with smoothness-tag

		/// SURFACE
		String surfaceTag = way.tags.get(TAG_SURFACE);
		if (surfaceTag != null) {
			switch (surfaceTag){
			case "paved": 					bike_freespeed_surface=  18; break;
			case "asphalt": 				bike_freespeed_surface=  18; break;
			case "cobblestone":				bike_freespeed_surface=   9; break;
			case "cobblestone (bad)":		bike_freespeed_surface=   8; break;
			case "cobblestone;flattened":
			case "cobblestone:flattened": 	bike_freespeed_surface=  10; break;
			case "sett":					bike_freespeed_surface=  10; break;

			case "concrete": 				bike_freespeed_surface=  18; break;
			case "concrete:lanes": 			bike_freespeed_surface=  16; break;
			case "concrete_plates":
			case "concrete:plates": 		bike_freespeed_surface=  16; break;
			case "paving_stones": 			bike_freespeed_surface=  12; break;
			case "paving_stones:35": 
			case "paving_stones:30": 		bike_freespeed_surface=  12; break;

			case "unpaved": 				bike_freespeed_surface=  14; break;
			case "compacted": 				bike_freespeed_surface=  16; break;
			case "dirt": 					bike_freespeed_surface=  10; break;
			case "earth": 					bike_freespeed_surface=  12; break;
			case "fine_gravel": 			bike_freespeed_surface=  16; break;

			case "gravel": 					bike_freespeed_surface=   8; break;
			case "ground": 					bike_freespeed_surface=  12; break;
			case "wood": 					bike_freespeed_surface=   8; break;
			case "pebblestone": 			bike_freespeed_surface=  16; break;
			case "sand": 					bike_freespeed_surface=   8; break; //very different kinds of sand :(

			case "bricks": 					bike_freespeed_surface=  14; break;
			case "stone": 					bike_freespeed_surface=  14; break;
			case "grass": 					bike_freespeed_surface=   8; break;

			case "compressed": 				bike_freespeed_surface=  10; break; //guter sandbelag
			case "asphalt;paving_stones:35":bike_freespeed_surface=  16; break;
			case "paving_stones:3": 		bike_freespeed_surface=  12; break;

			default: 						bike_freespeed_surface=  14; log.info(surfaceTag + " surface not recognized");
			}		
		}
		else {
			if (highwayTag != null) {
				if (highwayTag.equals("primary") || highwayTag.equals("primary_link") ||highwayTag.equals("secondary") || highwayTag.equals("secondary_link")) {	
					bike_freespeed_surface= 18;
				} else {
					bike_freespeed_surface = 14;
					//log.info("no surface info");
				}
			}
		}
		
		//Minimum of surface_speed and highwaytype_speed
		double bike_freespeedMin = Math.min(bike_freespeed_surface, bike_freespeed_highway);



		/// SLOPE
	//	double slopeTag = getSlope(way, fromNode, toNode, length, hinweg, matsimID);
		double slopeSpeedFactor = 1; 
		String slopeTagStr = way.tags.get(TAG_SLOPE);
		if (slopeTagStr != null) {	
			/// unequil stuff
		double slopeTag =  Double.parseDouble(slopeTagStr);
		bikeAttributes.putAttribute(Long.toString(matsimID), "slope", slopeTag);
		

		if (slopeTag > 0.10) {								//// uphill
			slopeSpeedFactor= 0.1;
		} else if (slopeTag <=  0.10 && slopeTag >  0.05) {		
			slopeSpeedFactor= 0.4;		
		} else if (slopeTag <=  0.05 && slopeTag >  0.03) {
			slopeSpeedFactor= 0.6;	
		} else if (slopeTag <=  0.03 && slopeTag >  0.01) {
			slopeSpeedFactor= 0.8;
		} else if (slopeTag <=  0.01 && slopeTag > -0.01) { //// flat
			slopeSpeedFactor= 1;
		} else if (slopeTag <= -0.01 && slopeTag > -0.03) {	//// downhill
			slopeSpeedFactor= 1.2;
		} else if (slopeTag <= -0.03 && slopeTag > -0.05) {	
			slopeSpeedFactor= 1.3;
		} else if (slopeTag <= -0.05 && slopeTag > -0.10) {	
			slopeSpeedFactor= 1.4;
		} else if (slopeTag <= -0.10) {	
			slopeSpeedFactor= 1.5;
		}
		}

		// speed on links that lead to a signal crossing are reduced by 10%
		double signalSpeedReductionFactor = 1;
		for (Long SigNodeID : signalNodes) {
			if (toNode.id == SigNodeID){
				signalSpeedReductionFactor= 0.9;
			}
		}

		//bike_freespeed incl. slope und signal
		double bike_freespeed= bike_freespeedMin*slopeSpeedFactor*signalSpeedReductionFactor;

		//not slower than 4km/h
		bike_freespeed = Math.max(bike_freespeed, 4.0);
		return bike_freespeed/3.6;
	}

	
	
	private double getSlope(final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length, boolean hinweg, long matsimID) {

		String matsimId = Long.toString(matsimID); 		// MAsim Link ID
		double realSlope= 0;

		ParseEleDataFromGeoTiff tiffObject = new ParseEleDataFromGeoTiff();
		try {
			double heightFrom = tiffObject.parseGeoTiff(fromNode.coord.getX(), fromNode.coord.getY(), firsttimeParseGeoTiff);
			firsttimeParseGeoTiff = false;
			double heightTo = tiffObject.parseGeoTiff(toNode.coord.getX(), toNode.coord.getY(), firsttimeParseGeoTiff);
			double eleDiff = heightTo - heightFrom;
			double slope = eleDiff/length;

			//for better visualisation
			double avgHeight= (heightFrom+heightTo)/2;
			bikeAttributes.putAttribute(matsimId, "avgHeight", avgHeight);
			//

			if (hinweg){
				realSlope = slope;
				bikeAttributes.putAttribute(matsimId, "eleDiff", eleDiff);
				bikeAttributes.putAttribute(matsimId, "slope", slope);
			} else {
				realSlope = -1*slope;
				bikeAttributes.putAttribute(matsimId, "eleDiff", -1*eleDiff);
				bikeAttributes.putAttribute(matsimId, "slope", realSlope);
			}

		} catch (Exception e) {
			e.printStackTrace();}

		return realSlope;
	}

	// schreiben der Bike-Attribute: wichtig fuer Disutility und Visualisierung
	private void bikeLinkAtts(final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length, boolean hinweg, long matsimID) {

		String matsimId = Long.toString(matsimID); 		// MAsim Link ID
		//		String orgOSMId = Long.toString(way.id);	// Original OSM LinkID


		// cyclewaytype
		String cyclewaytypeTag = way.tags.get(TAG_CYCLEWAYTYPE);
		if (cyclewaytypeTag != null) {
			bikeAttributes.putAttribute(matsimId, "cyclewaytype", cyclewaytypeTag);
			countCyclewaytype++;
		};
		
		//highwaytype
		String highwayTag = way.tags.get(TAG_HIGHWAY);
		if (highwayTag != null) {
			bikeAttributes.putAttribute(matsimId, "highway", highwayTag);
			//countHighway++;
		};

		//surfacetype
		String surfaceTag = way.tags.get(TAG_SURFACE);
		if (surfaceTag != null) {
			bikeAttributes.putAttribute(matsimId, "surface", surfaceTag);
			countSurface++;
		};
			//osm defaeult for prim and sec highways is asphalt
			if ((surfaceTag != null) && (highwayTag.equals("primary") || highwayTag.equals("secondary"))){
			bikeAttributes.putAttribute(matsimId, "surface", "asphalt");
			};

		//smoothness
		String smoothnessTag = way.tags.get(TAG_SMOOTHNESS);
		if (smoothnessTag != null) {
			bikeAttributes.putAttribute(matsimId, "smoothness", smoothnessTag);
			countSmoothness++;
		};



		//bicycleTag
		String bicycleTag = way.tags.get(TAG_BICYCLE);
		if (bicycleTag != null) {
			bikeAttributes.putAttribute(matsimId, "bicycleTag", bicycleTag);
			//countHighway++;
		};


		/// Crossing and Signal
		//		for (Long CroNodeID : crossingNodes) {
		//			if (toNode.id == CroNodeID)
		//				bikeAttributes.putAttribute(matsimId, "junctionTag", 1);   
		//		};
		//info hier
		for (Long SigNodeID : signalNodes) {
			if (toNode.id == SigNodeID){
				bikeAttributes.putAttribute(matsimId, "junctionTag", "signal");
				countSignalLinks++;}
		};


		///monitoring tag
		//iterates over all nodes that are on this way, is one of them a pegelnode?
		for(int i=0; i<way.nodes.size(); i++) {
			Long AllWayNodesIDs = way.nodes.get(i).longValue();
			for (Long MoniNodeID : monitorNodes) {
				if (MoniNodeID.equals(AllWayNodesIDs)) {
					bikeAttributes.putAttribute(matsimId, "pegel", 1);   
				}
			};
		}
		//new end

	}

	//new 
	public ObjectAttributes getBikeAttributes() {
		return this.bikeAttributes;
	}
	//new end

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
		public boolean used = false;
		public int ways = 0;
		public /*final*/ Coord coord;
		//		public final Map<String, String> Nodetags = new HashMap<String, String>(4);

		//public final Map<String, String> NodeTags = new HashMap<String, String>(4);


		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
		}
	}

	private static class OsmWay {
		public final long id;
		public final List<Long> nodes = new ArrayList<Long>(4);
		public final Map<String, String> tags = new HashMap<String, String>(4);
		public int hierarchy = -1;

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	private static class OsmHighwayDefaults {

		public final int hierarchy;
		public final double lanesPerDirection;
		public final double freespeed;
		public final double freespeedFactor;
		public final double laneCapacity;
		public final boolean oneway;

		public OsmHighwayDefaults(final int hierarchy, final double lanesPerDirection, final double freespeed, final double freespeedFactor, final double laneCapacity, final boolean oneway) {
			this.hierarchy = hierarchy;
			this.lanesPerDirection = lanesPerDirection;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
	}

	private class OsmXmlParser extends MatsimXmlParser {

		private OsmWay currentWay = null;
		private final Map<Long, OsmNode> nodes;
		private final Map<Long, OsmWay> ways;
		/*package*/ final Counter nodeCounter = new Counter("node ");
		/*package*/ final Counter wayCounter = new Counter("way ");
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
					currentNodeID = id;
					double lat = Double.parseDouble(atts.getValue("lat"));
					double lon = Double.parseDouble(atts.getValue("lon"));

					this.nodes.put(id, new OsmNode(id, this.transform.transform(new Coord(lon, lat))));
					this.nodeCounter.incCounter();
				} else if (this.mergeNodes) {
					OsmNode node = this.nodes.get(Long.valueOf(atts.getValue("id")));
					if (node != null) {
						double lat = Double.parseDouble(atts.getValue("lat"));
						double lon = Double.parseDouble(atts.getValue("lon"));

						Coord c = this.transform.transform(new Coord(lon, lat));
//						node.coord.setXY(c.getX(), c.getY());
						node.coord = c ;
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
				///new
				String TagKey = StringCache.get(atts.getValue("k"));
				if (TagKey.equals("highway")){
					String TagVal = StringCache.get(atts.getValue("v"));
					if (TagVal.equals("traffic_signals")) {
						signalNodes.add(currentNodeID);}
					//					if (TagVal.equals("crossing")) {
					//						crossingNodes.add(currentNodeID);}
				}
				//counts
				if (TagKey.equals("monitoring:traffic")){
					String TagVal = StringCache.get(atts.getValue("v"));
					if (TagVal.equals("bicycle")) {
						monitorNodes.add(currentNodeID);}
				}
				//new end			

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
					OsmHighwayDefaults osmHighwayDefaults = BikeCustomizedOsmNetworkReader_Unequil.this.highwayDefaults.get(this.currentWay.tags.get(TAG_HIGHWAY));
					if (osmHighwayDefaults != null) {
						int hierarchy = osmHighwayDefaults.hierarchy;
						this.currentWay.hierarchy = hierarchy;
						if (BikeCustomizedOsmNetworkReader_Unequil.this.hierarchyLayers.isEmpty()) {
							used = true;
						}
						if (this.collectNodes) {
							used = true;
						} else {
							for (OsmFilter osmFilter : BikeCustomizedOsmNetworkReader_Unequil.this.hierarchyLayers) {
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
								this.nodes.put(id, new OsmNode(id, new Coord((double) 0, (double) 0)));
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

	}

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

	public void constructBikeNetwork(String inputOSM) {
		constructingBikeNetwork = true;
		constructingCarNetwork = false;
		parse(inputOSM);
	}

	public void constructCarNetwork(String inputOSM) {
		constructingBikeNetwork = false;
		constructingCarNetwork = true;
		parse(inputOSM);
	}
}
