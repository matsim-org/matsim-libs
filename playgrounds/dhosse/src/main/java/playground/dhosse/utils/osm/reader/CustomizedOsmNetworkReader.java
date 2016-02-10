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

package playground.dhosse.utils.osm.reader;

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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

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
 * ADDITIONS:
 * Checks for bicycle and pt usage for osm ways in order to create a multimodal network out of the box. dhosse Feb'16
 *
 * @author mrieser, aneumann
 */
public class CustomizedOsmNetworkReader implements MatsimSomeReader {

	private final static Logger log = Logger.getLogger(CustomizedOsmNetworkReader.class);

	private final static String TAG_LANES = "lanes";
	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	private final static String TAG_JUNCTION = "junction";
    private final static String TAG_ONEWAY = "oneway";
    private final static String TAG_ACCESS = "access";
    //extension
    private final static String TAG_BUS = "bus";
    private final static String TAG_BICYCLE = "bicycle";
    private static final String TAG_CYCLEWAY = "cycleway";
    private static final String TAG_RAILWAY = "railway";
    private static final String TAG_TRACKS = "tracks";
    private static final String TAG_TYPE = "type";
    private static final String TAG_REF = "ref";
    private static final String TAG_TO = "to";
    private static final String TAG_NAME = "name";
    
	private final static String[] ALL_TAGS = new String[] {TAG_LANES, TAG_HIGHWAY, TAG_MAXSPEED, 
		TAG_JUNCTION, TAG_ONEWAY, TAG_ACCESS, TAG_BUS, TAG_BICYCLE, TAG_CYCLEWAY, TAG_RAILWAY, TAG_TRACKS,
		TAG_TYPE, TAG_REF, TAG_TO, TAG_NAME};

	private final Map<Long, OsmNode> nodes = new HashMap<Long, OsmNode>();
	private final Map<Long, OsmWay> ways = new HashMap<Long, OsmWay>();
	private final Map<Long, OsmRelation> reations = new HashMap<Long, OsmRelation>();
	private final Set<String> unknownHighways = new HashSet<String>();
	private final Set<String> unknownMaxspeedTags = new HashSet<String>();
	private final Set<String> unknownLanesTags = new HashSet<String>();
	private long id = 0;
	/*package*/ final Map<String, OsmHighwayDefaults> highwayDefaults = 
			new HashMap<String, OsmHighwayDefaults>();
	final Map<String, OsmHighwayDefaults> railwayDefaults = new HashMap<>();
	private final Network network;
	private final CoordinateTransformation transform;
	private boolean keepPaths = false;
	private boolean scaleMaxSpeed = false;

	private boolean slowButLowMemory = false;
	
	private final TransitSchedule schedule;
	private int transitRoutesCounter = 0;
	private Map<Long, Id<Link>> wayId2LinkId = new HashMap<>();
	
	/*package*/ final List<OsmFilter> hierarchyLayers = new ArrayList<OsmFilter>();

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84,
	 * 			which is often not optimal for MATSim.
	 */
	public CustomizedOsmNetworkReader(final Network network, final TransitSchedule schedule,
			final CoordinateTransformation transformation) {
		this(network, schedule, transformation, true);
	}

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, 
	 * 			which is often not optimal for MATSim.
	 * @param useHighwayDefaults Highway defaults are set to standard values, if true.
	 */
	public CustomizedOsmNetworkReader(final Network network, final TransitSchedule schedule,
			final CoordinateTransformation transformation, final boolean useHighwayDefaults) {
		this.network = network;
		this.transform = transformation;
		
		this.schedule = schedule;

		if (useHighwayDefaults) {
			log.info("Falling back to default values.");
			this.setHighwayDefaults(1, "motorway",      2, 100.0/3.6, 1.2, 2000, true);
			this.setHighwayDefaults(1, "motorway_link", 1,  60.0/3.6, 1.2, 1500, true);
			this.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 0.5, 2000);
			this.setHighwayDefaults(2, "trunk_link",    1,  60.0/3.6, 0.5, 1500);
			this.setHighwayDefaults(3, "primary",       1,  50.0/3.6, 0.5, 1500);
			this.setHighwayDefaults(3, "primary_link",  1,  50.0/3.6, 0.5, 1500);
			this.setHighwayDefaults(4, "secondary",     1,  50.0/3.6, 0.5, 1000);
			this.setHighwayDefaults(5, "tertiary",      1,  30.0/3.6, 0.8,  600);
			this.setHighwayDefaults(6, "minor",         1,  30.0/3.6, 0.8,  600);
			this.setHighwayDefaults(6, "unclassified",  1,  30.0/3.6, 0.8,  600);
			this.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 0.6,  600);
			this.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);
			//customization:
			this.setHighwayDefaults(7, "pedestrian", 1, 10/3.6, 1.0, 100);
			this.setHighwayDefaults(7, "footway", 1, 10/3.6, 1.0, 100);
			this.setHighwayDefaults(6, "service", 1,  30.0/3.6, 0.6,  300);
			
//			this.setHighwayDefaults(1, "light_rail", 1, 50 / 3.6, 0.6, 30);
//			this.setHighwayDefaults(1, "narrow_gauge", 1, 120 / 3.6, 1.0, 30);
			this.setRailwayDefaults(1, "rail", 1, 120 / 3.6, 1.0, 30);
//			this.setRailwayDefaults(1, "subway", 1, 40 / 3.6, 1.0, 30);
//			this.setRailwayDefaults(1, "tram", 1, 50 / 3.6, 0.6, 30);
			//railways:
			/*
			 * light_rail
			 * narrow_gauge
			 * rail
			 * subway
			 * tram
			 */
			
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
			parser = new OsmXmlParser(this.nodes, this.ways, this.reations, this.transform);
			parser.enableOptimization(1);
			if (stream != null) {
				parser.parse(new InputSource(stream));
			} else {
				parser.parse(osmFilename);
			}
			log.info("parsing osm file second time: loading required nodes and ways");
			parser.enableOptimization(2);
			if (stream != null) {
				parser.parse(new InputSource(stream));
			} else {
				parser.parse(osmFilename);
			}
			log.info("done loading data");
		} else {
			parser = new OsmXmlParser(this.nodes, this.ways, this.reations, this.transform);
			if (stream != null) {
				parser.parse(new InputSource(stream));
			} else {
				parser.parse(osmFilename);
			}
		}
		convert();
		log.info("= conversion statistics: ==========================");
		log.info("osm: # nodes read:                " + parser.nodeCounter.getCounter());
		log.info("osm: # ways read:                 " + parser.wayCounter.getCounter());
		log.info("osm: # relations read:            " + parser.relationCounter.getCounter());
		log.info("MATSim: # nodes created:          " + this.network.getNodes().size());
		log.info("MATSim: # links created:          " + this.network.getLinks().size());
		log.info("MATSim: # stops created:          " + this.schedule.getFacilities().size());
		log.info("MATSim: # transit lines created:  " + this.schedule.getTransitLines().size());
		log.info("MATSim: # transit routes created: " + this.transitRoutesCounter);

		if (this.unknownHighways.size() > 0) {
			log.info("The following highway-types had no defaults set and were thus NOT converted:");
			for (String highwayType : this.unknownHighways) {
				log.info("- \"" + highwayType + "\"");
			}
		}
		log.info("= end of conversion statistics ====================");
	}
	
	public void setRailwayDefaults(final int hierarchy , final String highwayType, final double lanesPerDirection, final double freespeed, final double freespeedFactor, final double laneCapacity_vehPerHour) {
		this.railwayDefaults.put(highwayType, new OsmHighwayDefaults(hierarchy, lanesPerDirection, freespeed, freespeedFactor, laneCapacity_vehPerHour, false));
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
	public void setHierarchyLayer(final double coordNWLat, final double coordNWLon, final double coordSELat,
			final double coordSELon, final int hierarchy) {
		this.hierarchyLayers.add(new OsmFilter(this.transform.transform(new Coord(coordNWLon, coordNWLat)),
				this.transform.transform(new Coord(coordSELon, coordSELat)), hierarchy));
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
		
		Iterator<Entry<Long, OsmRelation>> rIt = this.reations.entrySet().iterator();
		while(rIt.hasNext()){
			
			Entry<Long, OsmRelation> entry = rIt.next();
			for(Long wayId : entry.getValue().ways){
				if(this.ways.get(wayId) == null){
					rIt.remove();
					break;
				}
			}
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
			} else if(railway != null && this.railwayDefaults.containsKey(railway)){
				
				// check to which level a way belongs
				way.hierarchy = this.railwayDefaults.get(railway).hierarchy;

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
					node.used = false;
				}
			}
			// verify we did not mark nodes as unused that build a loop
			for (OsmWay way : this.ways.values()) {
				String highway = way.tags.get(TAG_HIGHWAY);
				String railway = way.tags.get(TAG_RAILWAY);
				if (((highway != null) && (this.highwayDefaults.containsKey(highway))) ||
						(railway != null && this.railwayDefaults.containsKey(railway))) {
					int prevRealNodeIndex = 0;
					OsmNode prevRealNode = this.nodes.get(way.nodes.get(prevRealNodeIndex));

					for (int i = 1; i < way.nodes.size(); i++) {
						OsmNode node = this.nodes.get(way.nodes.get(i));
						if (node.used) {
							if (prevRealNode == node) {
								/* We detected a loop between to "real" nodes.
								 * Set some nodes between the start/end-loop-node to "used" again.
								 * But don't set all of them to "used", as we still want to do some
								 *  network-thinning.
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
			String railway = way.tags.get(TAG_RAILWAY);
			if (highway != null || railway != null) {
				OsmNode fromNode = this.nodes.get(way.nodes.get(0));
				double length = 0.0;
				OsmNode lastToNode = fromNode;
				if (fromNode.used) {
					for (int i = 1, n = way.nodes.size(); i < n; i++) {
						OsmNode toNode = this.nodes.get(way.nodes.get(i));
						if (toNode != lastToNode) {
							length += CoordUtils.calcDistance(lastToNode.coord, toNode.coord);
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
		
//		for(OsmNode node : this.nodes.values()){
//			
//			String railwayType = node.tags.get(TAG_RAILWAY);
//			if(railwayType != null){
//				createTransitStopFacility(this.schedule, node);
//			}
//			
//		}
//		
//		for(OsmRelation relation : this.reations.values()){
//			createTransitSchedule(this.schedule, relation);
//		}

		// free up memory
		this.nodes.clear();
		this.ways.clear();
	}
	
	private void createTransitStopFacility(final TransitSchedule schedule, OsmNode node){
		
//		if("station".equals(node.tags.get(TAG_RAILWAY)) || "halt".equals(node.tags.get(TAG_RAILWAY))
//				|| "platform".equals(node.tags.get(TAG_RAILWAY))){
			
			String origId = Long.toString(node.id);
			Id<TransitStopFacility> id = Id.create(origId, TransitStopFacility.class);
			String name = node.tags.get(TAG_NAME);
			
			TransitStopFacility facility = schedule.getFactory().createTransitStopFacility(id, node.coord, false);
			facility.setName(name);
			facility.setStopPostAreaId(origId);
			schedule.addStopFacility(facility);
			
//		}
		
	}
	
	private void createTransitSchedule(final TransitSchedule schedule, final OsmRelation relation){
		
		String routeType = relation.tags.get(TAG_TYPE);
		
		if("route_master".equals(routeType)){
			
			//it's a transit line
			TransitLine line = schedule.getFactory().createTransitLine(Id.create(relation.id, TransitLine.class));
			schedule.addTransitLine(line);
			
		} else if("route".equals(routeType)){
			
			//it's a transit route
			
			String refId = relation.tags.get(TAG_REF);
			
			if(refId == null){
				refId = Integer.toString(MatsimRandom.getLocalInstance().nextInt(10000));
			}
			
			Id<TransitLine> transitLineRefId = Id.create(refId, TransitLine.class);
			TransitLine line = schedule.getTransitLines().containsKey(transitLineRefId) ? schedule.getTransitLines().get(transitLineRefId) :
					schedule.getFactory().createTransitLine(Id.create(transitLineRefId, TransitLine.class));
			
			if(!schedule.getTransitLines().containsKey(transitLineRefId)){
				
				schedule.addTransitLine(line);
				
			}
			
			ArrayList<TransitRouteStop> stops = new ArrayList<>();
			
			for(Long nodeId : relation.nodes) {
				
				if(this.nodes.get(nodeId).tags.get(TAG_RAILWAY) != null){
					
					TransitStopFacility facility = schedule.getFacilities().get(Id.create(nodeId, TransitStopFacility.class));
					TransitRouteStop stop = schedule.getFactory().createTransitRouteStop(facility, 0.0, 0.0);
					stops.add(stop);
					
				}
				
			}
			
			ArrayList<Id<Link>> linkIds = new ArrayList<>();
			for(Long wayId : relation.ways){

				linkIds.add(this.wayId2LinkId.get(wayId));
				
			}
			
			NetworkRoute networkRoute = linkIds.size() > 1 ? 
					RouteUtils.createNetworkRoute(linkIds, this.network) : null;
			
			TransitRoute route = schedule.getFactory().createTransitRoute(Id.create(refId + "==to==" + relation.tags.get(TAG_TO), TransitRoute.class), networkRoute, stops, "train");
			if(route.getRoute() != null && route.getStops().size() > 0){
				line.addRoute(route);
			}
			this.transitRoutesCounter++;
			
		}
		
	}

	private void createLink(final Network network, final OsmWay way, final OsmNode fromNode,
			final OsmNode toNode, final double length) {
		
		String highway = way.tags.get(TAG_HIGHWAY);
		String railway = way.tags.get(TAG_RAILWAY);

        if ("no".equals(way.tags.get(TAG_ACCESS))) {
             return;
        }
        
		// load defaults
		OsmHighwayDefaults defaults = this.highwayDefaults.get(highway) != null ?
				this.highwayDefaults.get(highway) : this.railwayDefaults.get(railway);
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

        // In case trunks, primary and secondary roads are marked as oneway,
        // the default number of lanes should be two instead of one.
		if(highway != null){
			
			if(highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") ||
	        		highway.equalsIgnoreCase("secondary")){
	            if((oneway || onewayReverse) && nofLanes == 1.0){
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
		String tracksTag = way.tags.get(TAG_TRACKS);
		if (lanesTag != null) {
			try {
				double totalNofLanes = Double.parseDouble(lanesTag);
				if (totalNofLanes > 0) {
					nofLanes = totalNofLanes;

					//By default, the OSM lanes tag specifies the total number of lanes in both directions.
					//So if the road is not oneway (onewayReverse), let's distribute them between
					//both directions
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
		} else if(tracksTag != null) {
			
			//the number of tracks is directly applied to the link
			//all railway links should be oneway and thus we do not need
			//to divide here
			double totalNofTracks = Double.parseDouble(tracksTag);
			if(totalNofTracks > 0){
				nofLanes = totalNofTracks;
			}
			
		}
		
		Set<String> allowedModes = new HashSet<>();
		
		if(way.tags.get(TAG_HIGHWAY) == null ){
			
			allowedModes.add(TransportMode.pt);
			
		} else {
			
			if(way.tags.get(TAG_HIGHWAY).equals("footway") || way.tags.get(TAG_HIGHWAY).equals("pedestrian")){
				
				allowedModes.add(TransportMode.walk);
				
			} else {
				
				allowedModes.add(TransportMode.car);
				allowedModes.add(TransportMode.pt);
				
				if(freespeed <= 50 / 3.6){
					
					allowedModes.add(TransportMode.walk);
					allowedModes.add(TransportMode.bike);
					
				}
				
			}
			
		}
		
		String cyclewayTag = way.tags.get(TAG_CYCLEWAY);
		String bicycleTag = way.tags.get(TAG_BICYCLE);
		String busTag = way.tags.get(TAG_BUS);
		
		if(cyclewayTag != null || bicycleTag != null){
			allowedModes.add(TransportMode.bike);
		}
		if(busTag != null){
			allowedModes.add(TransportMode.pt);
		}

		// create the link(s)
		double capacity = nofLanes * laneCapacity;

		if (this.scaleMaxSpeed) {
			freespeed = freespeed * freespeedFactor;
		}

		boolean networkModes = false;
		if(allowedModes.contains(TransportMode.car) || allowedModes.contains(TransportMode.pt)){
			networkModes = true;
		}
		
		// only create link, if both nodes were found, node could be null, since nodes outside a layer
		//were dropped
		Id<Node> fromId = Id.create(fromNode.id, Node.class);
		Id<Node> toId = Id.create(toNode.id, Node.class);
		if(network.getNodes().get(fromId) != null && network.getNodes().get(toId) != null &&
				networkModes){
			String origId = Long.toString(way.id);

			if (!onewayReverse) {
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class),
						network.getNodes().get(fromId), network.getNodes().get(toId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(allowedModes);
				if (l instanceof LinkImpl) {
					((LinkImpl) l).setOrigId(origId);
				}
				network.addLink(l);
				this.wayId2LinkId.put(way.id, l.getId());
				this.id++;
			}
			if (!oneway) {
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class),
						network.getNodes().get(toId), network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacity);
				l.setNumberOfLanes(nofLanes);
				l.setAllowedModes(allowedModes);
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
		public boolean used = false;
		public int ways = 0;
		public final Coord coord;
		public final Map<String, String> tags = new HashMap<String, String>(4);

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
	
	private static class OsmRelation {
		
		public final long id;
		public final List<Long> nodes = new ArrayList<Long>();
		public final List<Long> ways = new ArrayList<Long>();
		public final List<Long> relations = new ArrayList<Long>();
		public final Map<String, String> tags = new HashMap<String, String>(4);
		
		public OsmRelation(final long id){
			
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

		public OsmHighwayDefaults(final int hierarchy, final double lanesPerDirection,
				final double freespeed, final double freespeedFactor, final double laneCapacity,
				final boolean oneway) {
			this.hierarchy = hierarchy;
			this.lanesPerDirection = lanesPerDirection;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
		}
	}
	
	private class OsmXmlParser extends MatsimXmlParser {

		private OsmNode currentNode = null;
		private OsmWay currentWay = null;
		private OsmRelation currentRelation = null;
		private final Map<Long, OsmNode> nodes;
		private final Map<Long, OsmWay> ways;
		private final Map<Long, OsmRelation> relations;
		private final Stack<OsmRelation> activeRelations = new Stack<OsmRelation>();
		/*package*/ final Counter nodeCounter = new Counter("node ");
		/*package*/ final Counter wayCounter = new Counter("way ");
		/*package*/ final Counter relationCounter = new Counter("relation ");
		private final CoordinateTransformation transform;
		private boolean loadNodes = true;
		private boolean loadWays = true;
		private boolean mergeNodes = false;
		private boolean collectNodes = false;

		public OsmXmlParser(final Map<Long, OsmNode> nodes, final Map<Long, OsmWay> ways, final Map<Long, OsmRelation> relations,
				final CoordinateTransformation transform) {
			super();
			this.nodes = nodes;
			this.ways = ways;
			this.relations = relations;
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
		protected void parse(InputSource input) throws UncheckedIOException {
			super.parse(input);
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			if ("node".equals(name)) {
				if (this.loadNodes) {
					Long id = Long.valueOf(atts.getValue("id"));
					double lat = Double.parseDouble(atts.getValue("lat"));
					double lon = Double.parseDouble(atts.getValue("lon"));
					OsmNode node = new OsmNode(id, this.transform.transform(new Coord(lon, lat)));
					this.nodes.put(id, node);
					this.nodeCounter.incCounter();
					this.currentNode = node;
					
				} else if (this.mergeNodes) {
					OsmNode node = this.nodes.get(Long.valueOf(atts.getValue("id")));
					if (node != null) {
						double lat = Double.parseDouble(atts.getValue("lat"));
						double lon = Double.parseDouble(atts.getValue("lon"));
						Coord c = this.transform.transform(new Coord(lon, lat));
						node.coord.setXY(c.getX(), c.getY());
						this.nodeCounter.incCounter();
						this.currentNode = node;
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
				} else if(this.currentRelation != null){
					
					String key = StringCache.get(atts.getValue("k"));
					for(String tag : ALL_TAGS) {
						
						if(tag.equals(key)){
							this.currentRelation.tags.put(key, StringCache.get(atts.getValue("v")));
						}
						
					}
					
				} else if(this.currentNode != null){
					
					String key = StringCache.get(atts.getValue("k"));
					for(String tag : ALL_TAGS) {
						
						if(tag.equals(key)){
							this.currentNode.tags.put(key, StringCache.get(atts.getValue("v")));
						}
						
					}
					
				}
			} else if ("relation".equals(name)){
				
				if(this.currentRelation != null){
				
					this.activeRelations.add(this.currentRelation);
					
				}
				
				this.currentRelation = new OsmRelation(Long.parseLong(atts.getValue("id")));
				
			} else if("member".equals(name)){
				
				String type = atts.getValue("type");
				long refId = Long.parseLong(atts.getValue("ref"));
				
				if("node".equals(type)){
					
					this.currentRelation.nodes.add(refId);
					
				} else if("way".equals(type)){
					
					this.currentRelation.ways.add(refId);
					
				} else if("relation".equals(type)){
					
					this.currentRelation.relations.add(refId);
					
				}
				
			}
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			if ("way".equals(name)) {
				if (!this.currentWay.nodes.isEmpty()) {
					boolean used = false;
					OsmHighwayDefaults osmHighwayDefaults = CustomizedOsmNetworkReader.this.highwayDefaults.get(this.currentWay.tags.get(TAG_HIGHWAY));
					if(osmHighwayDefaults == null){
						osmHighwayDefaults = CustomizedOsmNetworkReader.this.railwayDefaults.get(this.currentWay.tags.get(TAG_RAILWAY));
					}
					if (osmHighwayDefaults != null) {
						int hierarchy = osmHighwayDefaults.hierarchy;
						this.currentWay.hierarchy = hierarchy;
						if (CustomizedOsmNetworkReader.this.hierarchyLayers.isEmpty()) {
							used = true;
						}
						if (this.collectNodes) {
							used = true;
						} else {
							for (OsmFilter osmFilter : CustomizedOsmNetworkReader.this.hierarchyLayers) {
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
			} else if("relation".equals(name)){
				if(!this.currentRelation.nodes.isEmpty() || !this.currentRelation.ways.isEmpty() || !this.currentRelation.relations.isEmpty()){
					
					this.relations.put(this.currentRelation.id, this.currentRelation);
					this.relationCounter.incCounter();
					this.currentRelation = null;
					
				}
			} else if("node".equals(name)){
				this.currentNode = null;
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

}
