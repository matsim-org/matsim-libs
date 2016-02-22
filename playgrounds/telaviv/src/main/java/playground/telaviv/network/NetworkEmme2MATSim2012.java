/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkEmme2MATSim2012.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.telaviv.network;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.telaviv.config.XMLParameterParser;
import playground.toronto.maneuvers.NetworkAddEmmeManeuverRestrictions;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Translates emme2 networks into matsim networks.
 * <p/>
 * The network data is pre-processed by Shlomo Bekhor in an Excel file. The tables
 * are exported to csv/txt files and parsed.
 * <p/>
 * The provided shp files use WGS84 Coordinates but MATSim need an euclidian System.<br>
 * Therefore two Network files are created:<br>
 * - network_WGS84.xml with WGS84 coordinates<br>
 * - network.xml with ITM coordinates (Israeli Transverse Mercator)<br>
 * <p/>
 * Nodes:<br>
 * index / column / data example: <br>
 * 0	/	NODE_ID	/	100<br>
 * 1	/	COORD_X	/	138.0940	// Israeli Coordinates (ITM)<br>
 * 2	/	COORD_Y	/	192.4970	// // Israeli Coordinates (ITM)<br>
 * 4	/	longitude	/	34.8706600<br>
 * 5	/	latitude	/	32.3254460<br>
 * <p/>
 * Links:<br>
 * index / column / data example:<br>
 * 0	/	From_node	/	103<br>
 * 1	/	To_node	/	220500<br>
 * 2	/	Connector	/	1<br>
 * 3	/	Length (km)	/	0.20<br>
 * 4	/	modes	/	cu<br>
 * 5	/	type	/	9<br>
 * 6	/	lanes	/	9<br>
 * 7	/	vdf	/	2<br>
 * 8	/	FT (facility type)	/	0<br>
 * 9	/	AT (area type)	/	0<br>
 * 10	/	IT (intersection type)	/	0<br>
 * 11	/	NLI (additional lanes at intersection)	/	0<br>
 * 12	/	tzero_link	/	0<br>
 * 13	/	tzero_int	/	0<br>
 * 14	/	cap_link	/	0<br>
 * 15	/	cap_int	/	0<br>
 * 16	/	tzero_mod	/	0.8<br>
 * 17	/	cap_mod	/	999000<br>
 * 18	/	speed(m/s)	/	4.17<br>
 * 19	/	speed(km/h)	/	15<br>
 * 20	/	auto_vol_AM	/	0<br>
 * 21	/	comm_vol_AM	/	0<br>
 * 22	/	truck_vol_AM	/	0<br>
 * 23	/	bus_vol_AM	/	0<br>
 * 24	/	Tot_vol_AM	/	0<br>
 * <p/>
 * 
 * <p>
 * Link types are like:<br>
 * 1 ... freeway<br>
 * 2 ... main road<br>
 * 3 ... arterial road<br>
 * 4 ... regional road<br>
 * 5 ... collector street<br>
 * 6 ... local street<br>
 * 7 ... multilane highway<br>
 * 9 ... centroid connector<br>
 * 
 * By default, centroid connectors are NOT included in the MATSim network. They do not need to be present
 * there since we can add traffic to any link.
 * </p>
 * 
 * <p>
 * Several output files are created - some of them are optional:<br>
 * - network file in MATSim xml format using ITM coordinates<br>
 * - network file in MATSim xml format using WGS84 coordinates<br>
 * - network file in MATSim xml format without post-processing (for counts and road pricing)<br>
 * - network file in google kml format using WGS84 coordinates (optional)<br>
 * - links file in shp format using WGS84 coordinates (optional)<br>
 * - nodes file in shp format using WGS84 coordinates (optional)<br>
 * </p>
 * 
 * @author cdobler
 */
public class NetworkEmme2MATSim2012 {
	
	private static final Logger log = Logger.getLogger(NetworkEmme2MATSim2012.class);

	/*
	 *  possible modes
	 *  - car
	 *  - ???
	 *  - bus
	 *  - pedestrian
	 *  - rail
	 */
//	private static enum modes {c, u, b, p, r};
	
//	private static double capacityScaleFator = 1.1;	// increase capacity by 10%
	private static double capacityScaleFator = 1.0;
	
	private static String ITM = "EPSG:2039";	// network coding String
	
	private static String basePath = "";
	private static String nodesFile = "";
	private static String linksFile = "";
	private static String maneuversFile = "";
	private static String outFileWGS84 = "";
	private static String outFileITM = "";
	private static String outFileOriginal = "";
	private static String kmlFile = "";
	private static String shpLinksFile = "";
	private static String shpNodesFile = "";
	
	private static String separator = ";";
	
	private static Set<String> includedLinkTypes = CollectionUtils.stringToSet("1,2,3,4,5,6,7");
	
	private static boolean writeKMLFile = true;
	private static boolean writeSHPFiles = true;
	
	public static void main(String[] args) {		
		try {
			if (args.length > 0) {
				String file = args[0];
				Map<String, String> parameterMap = new XMLParameterParser().parseFile(file);
				String value;
				
				value = parameterMap.remove("basePath");
				if (value != null) basePath = value;

				value = parameterMap.remove("nodesFile");
				if (value != null) nodesFile = value;
				
				value = parameterMap.remove("linksFile");
				if (value != null) linksFile = value;
				
				value = parameterMap.remove("maneuversFile");
				if (value != null) maneuversFile = value;
				
				value = parameterMap.remove("outFileWGS84");
				if (value != null) outFileWGS84 = value;
				
				value = parameterMap.remove("outFileITM");
				if (value != null) outFileITM = value;
				
				value = parameterMap.remove("outFileOriginal");
				if (value != null) outFileOriginal = value;
				
				value = parameterMap.remove("kmlFile");
				if (value != null) kmlFile = value;
				
				value = parameterMap.remove("shpLinksFile");
				if (value != null) shpLinksFile = value;
				
				value = parameterMap.remove("shpNodesFile");
				if (value != null) shpNodesFile = value;
				
				value = parameterMap.remove("separator");
				if (value != null) separator = value;
				
				value = parameterMap.remove("capacityScaleFator");
				if (value != null) capacityScaleFator = Double.parseDouble(value);

				value = parameterMap.remove("includedLinkTypes");
				if (value != null) includedLinkTypes = CollectionUtils.stringToSet(value);
				
				value = parameterMap.remove("writeKMLFile");
				if (value != null) writeKMLFile = Boolean.parseBoolean(value);
				
				value = parameterMap.remove("writeSHPFiles");
				if (value != null) writeSHPFiles = Boolean.parseBoolean(value);
				
				for (String key : parameterMap.keySet()) log.warn("Found parameter " + key + " which is not handled!");
			} else {
				log.error("No input config file was given. Therefore cannont proceed. Aborting!");
				return;
			}
			
			log.info("reading network ...");
			Network networkITM = readNetwork(false);
			Network networkWGS84 = readNetwork(true);
			Network networkOriginal = readNetwork(false);
			log.info("done.\n");
			
			log.info("reading and processing maneuvers ...");
			NetworkAddEmmeManeuverRestrictions mr = new NetworkAddEmmeManeuverRestrictions(basePath + maneuversFile);
			mr.run(networkITM);
			mr.run(networkWGS84);
			log.info("done.\n");
		
			log.info("cleaning network ...");
			NetworkCleaner nwCleaner = new NetworkCleaner();
			nwCleaner.run(networkITM);
			nwCleaner.run(networkWGS84);
			log.info("done.\n");
			
			log.info("writing network ...");
			new NetworkWriter(networkITM).write(basePath + outFileITM);
			new NetworkWriter(networkWGS84).write(basePath + outFileWGS84);
			new NetworkWriter(networkOriginal).write(basePath + outFileOriginal);
			log.info("done.\n");
			
			if (writeKMLFile) {
				log.info("writing KML file ...");
				ObjectFactory kmlObjectFactory = new ObjectFactory();
				DocumentType mainDoc = kmlObjectFactory.createDocumentType();
				KMZWriter kmzWriter = new KMZWriter(basePath + kmlFile);
				KmlType mainKml = kmlObjectFactory.createKmlType();
				mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));			

				KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(networkITM, new GeotoolsTransformation(ITM, "WGS84"), kmzWriter, mainDoc);
				mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
				kmzWriter.writeMainKml(mainKml);
				kmzWriter.close();
				log.info("done.\n");
			}

			if (writeSHPFiles) {
				log.info("writing SHP files ...");
				Collection<SimpleFeature> ft;
				ft = generateNodesFromNet(networkWGS84);
				ShapeFileWriter.writeGeometries(ft, basePath + shpNodesFile);
				
				ft = generateLinksFromNet(networkWGS84);
				ShapeFileWriter.writeGeometries(ft, basePath + shpLinksFile);
				log.info("done.\n");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Network readNetwork(boolean useWGS84) throws Exception {
//		network.setCapacityPeriod(3600.0);
//		network.setEffectiveLaneWidth(3.75);
//		network.setEffectiveCellSize(7.5);

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		// read emme2 network
		readNodes(scenario, useWGS84);
		readLinks(scenario);
		
		return scenario.getNetwork();
	}

	// TODO: make this private and use "network_original" in place that access this method
	public static void readNodes(Scenario scenario, boolean useWGS84) throws FileNotFoundException, IOException {
		
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84");
		
		BufferedReader reader = IOUtils.getBufferedReader(basePath + nodesFile);
		String line;

		// skip header line
		reader.readLine();
		
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(separator);
			String idStr = parts[0];
			String xxStr = parts[1];
			String yyStr = parts[2];
			String xxStrWGS84 = parts[3];
			String yyStrWGS84 = parts[4];
			
			if (useWGS84) {
				/*
				 * For some connector links the WGS84 coordinate seem to be wrong. Therefore, we
				 * create ITM coordinates and convert them to WGS84.
				 */
//				network.createAndAddNode(scenario.createId(idStr), new CoordImpl(xxStrWGS84, yyStrWGS84));	
				
				double xx = Double.valueOf(xxStr);
				double yy = Double.valueOf(yyStr);
				
				/*
				 * The Coordinates are in the old Israeli Cassini-Soldner (ICS).
				 * We convert them to the new one (ITM) and then convert from km to m.
				 * (See: http://en.wikipedia.org/wiki/Israeli_Transverse_Mercator) 
				 */
				xx = (xx + 50) * 1000;
				yy = (yy + 500) * 1000;

				Node node = networkFactory.createNode(Id.create(idStr, Node.class), transformation.transform(new Coord(xx, yy)));
				network.addNode(node);
			} else {
				double xx = Double.valueOf(xxStr);
				double yy = Double.valueOf(yyStr);
				
				/*
				 * The Coordinates are in the old Israeli Coordinates System (ICS).
				 * We convert them to the new one (ITM) and then convert from km to m.
				 * (See: http://en.wikipedia.org/wiki/Israeli_Transverse_Mercator) 
				 */
				xx = (xx + 50) * 1000;
				yy = (yy + 500) * 1000;

				Node node = networkFactory.createNode(Id.create(idStr, Node.class), new Coord(xx, yy));
				network.addNode(node);
			}
		}
	}
	
	private static void readLinks(Scenario scenario) throws FileNotFoundException, IOException {
		
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();
		
		BufferedReader reader = IOUtils.getBufferedReader(basePath + linksFile);
		String line;

		// skip header line
		reader.readLine();

		long linkCnt = 0;
		
		Counter counter = new Counter("\t# links that are ignored due to their type: ");
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(separator);

			Node fromNode = network.getNodes().get(Id.create(parts[0], Node.class));
			Node   toNode = network.getNodes().get(Id.create(parts[1], Node.class));
			if ( fromNode==null || toNode==null ) {
				log.info("fromNode or toNode == null; probably connector link; skipping it ...") ;
				continue ;
			}
			
//			if ( parts[4].contains("r") || parts[4].contains("b") ) {
//				log.info("rail only or bus only link; skipping it ...") ;
//				continue;
//			}
			if (!parts[4].contains("c")) {
				log.info("no car link; skipping it ...") ;
				continue;
			}

			double length = 1000 * Double.parseDouble(parts[2]); // km -> convert to m

			// if length is 0.0, try crow fly distance
			if (length <= 0.0) length = CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
			
			// if length is still null, use 25.0m instead
			if (length <= 0.0) length = 25.0;

			double permlanes = Double.parseDouble(parts[6]);
			if ( permlanes <= 0 ) { permlanes = 0.5; }

			String origId = parts[0] + "#" + parts[1];
			String type = parts[5];
			
			if (!includedLinkTypes.contains(type)) {
				counter.incCounter();
				continue;
			}
			
			double capacity = Double.valueOf(parts[17]) * capacityScaleFator;
			
			// if the capacity is <= 0.0 and it is a connector link, increase capacity
			if (capacity <= 0.0 && Integer.parseInt(parts[2]) == 1) capacity = 999000;
			
			double freespeed = Double.valueOf(parts[18]);

			Id<Link> id = Id.create(String.valueOf(linkCnt), Link.class);
			linkCnt++;

			Link link = networkFactory.createLink(id, fromNode, toNode);
			link.setLength(length);
			link.setFreespeed(freespeed);
			link.setCapacity(capacity);
			link.setNumberOfLanes(permlanes);
			((LinkImpl) link).setType(type);
			((LinkImpl) link).setOrigId(origId);
			network.addLink(link);
			
			/* 
			 * TODO:
			 * Some turning restrictions are not restricted but have a waiting time.
			 * We alter the network to represent them:
			 * 
			 * 	At	From	To	waiting_time
			 *	200970	200222	200960	1
			 * 	220360	220610	220860	1
			 *	200960	200550	200970	2
			 *	222290	200550	222370	3
			 *	200960	200970	200550	5
			 */
		}
		counter.printCounter();
	}
	
	private static Collection<SimpleFeature> generateLinksFromNet(Network network) {

		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder().
				setCrs(crs).
				setName("links").
				addAttribute("ID", String.class).
				addAttribute("fromID", String.class).
				addAttribute("toID", String.class).
				addAttribute("length", Double.class).
				addAttribute("type", String.class).
				create();
		
		for (Link link : network.getLinks().values()) {
			SimpleFeature ft = factory.createPolyline(
					new Coordinate [] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())},
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(),link.getLength(), ((LinkImpl)link).getType()},
					link.getId().toString()
					);
			features.add(ft);
		}
				
		return features;
	}

	private static Collection<SimpleFeature> generateNodesFromNet(Network network) {
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;

		PointFeatureFactory factory = new PointFeatureFactory.Builder().
				setCrs(crs).
				setName("nodes").
				addAttribute("ID", String.class).
				create();
		
		for (Node node : network.getNodes().values()) {
			SimpleFeature ft = factory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, node.getId().toString());
			features.add(ft);
		}
				
		return features;
	}
}