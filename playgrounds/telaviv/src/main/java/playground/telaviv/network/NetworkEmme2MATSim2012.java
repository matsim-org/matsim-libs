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

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.telaviv.config.TelAvivConfig;
import playground.toronto.maneuvers.NetworkAddEmmeManeuverRestrictions;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Translates emme2 networks into matsim networks.
 * <p/>
 * The network data is preprocessed by Shlomo Bekhor in an Excel file. The tables
 * are exported to csv/txt files and parsed.
 * <p/>
 * The provided Shape Files use WGS84 Coordinates but MATSim need an euclidian System.
 * Therefore two Network files are created:
 * - network_WGS84.xml with WGS84 coordinates
 * - network.xml with ITM coordinates (Israeli Transverse Mercator)
 * <p/>
 * Nodes:
 * index / column / data example: 
 * 0	/	NODE_ID	/	100
 * 1	/	COORD_X	/	138.0940	// Israeli Coordinates (ITM)
 * 2	/	COORD_Y	/	192.4970	// // Israeli Coordinates (ITM)
 * 4	/	longitude	/	34.8706600
 * 5	/	latitude	/	32.3254460
 * <p/>
 * Links:
 * index / column / data example:  
 * 0	/	From_node	/	103
 * 1	/	To_node	/	220500
 * 2	/	Connector	/	1
 * 3	/	Length (km)	/	0.20
 * 4	/	modes	/	cu
 * 5	/	type	/	9
 * 6	/	lanes	/	9
 * 7	/	vdf	/	2
 * 8	/	FT (facility type)	/	0
 * 9	/	AT (area type)	/	0
 * 10	/	IT (intersection type)	/	0
 * 11	/	NLI (additional lanes at intersection)	/	0
 * 12	/	tzero_link	/	0
 * 13	/	tzero_int	/	0
 * 14	/	cap_link	/	0
 * 15	/	cap_int	/	0
 * 16	/	tzero_mod	/	0.8
 * 17	/	cap_mod	/	999000
 * 18	/	speed(m/s)	/	4.17
 * 19	/	speed(km/h)	/	15
 * 20	/	auto_vol_AM	/	0
 * 21	/	comm_vol_AM	/	0
 * 22	/	truck_vol_AM	/	0
 * 23	/	bus_vol_AM	/	0
 * 24	/	Tot_vol_AM	/	0
 * <p/>
 * 
 * Keyword(s): emme/2
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
	private static enum modes {c, u, b, p, r};
	
//	private static double capacityScaleFator = 1.1;	// increase capacity by 10%
	private static double capacityScaleFator = 1.0;
	
	private static String ITM = "EPSG:2039";	// network coding String
	
	private static String nodesFile = TelAvivConfig.basePath + "/network/nodes.csv";
	private static String linksFile = TelAvivConfig.basePath + "/network/links_updated-revised_2013-09.csv";
	private static String maneuversFile = TelAvivConfig.basePath + "/network/turns.csv";
	private static String outFileWGS84 = TelAvivConfig.basePath + "/network/network_WGS84.xml";
	private static String outFileITM = TelAvivConfig.basePath + "/network/network.xml";
	private static String kmzFile = TelAvivConfig.basePath + "/network/network.kml";
	private static String shpLinksFile = TelAvivConfig.basePath + "/network/links.shp";
	private static String shpNodesFile = TelAvivConfig.basePath + "/network/nodes.shp";
	
	private static String separator = ";";
	
	public static void readNetwork(NetworkImpl network, boolean useWGS84) {
		network.setCapacityPeriod(3600.) ;
		network.setEffectiveLaneWidth(3.75) ;
//		network.setEffectiveCellSize(7.5) ;

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		// read emme2 network
		try {
			
			readNodes(network, scenario, useWGS84);
			
			readLinks(network, scenario);
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	public static void readNodes(NetworkImpl network, Scenario scenario, boolean useWGS84) throws FileNotFoundException, IOException {
		
		BufferedReader reader = IOUtils.getBufferedReader(nodesFile);
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
				
				CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84");
				
				double xx = Double.valueOf(xxStr);
				double yy = Double.valueOf(yyStr);
				
				/*
				 * The Coordinates are in the old Israeli Cassini-Soldner (ICS).
				 * We convert them to the new one (ITM) and then convert from km to m.
				 * (See: http://en.wikipedia.org/wiki/Israeli_Transverse_Mercator) 
				 */
				xx = (xx + 50) * 1000;
				yy = (yy + 500) * 1000;
				
				network.createAndAddNode(scenario.createId(idStr), transformation.transform(new CoordImpl(xx, yy)));
			}
			else {
				double xx = Double.valueOf(xxStr);
				double yy = Double.valueOf(yyStr);
				
				/*
				 * The Coordinates are in the old Israeli Coordinates System (ICS).
				 * We convert them to the new one (ITM) and then convert from km to m.
				 * (See: http://en.wikipedia.org/wiki/Israeli_Transverse_Mercator) 
				 */
				xx = (xx + 50) * 1000;
				yy = (yy + 500) * 1000;
				
				network.createAndAddNode(scenario.createId(idStr), new CoordImpl(xx, yy));
			}
		}
	}
	
	public static void readLinks(NetworkImpl network, Scenario scenario) throws FileNotFoundException, IOException {
		
		BufferedReader reader = IOUtils.getBufferedReader(linksFile);
		String line;

		// skip header line
		reader.readLine();

		long linkCnt = 0;
		
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(separator);

			Node fromNode = network.getNodes().get(scenario.createId(parts[0]));
			Node   toNode = network.getNodes().get(scenario.createId(parts[1]));
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
			if (length <= 0.0) length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
			
			// if length is still null, use 25.0m instead
			if (length <= 0.0) length = 25.0;

			double permlanes = Double.parseDouble(parts[6]);
			if ( permlanes <= 0 ) { permlanes = 0.5; }

			String oridId = parts[0] + "#" + parts[1];
			String type = parts[5];
			
			double capacity = Double.valueOf(parts[17]) * capacityScaleFator;
			
			// if the capacity is <= 0.0 and it is a connector link, increase capacity
			if (capacity <= 0.0 && Integer.parseInt(parts[2]) == 1) capacity = 999000;
			
			double freespeed = Double.valueOf(parts[18]);

			Id id = scenario.createId(String.valueOf(linkCnt));
			linkCnt++;

			network.createAndAddLink(id, fromNode, toNode, length, freespeed, capacity, permlanes, oridId, type);
			
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
	}

	public static void main(String[] args) {
		NetworkImpl networkITM = NetworkImpl.createNetwork();
		NetworkImpl networkWGS84 = NetworkImpl.createNetwork();

		log.info("reading network ...");
		readNetwork(networkITM, false);
		readNetwork(networkWGS84, true);
		log.info("... finished reading network.\n");
		
		log.info("reading and processing maneuvers ...");
		try {
			NetworkAddEmmeManeuverRestrictions mr = new NetworkAddEmmeManeuverRestrictions(maneuversFile);
			mr.run(networkITM);
			mr.run(networkWGS84);
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
		
		log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner();
		nwCleaner.run(networkITM);
		nwCleaner.run(networkWGS84);
		log.info("... finished reading and processing maneuvers.\n");
		
		log.info("writing network ...");
		new NetworkWriter(networkITM).write(outFileITM);
		new NetworkWriter(networkWGS84).write(outFileWGS84);
		log.info("... finished writing network.\n");
		
		
		try {
			ObjectFactory kmlObjectFactory = new ObjectFactory();
			KMZWriter kmzWriter = new KMZWriter(kmzFile);
		
			KmlType mainKml = kmlObjectFactory.createKmlType();
			DocumentType mainDoc = kmlObjectFactory.createDocumentType();
			mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
			
			KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(networkITM, new GeotoolsTransformation(ITM, "WGS84"), kmzWriter, mainDoc);
		
			mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
			kmzWriter.writeMainKml(mainKml);
			kmzWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Collection<SimpleFeature> ft;
			ft = generateNodesFromNet(networkWGS84);
			ShapeFileWriter.writeGeometries(ft, shpNodesFile);
			
			ft = generateLinksFromNet(networkWGS84);
			ShapeFileWriter.writeGeometries(ft, shpLinksFile);
			
		} 
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}
	
	
	public static Collection<SimpleFeature> generateLinksFromNet(Network network) {

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

	public static Collection<SimpleFeature> generateNodesFromNet(Network network) {
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
