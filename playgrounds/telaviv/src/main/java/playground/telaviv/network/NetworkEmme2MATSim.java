/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkEmme2MATSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.KmlNetworkWriter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.kml.KMZWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.toronto.maneuvers.NetworkAddEmmeManeuverRestrictions;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * This class has been used for the 2010 model and is therefore outdated! To be removed in the future.
 * 
 * Translates emme2 networks into matsim networks.
 * <p></p>
 * The network data is preprocessed by Shlomo Bekhor in an Excel file. The tables
 * are exported to csv/txt files and parsed.
 * <p></p>
 * The provided Shape Files use WGS84 Coordinates but MATSim need an euclidian System.
 * Therefore two Network files are created:
 * - network_WGS84.xml with WGS84 coordinates
 * - network.xml with ITM coordinates (Israeli Transverse Mercator)
 * <p></p>
 * Nodes:
 * index / column / data example: 
 * 0	/	NODE_ID	/	100
 * 1	/	COORD_X	/	138.0940	// Israeli Coordinates (ITM)
 * 2	/	COORD_Y	/	192.4970	// // Israeli Coordinates (ITM)
 * 3	/	Car_assign?	/	1	(boolean 1/0)
 * 4	/	longitude	/	34.8706600
 * 5	/	latitude	/	32.3254460
 * <p></p>
 * Links:
 * index / column / data example:  
 * 0	/	From_node	/	103
 * 1	/	To_node	/	220500
 * 2	/	Length (km)	/	0.20
 * 3	/	modes	/	cu
 * 4	/	type	/	9
 * 5	/	lanes	/	9
 * 6	/	vdf	/	2
 * 7	/	ul1	/	0
 * 8	/	ul2	/	0
 * 9	/	add_flow	/	0
 * 10	/	tzero_link	/	0
 * 11	/	tzero_int	/	0
 * 12	/	cap_link	/	0
 * 13	/	cap_int	/	0
 * 14	/	tzero_mod	/	0.8
 * 15	/	cap_mod	/	999000
 * 16	/	speed(m/s)	/	4.17
 * 17	/	speed(km/h)	/	15
 * <p></p>
 * Keyword(s): emme/2
 * 
 * @author cdobler
 *
 */
public class NetworkEmme2MATSim {
	private static final Logger log = Logger.getLogger(NetworkEmme2MATSim.class);

	/*
	 *  possible modes
	 *  - car
	 *  - ???
	 *  - bus
	 *  - pedestrian
	 *  - rail
	 */
	private static enum modes {c, u, b, p, r};
	
	private static double capacityScaleFator = 1.1;	// increase capacity by 10%
	
	private static String ITM = "EPSG:2039";	// network coding String
	
	private static String nodesFile = "../../matsim/mysimulations/telaviv/network/nodes.csv";
	private static String linksFile = "../../matsim/mysimulations/telaviv/network/links.csv";
//	private static String maneuversFile = "../../matsim/mysimulations/telaviv/network/turns1000.in";
	private static String maneuversFile = "../../matsim/mysimulations/telaviv/network/maneuvers.csv";
	private static String outFileWGS84 = "../../matsim/mysimulations/telaviv/network/network_WGS84.xml";
	private static String outFileITM = "../../matsim/mysimulations/telaviv/network/network.xml";
	private static String kmzFile = "../../matsim/mysimulations/telaviv/network/network.kml";
	private static String shpFile = "../../matsim/mysimulations/telaviv/network/network.shp";
	private static String shpLinksFile = "../../matsim/mysimulations/telaviv/network/links.shp";
	private static String shpNodesFile = "../../matsim/mysimulations/telaviv/network/nodes.shp";
	
	private static String separator = ",";
	
	public static void readNetwork(Network network, boolean useWGS84) {
		network.setCapacityPeriod(3600.) ;
		network.setEffectiveLaneWidth(3.75) ;
//		network.setEffectiveCellSize(7.5) ;

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		// read emme2 network
		try {
			
			BufferedReader reader = null;
			String line;
			
			/*
			 * read nodes
			 */
			reader = IOUtils.getBufferedReader(nodesFile);

			// skip header line
			reader.readLine();
			
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(separator);
				String idStr = parts[0];
				String xxStr = parts[1];
				String yyStr = parts[2];
				String xxStrWGS84 = parts[4];
				String yyStrWGS84 = parts[5];
				String carAssignStr = parts[3];
				
				if (useWGS84) {
					Node node = NetworkUtils.createAndAddNode(network, Id.create(idStr, Node.class), new Coord(Double.parseDouble(xxStrWGS84), Double.parseDouble(yyStrWGS84)));
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

					Node node = NetworkUtils.createAndAddNode(network, Id.create(idStr, Node.class), new Coord(xx, yy));
				}
			}
			
			/*
			 * read links
			 */
			reader = IOUtils.getBufferedReader(linksFile);
			
			// skip header line
			reader.readLine();
			
			long linkCnt = 0;
			
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(separator);

				Node fromNode = network.getNodes().get(Id.create(parts[0], Node.class));
				Node   toNode = network.getNodes().get(Id.create(parts[1], Node.class));
				if ( fromNode==null || toNode==null ) {
					log.info("fromNode or toNode == null; probably connector link; skipping it ...") ;
					continue ;
				}
//				if ( parts[3].contains("r") || parts[3].contains("b") ) {
//					log.info("rail only or bus only link; skipping it ...") ;
//					continue;
//				}
				if (!parts[3].contains("c")) {
					log.info("no car link; skipping it ...") ;
					continue;
				}

				double length = 1000 * Double.parseDouble( parts[2]); // km -> convert to m
//				String type = parts[4] ;

				double permlanes = Double.parseDouble(parts[5]);
				if ( permlanes <= 0 ) { permlanes = 0.5; }

				String oridId = parts[0] + "#" + parts[1];
				String type = parts[4];
				
				double capacity = Double.valueOf(parts[15]) * capacityScaleFator;
				double freespeed = Double.valueOf(parts[16]);
				
//				double capacity, freespeed;
//				if ( NW_NAME==PSRC ) {
//					capacity = permlanes * Double.parseDouble( parts[7] ) ;
//					if ( capacity <= 500 ) { capacity = 500. ; }
//
//					freespeed = Double.parseDouble( parts[8] ) ; // mph
//					if ( freespeed < 10. ) { freespeed = 10. ; }
//					freespeed *= 1600./3600. ;
//				} else if ( NW_NAME==EUGENE ) {
//					log.warn("For EUGENE, I have not clarified if capacity really needs to be multiplied by number of lanes.");
//					capacity = permlanes * Double.parseDouble( parts[9] ) ;
//					if ( capacity <= 500 ) { capacity = 500. ; }
//
//					freespeed = Double.parseDouble( parts[8] ) ; // mph
//					if ( freespeed < 10. ) { freespeed = 10. ; }
//					freespeed *= 1600./3600. ;
//				} else {
//					log.error( "NW_NAME not known; aborting" ) ;
//					System.exit(-1);
//				}

				Id<Link> id = Id.create(String.valueOf(linkCnt), Link.class);
				linkCnt++;
				final Id<Link> id1 = id;
				final Node fromNode1 = fromNode;
				final Node toNode1 = toNode;
				final double length1 = length;
				final double freespeed1 = freespeed;
				final double capacity1 = capacity;
				final double numLanes = permlanes;
				final String origId = oridId;
				final String type1 = type;

				NetworkUtils.createAndAddLink(network,id1, fromNode1, toNode1, length1, freespeed1, capacity1, numLanes, origId, type1);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}


	public static void main(String[] args) {
		Network networkITM = NetworkUtils.createNetwork();
		Network networkWGS84 = NetworkUtils.createNetwork();

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
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(),link.getLength(), NetworkUtils.getType(((Link)link))},
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
