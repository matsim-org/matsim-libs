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

package org.matsim.core.utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

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
 * @deprecated The OsmNetworkReader has been moved to the osm-contrib. Use org.matsim.contribs.osm.networkReader.SupersonicOsmNetworkReader instead.
 *
 * @author mrieser, aneumann
 */
@Deprecated
public class OsmNetworkReader implements MatsimSomeReader {

	private final static Logger log = LogManager.getLogger(OsmNetworkReader.class);

	private final static String TAG_LANES = "lanes";
	private final static String TAG_LANES_FORWARD = "lanes:forward";
	private final static String TAG_LANES_BACKWARD = "lanes:backward";
	protected final static String TAG_HIGHWAY = "highway";
	private final static String TAG_MAXSPEED = "maxspeed";
	protected final static String TAG_JUNCTION = "junction";
    protected final static String TAG_ONEWAY = "oneway";
    private final static String TAG_ACCESS = "access";
	private static final List<String> allTags = new LinkedList<>(Arrays.asList(TAG_LANES, TAG_LANES_FORWARD,
			TAG_LANES_BACKWARD, TAG_HIGHWAY, TAG_MAXSPEED, TAG_JUNCTION, TAG_ONEWAY, TAG_ACCESS));

	protected final Map<Long, OsmNode> nodes = new HashMap<>();
	protected final Map<Long, OsmWay> ways = new HashMap<>();
	private final Set<String> unknownHighways = new HashSet<>();
	private final Set<String> unknownMaxspeedTags = new HashSet<>();
	private final Set<String> unknownLanesTags = new HashSet<>();
	protected long id = 0;
	protected final Map<String, OsmHighwayDefaults> highwayDefaults = new HashMap<>();
	protected final Network network;
	protected final CoordinateTransformation transform;
	private boolean keepPaths = false;
	private boolean scaleMaxSpeed = false;

	private boolean slowButLowMemory = false;

	private boolean useVspAdjustments = false; // Adjustments discussed on 2018-04-30, kn,ik,dz. apr'18 (Might become default after testing)

	/*package*/ final List<OsmFilter> hierarchyLayers = new ArrayList<>();

	// nodes that are definitely to be kept (e.g. for counts later)
	private Set<Long> nodeIDsToKeep = null;

	private OsmXmlParser parser;


	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 */
	public OsmNetworkReader(final Network network, final CoordinateTransformation transformation) {
		this(network, transformation, true);
	}

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 * @param useHighwayDefaults Highway defaults are set to standard values, if true.
	 */
	public OsmNetworkReader(final Network network, final CoordinateTransformation transformation, final boolean useHighwayDefaults) {
		this(network, transformation, useHighwayDefaults, false);
	}

	/**
	 * Creates a new Reader to convert OSM data into a MATSim network.
	 *
	 * After discussion, we (kn,ik,dz) introduced some adjustments to links regarding speeds and capacities
	 * that aim to represent different travel times in urban vs. rural areas better, esp. taking into account
	 * intersections/traffic lights and their implications on speeds and capacities in urban areas.
	 *
	 * @param network An empty network where the converted OSM data will be stored.
	 * @param transformation A coordinate transformation to be used. OSM-data comes as WGS84, which is often not optimal for MATSim.
	 * @param useHighwayDefaults Highway defaults are set to standard values, if true.
	 * @param useVspAdjustments Highway defaults are set to standard VSP values, if true.
	 *
	 */
	public OsmNetworkReader(final Network network, final CoordinateTransformation transformation, final boolean useHighwayDefaults, final boolean useVspAdjustments) {
		this.network = network;
		this.transform = transformation;
		this.useVspAdjustments = useVspAdjustments;

		if (useHighwayDefaults) {
			log.info("Falling back to default values.");
			this.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
			this.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			this.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
			this.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
			if (useVspAdjustments) {
				this.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1000);
				this.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1000);
			} else {
				this.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
				this.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
			}

//			this.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
//			this.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
//			this.setHighwayDefaults(6, "minor",         1,  45.0/3.6, 1.0,  600);
//			this.setHighwayDefaults(6, "unclassified",  1,  45.0/3.6, 1.0,  600);
//			this.setHighwayDefaults(6, "residential",   1,  30.0/3.6, 1.0,  600);
//			this.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);

			// Setting the following to considerably smaller values, since there are often traffic signals/non-prio intersections.
			// If someone does a systematic study, please report.  kai, jul'16
			//
			// We revised the below street types (removed "minor" and put "living_street", "residential", and "unclassified" into
			// different hierarchy layers), trying to make this more reasonable based on the
			// <a href="OMS Wiki">http://wiki.openstreetmap.org/wiki/DE:Key:highway</a>, ts/aa/dz, oct'17
			if (useVspAdjustments) {
				this.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 1.0, 800);
				this.setHighwayDefaults(4, "secondary_link",     1,  30.0/3.6, 1.0, 800);
			} else {
				this.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 1.0, 1000);
				this.setHighwayDefaults(4, "secondary_link",     1,  30.0/3.6, 1.0, 1000);
			}
			this.setHighwayDefaults(5, "tertiary",      1,  25.0/3.6, 1.0,  600);
			this.setHighwayDefaults(5, "tertiary_link",      1,  25.0/3.6, 1.0,  600);
			this.setHighwayDefaults(6, "unclassified",  1,  15.0/3.6, 1.0,  600);
			this.setHighwayDefaults(7, "residential",   1,  15.0/3.6, 1.0,  600);
			this.setHighwayDefaults(8, "living_street", 1,  10.0/3.6, 1.0,  300);
			// changing the speed values failed the evacuation ScenarioGenerator test because of a different network -- DESPITE
			// the fact that all the speed values are reset to some other value there.  No idea what happens there. kai, jul'16
		}

		this.parser = new OsmXmlParser(this.nodes, this.ways, this.transform);
	}

	/**
	 * Parses the given osm file and creates a MATSim network from the data.
	 *
	 * @param osmFilename
	 * @throws UncheckedIOException
	 */
	public final void parse(final String osmFilename) {
		parse(osmFilename, null);
	}

	/**
	 * Parses the given input stream and creates a MATSim network from the data.
	 * We need to pass a supplier as in some cases, the stream could be parsed multiple times.
	 *
	 * @param streamSupplier
	 * @throws UncheckedIOException
	 */
	public final void parse(final Supplier<InputStream> streamSupplier) throws UncheckedIOException {
		parse(null, streamSupplier);
	}

	/**
	 * Either osmFilename or streamSupplier must be <code>null</code>, but not both.
	 *
	 * @param osmFilename
	 * @param streamSupplier
	 * @throws UncheckedIOException
	 */
	private void parse(final String osmFilename, final Supplier<InputStream> streamSupplier) throws UncheckedIOException {
		if(this.hierarchyLayers.isEmpty()){
			log.warn("No hierarchy layer specified. Will convert every highway specified by setHighwayDefaults.");
		}

		if (this.slowButLowMemory) {
			log.info("parsing osm file first time: identifying nodes used by ways");
			this.parser.enableOptimization(1);
			if (streamSupplier != null) {
				try (InputStream is = streamSupplier.get()) {
					this.parser.parse(is);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				this.parser.readFile(osmFilename);
			}
			log.info("parsing osm file second time: loading required nodes and ways");
			this.parser.enableOptimization(2);
			if (streamSupplier != null) {
				try (InputStream is = streamSupplier.get()) {
					this.parser.parse(is);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				this.parser.readFile(osmFilename);
			}
			log.info("done loading data");
		} else {
			if (streamSupplier!= null) {
				try (InputStream is = streamSupplier.get()) {
					this.parser.parse(is);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			} else {
				this.parser.readFile(osmFilename);
			}
			log.info("done loading data");
		}
		convert();
		log.info("= conversion statistics: ==========================");
		log.info("osm: # nodes read:       " + this.parser.nodeCounter.getCounter());
		log.info("osm: # ways read:        " + this.parser.wayCounter.getCounter());
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
	 * Set a parser for OSM XML data.
	 * If no parser is set, the default parser from this class (OsmNetworkReader.OsmXmlParser) is used.
	 */
	public void setParser(OsmXmlParser parser) {
		this.parser = parser;
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
	public final void setHighwayDefaults(final int hierarchy , final String highwayType, final double lanesPerDirection, final double freespeed, final double freespeedFactor, final double laneCapacity_vehPerHour) {
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
	public final void setHighwayDefaults(final int hierarchy, final String highwayType, final double lanesPerDirection, final double freespeed,
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
	public final void setKeepPaths(final boolean keepPaths) {
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
	public final void setScaleMaxSpeed(final boolean scaleMaxSpeed) {
		this.scaleMaxSpeed = scaleMaxSpeed;
	}

	/**
	 * Defines a new hierarchy layer by specifying a rectangle and the hierarchy level to which highways will be converted.
	 *
	 * @param coordNWNorthing The latitude of the north western corner of the rectangle.
	 * @param coordNWEasting The longitude of the north western corner of the rectangle.
	 * @param coordSENorthing The latitude of the south eastern corner of the rectangle.
	 * @param coordSEEasting The longitude of the south eastern corner of the rectangle.
	 * @param hierarchy Layer specifying the hierarchy of the layers starting with 1 as the top layer.
	 */
	public final void setHierarchyLayer(final double coordNWNorthing, final double coordNWEasting, final double coordSENorthing, final double coordSEEasting, final int hierarchy) {
		this.hierarchyLayers.add(new OsmFilterImpl(this.transform.transform(new Coord(coordNWEasting, coordNWNorthing)), this.transform.transform(new Coord(coordSEEasting, coordSENorthing)), hierarchy));
	}
	public final void setHierarchyLayer(final int hierarchy) {
		this.hierarchyLayers.add(new GeographicallyNonrestrictingOsmFilterImpl(hierarchy));
	}

	/**
	 * Adds a new filter to hierarchy layer.
	 * @param osmFilter
	 */
	public final void addOsmFilter(final OsmFilter osmFilter) {
		this.hierarchyLayers.add(osmFilter);
	}

	/**
	 * By default, this converter caches a lot of data internally to speed up the network generation.
	 * This can lead to OutOfMemoryExceptions when converting huge osm files. By enabling this
	 * memory optimization, the converter tries to reduce its memory usage, but will run slower.
	 *
	 * @param memoryEnabled
	 */
	public final void setMemoryOptimization(final boolean memoryEnabled) {
		this.slowButLowMemory = memoryEnabled;
	}

	public final void setNodeIDsToKeep(Set<Long> nodeIDsToKeep){
		if(nodeIDsToKeep != null && !nodeIDsToKeep.isEmpty()){
			this.nodeIDsToKeep = nodeIDsToKeep;
		}
	}

	protected void addWayTags(List<String> wayTagsToAdd) {
		allTags.addAll(wayTagsToAdd);
	}

	private void convert() {
		this.network.setCapacityPeriod(3600);

		preprocessOsmData();

		simplifyOsmData();

		createMatsimData();

		// free up memory
		this.nodes.clear();
		this.ways.clear();
	}

	/**
	 * This method is called before the network (still based on osm data) gets simplified and before the matsim network is created.
	 *
	 * It cleans the OSM data (removes ways with nodes that does not exist)
	 * and prepares data for simplification (collects information about all ways a node is located on; marks end nodes of ways).
	 *
	 * Override/Extend this method to add additional preprocessing of OSM data (see e.g. the signals and lanes reader).
	 */
	protected void preprocessOsmData() {
		log.info("Remove ways that have at least one node that was not read previously ...");
		// yy I _think_ this is what it does.  kai, may'16
		Iterator<Entry<Long, OsmWay>> it = this.ways.entrySet().iterator();
		int counter = 0;
		while (it.hasNext()) {
			Entry<Long, OsmWay> entry = it.next();
			for (Long nodeId : entry.getValue().nodes) {
				if (this.nodes.get(nodeId) == null) {
					it.remove();
					break;
				}
			}
		}
		log.info("... done removing " + counter + "ways that have at least one node that was not read previously.");

		log.info("Fill ways and nodes with additional information: the hierarchy layer for ways, end points of ways...");
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get(TAG_HIGHWAY);
			if ((highway != null) && (this.highwayDefaults.containsKey(highway))) {
				// check to which level a way belongs
				way.hierarchy = this.highwayDefaults.get(highway).hierarchy;

				// first and last node are marked as endpoints, so they are kept in all cases
				this.nodes.get(way.nodes.get(0)).endPoint = true;
				this.nodes.get(way.nodes.get(way.nodes.size() - 1)).endPoint = true;

				// save all ways a node is located on in a special map
				for (Long nodeId : way.nodes) {
					this.nodes.get(nodeId).ways.put(way.id, way);
				}
			}
		}
		log.info("... done filling ways and nodes with additional information.");
	}

	/**
	 * This method marks node that should be kept based on the highway type, the
	 * number of ways they are located at, whether they are endpoints of ways, and
	 * whether they belong to a certain group of nodes that should be kept anyway
	 * (e.g. counting stations).
	 *
	 * Override/Extend this method when further simplification of OSM data is
	 * necessary in your reader.
	 */
	protected void simplifyOsmData() {
		log.info("Mark OSM nodes that shoud be kept...");
		for (OsmNode node : this.nodes.values()) {
			// check whether the node belongs to a way that fulfills the hierarchyLayers (i.e. is 'big' enough)
			wayLoop: for (OsmWay way : node.ways.values()) {
				String highway = way.tags.get(TAG_HIGHWAY);
				if ((highway != null) && (this.highwayDefaults.containsKey(highway))) {
					if (this.hierarchyLayers.isEmpty()) {
						node.used = true;
						break;
					} else {
						for (OsmFilter osmFilter : this.hierarchyLayers) {
							if (osmFilter.coordInFilter(node.coord, way.hierarchy)) {
								node.used = true;
								break wayLoop;
							}
						}
					}
				}
			}

			// mark nodes that can be removed/simplified (no endpoint, no intersections)
			if (canNodeBeRemoved(node)) {
				node.used = false;
			}

			// keep special nodes (e.g. nodes with counts data), also when they don't fulfill hierarchyLayers (e.g. are located on smaller ways)
			if ((nodeIDsToKeep != null) && nodeIDsToKeep.contains(node.id)) {
				node.used = true;
			}
		}
		log.info("... done marking OSM nodes that shoud be kept.");

		log.info("Verify we did not mark nodes that build a loop ...");
		for (OsmWay way : this.ways.values()) {
			String highway = way.tags.get(TAG_HIGHWAY);
			if ((highway != null) && (this.highwayDefaults.containsKey(highway))) {
				int prevRealNodeIndex = 0;
				OsmNode prevRealNode = this.nodes.get(way.nodes.get(prevRealNodeIndex));

				for (int i = 1; i < way.nodes.size(); i++) {
					OsmNode node = this.nodes.get(way.nodes.get(i));
					if (node.used) {
						if (prevRealNode == node) {
							/* We detected a loop between two "real" nodes.
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
		log.info("... done verifying that we did not mark nodes that build a loop.");
	}

	/**
	 * This method checks whether the node is an intersection node or an end node of a way and, therefore, can not be removed/simplified.
	 * Override/extend this when you want to keep more or other nodes, e.g. signalized nodes.
	 *
	 * @return whether the node can be simplified (true) or not (false)
	 */
	protected boolean canNodeBeRemoved(OsmNode node) {
		return !this.keepPaths && node.ways.size()<=1 && !node.endPoint;
	}

	/**
	 * This method creates nodes and links for a MATSim scenario.
	 * Override/extend this when additional data, e.g. signals, should be created.
	 */
	protected void createMatsimData() {
		log.info("Create the required nodes ...") ;
		for (OsmNode node : this.nodes.values()) {
			if (node.used) {
				Node nn = this.network.getFactory().createNode(Id.create(node.id, Node.class), node.coord);
				setOrModifyNodeAttributes(nn, node);
				this.network.addNode(nn);
			}
		}
		log.info("... done creating the required nodes.");

		log.info( "Create the links ...") ;
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
		log.info("... done creating the links.");
	}

	private void createLink(final Network network, final OsmWay way, final OsmNode fromNode, final OsmNode toNode,
			final double length) {
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

		double nofLanesForward = defaults.lanesPerDirection;
		double nofLanesBackward = defaults.lanesPerDirection;
		double laneCapacity = defaults.laneCapacity;
		double freespeed = defaults.freespeed;
		double freespeedFactor = defaults.freespeedFactor;

		boolean oneway = isOneway(way);
		boolean onewayReverse = isOnewayReverse(way);

        // In case trunks, primary and secondary roads are marked as oneway,
        // the default number of lanes should be two instead of one.
        if(highway.equalsIgnoreCase("trunk") || highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary")){
			if (oneway && nofLanesForward == 1.0) {
				nofLanesForward = 2.0;
			} else if(onewayReverse && nofLanesBackward == 1.0){
				nofLanesBackward = 2.0;
			}
		}

		String maxspeedTag = way.tags.get(TAG_MAXSPEED);
		if (maxspeedTag != null) {
			try {
				if(maxspeedTag.endsWith("mph")) {
					freespeed = Double.parseDouble(maxspeedTag.replace("mph", "").trim()) * 1.609344 / 3.6; // convert mph to m/s
				} else {
					freespeed = Double.parseDouble(maxspeedTag) / 3.6; // convert km/h to m/s
				}
				if (useVspAdjustments) {
					// For links whose maxspeed is known, we assume that those with maxspeed lower than or equl to 51km/h are 'urban' links
					// For these, we reduced speeds to 50% to account for traffic lights/intersections now, kn,ik,dz, apr'18
					if (freespeed <= 51./3.6) {
						freespeed = freespeed/2;
					}
				}
			} catch (NumberFormatException e) {
				if (!this.unknownMaxspeedTags.contains(maxspeedTag)) {
					this.unknownMaxspeedTags.add(maxspeedTag);
					log.warn("Could not parse maxspeed tag:" + e.getMessage() + ". Ignoring it.");
				}
			}
		} else {
			if (useVspAdjustments) {
				// For links whose maxspeed is unknown, we assume that links with a length (after removal of purely geometric nodes) of more
				// than 300m are 'rural' others 'urban'. 'Rural' speed is 100km/h, 'urban' linearly increasing from 10km/h at zero length to
				// the 'rural' speed for links with a length of 300m. kn,ik,dz, apr'18
				if(highway.equalsIgnoreCase("primary") || highway.equalsIgnoreCase("secondary") || highway.equalsIgnoreCase("tertiary")
						|| highway.equalsIgnoreCase("primary_link") || highway.equalsIgnoreCase("secondary_link") || highway.equalsIgnoreCase("tertiary_link")) {
							if (length > 300.) {
						freespeed = 80. / 3.6; // Might be different (but also not too much different) in other countries
					} else {
						freespeed = (10. + 70./300 * length) / 3.6;
					}
				}
			}
		}

		if (useVspAdjustments) {
			// Adjustments that KN had been using for a while: For short links, often roundabouts or short u-turns, etc.
			if (length < 100 ) {
				laneCapacity = 2 * laneCapacity;
			}
		}

		// check tag "lanes"
		String lanesTag = way.tags.get(TAG_LANES);
		String lanesForwardTag = way.tags.get(TAG_LANES_FORWARD);
		String lanesBackwardTag = way.tags.get(TAG_LANES_BACKWARD);
		try {
			/* If the tag "lanes:forward" or "lanes:backward" is set, use them.
		 	 * If not, give each direction half of the total number of lanes (except it is a oneway street)
		 	 * fzwick,nkuehnel, apr'17
			 * If only one of them is set and total number of lanes is known (as it is often the case),
			 * calculate missing value as the difference... tthunig, oct'17
			 */
			if (lanesForwardTag != null && lanesBackwardTag != null){
				nofLanesForward = Double.parseDouble(lanesForwardTag);
				nofLanesBackward = Double.parseDouble(lanesBackwardTag);
				// done
			} else if (oneway) {
				nofLanesBackward = 0;
				if (lanesForwardTag != null)
					nofLanesForward = Double.parseDouble(lanesForwardTag);
				else if (lanesTag != null)
					nofLanesForward = Double.parseDouble(lanesTag);
				// else keep default
			} else if (onewayReverse) {
				nofLanesForward = 0;
				if (lanesBackwardTag != null)
					nofLanesBackward = Double.parseDouble(lanesBackwardTag);
				else if (lanesTag != null)
					nofLanesBackward = Double.parseDouble(lanesTag);
				// else keep default
			} else if (lanesForwardTag != null) {
				// i.e. lanesBackwardTag is null
				nofLanesForward = Double.parseDouble(lanesForwardTag);
				if (lanesTag != null)
					nofLanesBackward = Double.parseDouble(lanesTag) - nofLanesForward;
				// else keep default
			}
			else if (lanesBackwardTag != null) {
				// i.e. lanesForwardTag is null
				nofLanesBackward = Double.parseDouble(lanesBackwardTag);
				if (lanesTag != null)
					nofLanesForward = Double.parseDouble(lanesTag) - nofLanesBackward;
				// else keep default
			} else { // i.e. lanesForwardTag and lanesBackwardTag are null. no oneway
				if (lanesTag != null) {
					/* By default, the OSM lanes tag specifies the total number of lanes in both direction.
					 * So, let's distribute them between both directions. michalm, jan'16
					 */
					nofLanesForward = Double.parseDouble(lanesTag) / 2;
					nofLanesBackward = nofLanesForward;
				}
				// else keep default
			}
		} catch (Exception e) {
			if (!this.unknownLanesTags.contains(lanesTag)) {
				this.unknownLanesTags.add(lanesTag);
				log.warn("Could not parse lanes tag:" + e.getMessage() + ". Ignoring it.");
			}
		}

		double capacityForward = nofLanesForward * laneCapacity;
		double capacityBackward = nofLanesBackward * laneCapacity;

		if (this.scaleMaxSpeed) {
			freespeed = freespeed * freespeedFactor;
			if (useVspAdjustments) {
				throw new RuntimeException("Max speed scaling and VSP adjustments used at the same time. Both reduce speeds. It is most likely "
						+ "unintended to use them both at the same time. ik,dz, spr'18");
			}
		}

		// only create link, if both nodes were found, node could be null, since nodes outside a layer were dropped
		Id<Node> fromId = Id.create(fromNode.id, Node.class);
		Id<Node> toId = Id.create(toNode.id, Node.class);
		if(network.getNodes().get(fromId) != null && network.getNodes().get(toId) != null){
			String origId = Long.toString(way.id);

			// Forward direction (in relation to the direction of the OSM way object)
			if (forwardDirectionExists(way)) {
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(fromId), network.getNodes().get(toId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacityForward);
				l.setNumberOfLanes(nofLanesForward);
				NetworkUtils.setOrigId(l, origId);
				NetworkUtils.setType(l, highway);
				setOrModifyLinkAttributes(l, way, true);
				network.addLink(l);
				this.id++;
			}
			// Backward/reverse direction (in relation to the direction of the OSM way object)
			if (reverseDirectionExists(way)) {
				Link l = network.getFactory().createLink(Id.create(this.id, Link.class), network.getNodes().get(toId), network.getNodes().get(fromId));
				l.setLength(length);
				l.setFreespeed(freespeed);
				l.setCapacity(capacityBackward);
				l.setNumberOfLanes(nofLanesBackward);
				NetworkUtils.setOrigId(l, origId);
				NetworkUtils.setType(l, highway);
				setOrModifyLinkAttributes(l, way, false);
				network.addLink(l);
				this.id++;
			}
		}
	}

	protected boolean isOneway(OsmWay way) {
		String onewayTag = way.tags.get(TAG_ONEWAY);
		if (onewayTag != null) {
			if ("yes".equals(onewayTag) || "true".equals(onewayTag) || "1".equals(onewayTag)) {
				return true;
			} else if ("-1".equals(onewayTag) || "reverse".equals(onewayTag)) {
				return false;
			} else if ("no".equals(onewayTag) || "false".equals(onewayTag) || "0".equals(onewayTag)) {
				return false; // may be used to overwrite defaults
			}
			else {
				log.warn("Could not interpret oneway tag:" + onewayTag + ". Ignoring it.");
			}
		}
		// If no oneway tag was found, see if it is a link in a roundabout
		if ("roundabout".equals(way.tags.get(TAG_JUNCTION))) {
			// if "junction" is not set in tags, get() returns null and equals() evaluates to false
			return true;
		}
		// If no direction-specific tag was found, use default value
		OsmHighwayDefaults defaults = this.highwayDefaults.get(way.tags.get(TAG_HIGHWAY));
		return defaults.oneway;
	}

	protected boolean isOnewayReverse(OsmWay way) {
		String onewayTag = way.tags.get(TAG_ONEWAY);
		if (onewayTag != null) {
			if ("yes".equals(onewayTag) || "true".equals(onewayTag) || "1".equals(onewayTag)) {
				return false;
			} else if ("-1".equals(onewayTag) || "reverse".equals(onewayTag)) {
				return true;
			} else if ("no".equals(onewayTag) || "false".equals(onewayTag) || "0".equals(onewayTag)) {
				return false; // may be used to overwrite defaults
			}
			else {
				log.warn("Could not interpret oneway tag:" + onewayTag + ". Ignoring it.");
			}
		}
		// Was set like this before; rather get this from defaults like for (forward) oneway? tt, aug'17
		return false;
	}

	/**
	 * Override this method if you want to add additional attributes to the node, e.g. elevation (z), or modify parameter values.
	 */
	protected void setOrModifyNodeAttributes(Node n, OsmNode node) {
	}

	/**
	 * Override this method if you want to add additional attributes to the link, e.g. modes, or modify parameter values.
	 */
	protected void setOrModifyLinkAttributes(Link l, OsmWay way, boolean forwardDirection) {
	}

	/**
	 * Override this method if you want to use another way to figure out if one can travel on this link in reverse direction.
	 */
	protected boolean reverseDirectionExists(OsmWay way) {
		return !isOneway(way);
	}

	/**
	 * Override this method if you want to use another way to figure out if one can travel on this link in forward direction.
	 */
	protected boolean forwardDirectionExists(OsmWay way) {
		return !isOnewayReverse(way);
	}

	public static interface OsmFilter {
		boolean coordInFilter( final Coord coord, final int hierarchyLevel ) ;
	}

	private static class OsmFilterImpl implements OsmFilter {
		private final Coord coordNW;
		private final Coord coordSE;
		private final int hierarchy;

		OsmFilterImpl(final Coord coordNW, final Coord coordSE, final int hierarchy) {
			this.coordNW = coordNW;
			this.coordSE = coordSE;
			this.hierarchy = hierarchy;
		}

		@Override
		public boolean coordInFilter(final Coord coord, final int hierarchyLevel){
			if(this.hierarchy < hierarchyLevel){
				return false;
			}

			return ((this.coordNW.getX() < coord.getX() && coord.getX() < this.coordSE.getX()) &&
				(this.coordNW.getY() > coord.getY() && coord.getY() > this.coordSE.getY()));
		}
	}

	private static class GeographicallyNonrestrictingOsmFilterImpl implements OsmFilter {
		private final int hierarchy;
		GeographicallyNonrestrictingOsmFilterImpl(final int hierarchy) {
			this.hierarchy = hierarchy;
		}

		@Override
		public boolean coordInFilter(final Coord coord, final int hierarchyLevel){
			if(this.hierarchy < hierarchyLevel){
				return false;
			}
			return true ;
		}
	}

	protected final static class OsmNode {
		public final long id;
		public boolean used = false;
		public Map<Long, OsmWay> ways = new HashMap<>();
		public Coord coord;
		public boolean endPoint = false;

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
		}
	}

	protected final static class OsmWay {
		public final long id;
		public final List<Long> nodes = new ArrayList<>(4);
		public final Map<String, String> tags = new HashMap<>(4);
		public int hierarchy = -1;

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	protected static class OsmHighwayDefaults {

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

	protected class OsmXmlParser extends MatsimXmlParser {

		protected OsmWay currentWay = null;
		protected OsmNode currentNode = null;
		protected final Map<Long, OsmNode> nodes;
		protected final Map<Long, OsmWay> ways;
		/*package*/ final Counter nodeCounter = new Counter("node ");
		/*package*/ final Counter wayCounter = new Counter("way ");
		private final CoordinateTransformation transform;
		private boolean loadNodes = true;
		private boolean loadWays = true;
		private boolean mergeNodes = false;
		private boolean collectNodes = false;

		public OsmXmlParser(final Map<Long, OsmNode> nodes, final Map<Long, OsmWay> ways, final CoordinateTransformation transform) {
			super(ValidationType.NO_VALIDATION);
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
					this.currentNode = new OsmNode(id, this.transform.transform(new Coord(lon, lat)));
					this.nodes.put(id, currentNode);
					this.nodeCounter.incCounter();
				} else if (this.mergeNodes) {
					OsmNode node = this.nodes.get(Long.valueOf(atts.getValue("id")));
					if (node != null) {
						double lat = Double.parseDouble(atts.getValue("lat"));
						double lon = Double.parseDouble(atts.getValue("lon"));
						node.coord = this.transform.transform(new Coord(lon, lat)) ;
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
					for (String tag : allTags) {
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
					OsmHighwayDefaults osmHighwayDefaults = OsmNetworkReader.this.highwayDefaults.get(this.currentWay.tags.get(TAG_HIGHWAY));
					if (osmHighwayDefaults != null) {
						int hierarchy = osmHighwayDefaults.hierarchy;
						this.currentWay.hierarchy = hierarchy;
						if (OsmNetworkReader.this.hierarchyLayers.isEmpty()) {
							used = true;
						}
						if (this.collectNodes) {
							used = true;
						} else {
							for (OsmFilter osmFilter : OsmNetworkReader.this.hierarchyLayers) {
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
								this.nodes.put(id, new OsmNode(id, new Coord(0, 0)));
							}
						} else if (this.loadWays) {
							this.ways.put(this.currentWay.id, this.currentWay);
							this.wayCounter.incCounter();
						}
					}
				}
				this.currentWay = null;
			} else if ("node".equals(name)) {
				// was already added in startTag(...)
				this.currentNode = null;
			}
		}

	}

	private static class StringCache {
		private static final  ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(10000);

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
