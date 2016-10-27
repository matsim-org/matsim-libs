/* *********************************************************************** *
 * project: org.matsim.*
 * Utils3D.java                                                            *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.jjoubert.coord3D;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.osmtools.srtm.SrtmTile;

import playground.southafrica.utilities.Header;

/**
 * A number of utilities to deal with 3D networks.
 * 
 * @author jwjoubert
 */
public class Utils3D {
	private final static Logger LOG = Logger.getLogger(Utils3D.class);
	private final static String SRTM_URL_AFRICA = "https://dds.cr.usgs.gov/srtm/version2_1/SRTM3/Africa/";
	
	public static void main(String[] args){
		Header.printHeader(Utils3D.class.toString(), args);
		
		int option = Integer.parseInt(args[0]);
		String network = args[1];
		String tilePath = args[2];
		String crs = args[3];
		String output = args[4];
		switch (option) {
		case 1: /* Calculate and write the grade for each link in a MATSim network. */
			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs, "WGS84");
			writeNetworkGrades(network, tilePath, ct, output);
			break;
		default:
			throw new RuntimeException("Don't know what to execute for option '" + option + "'.");
		}
		
		Header.printFooter();
	}
	
	
	
	private static void writeNetworkGrades(String network, String tilePath, CoordinateTransformation ct, String output){
		LOG.info("Calculating the grade for each link in a network and writing it to file...");
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(network);
		
		/* Provide elevation to each node in the network. */
		LOG.info("Estimating elevation for each network node...");
		Counter nodeCounter = new Counter("  node # ");
		for(Node node : sc.getNetwork().getNodes().values()){
			Coord cWgs = ct.transform(node.getCoord());
			double elev = estimateSrtmElevation(tilePath, cWgs);
			node.setCoord(CoordUtils.createCoord(cWgs.getX(), cWgs.getY(), elev));
			nodeCounter.incCounter();
		}
		nodeCounter.printCounter();
		
		LOG.info("Calculating grade for each link...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		Counter linkCounter = new Counter("  link # ");
		try{
			/* Write header. */
			bw.write("id,length,grade");
			bw.newLine();
			
			/* Calculate the grade of each link. */
			for(Link link : sc.getNetwork().getLinks().values()){
				double grade = calculateGrade(link);
				bw.write(String.format("%s,%.1f,%.8f\n", link.getId().toString(), link.getLength(), grade));
				linkCounter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		linkCounter.printCounter();
		LOG.info("Done calculating the grades for network links.");
	}
	
	
	
	
	
	public static double calculateAngle(Link link){
		if(link.getFromNode().getCoord().getZ() == Double.NEGATIVE_INFINITY || 
				link.getToNode().getCoord().getZ() == Double.NEGATIVE_INFINITY){
			LOG.error("From node: " + link.getFromNode().getCoord().toString());
			LOG.error("To node: " + link.getToNode().getCoord().toString());
			throw new IllegalArgumentException("Cannot calculate angle if both nodes on the link do not have elevation (z) set.");
		}
		
		Coord c1 = link.getFromNode().getCoord();
		Coord c2 = link.getToNode().getCoord();
		
		double length = link.getLength();
		double angle = Math.asin((c2.getZ()-c1.getZ())/length);
		
		return angle;
	}

	public static double calculateGrade(Link link){
		if(link.getFromNode().getCoord().getZ() == Double.NEGATIVE_INFINITY || 
				link.getToNode().getCoord().getZ() == Double.NEGATIVE_INFINITY){
			LOG.error("From node: " + link.getFromNode().getCoord().toString());
			LOG.error("To node: " + link.getToNode().getCoord().toString());
			throw new IllegalArgumentException("Cannot calculate grade if both nodes on the link do not have elevation (z) set.");
		}
		
		Coord c1 = link.getFromNode().getCoord();
		Coord c2 = link.getToNode().getCoord();
		
		double length = link.getLength();
		double grade = (c2.getZ()-c1.getZ())/length;
		
		return grade;
	}
	
	public static Scenario elevateEquilNetwork(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		/* Read in the basic equil network. */
		new MatsimNetworkReader(sc.getNetwork()).readFile("../../matsim/examples/equil/network.xml");
		
		/* Give elevation details to the nodes. The upper nodes are elevated
		 * by increments of 1%, and the lower nodes by increments of -1%. The
		 * result is that the shorter section will have double the grade, but
		 * with the opposite sign. */
		Node n;
		n = sc.getNetwork().getNodes().get(Id.createNodeId("3"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 400.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("4"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 300.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("5"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 200.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("6"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 100.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("8"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), -100.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("9"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), -200.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("10"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), -300.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("11"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), -400.0));
		
		/* The remaining nodes MUST have elevation set, so we set them to 0. */
		n = sc.getNetwork().getNodes().get(Id.createNodeId("1"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 0.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("2"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 0.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("7"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 0.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("12"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 0.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("13"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 0.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("14"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 0.0));
		n = sc.getNetwork().getNodes().get(Id.createNodeId("15"));
		n.setCoord(CoordUtils.createCoord(n.getCoord().getX(), n.getCoord().getY(), 0.0));
		
		/* Adjust all links to have 2 lanes. */
		for(Link link : sc.getNetwork().getLinks().values()){
			link.setNumberOfLanes(2.0);
			link.setCapacity(10000);

			/* Fix link 6's length to be the same as all the others. */
			if(link.getId().equals(Id.createLinkId("6"))){
				link.setLength(10000);
			}
		}
		
		return sc;
	}

	
 	public static Coord getBottomLeftCoordinate(Network network){
 		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for(Node n : network.getNodes().values()){
			Coord c = ct.transform(n.getCoord());
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
		}
		return CoordUtils.createCoord(minX, minY);
	}
 	
 
	/**
	 * Use the function in the southAfrica playground instead.
	 * 
	 * @param c a coordinate assumed to be in WGS84 decimal degrees format.
	 * @return
	 */
 	@Deprecated
 	public static String getSrtmTile(Coord c){
		int lon = (int)Math.floor(c.getX());
		int lat = (int)Math.floor(c.getY());
		String lonPrefix = lon < 0 ? "W" : "E";
		String latPrefix = lat < 0 ? "S" : "N";
		return String.format("%s%02d%s%03d", latPrefix, Math.abs(lat), lonPrefix, Math.abs(lon));
 	}
 	
 	/**
 	 * Use the function in the southAfrica playground instead.
 	 *  
 	 * @param pathToTiles
 	 * @param c
 	 * @return
 	 */
 	@Deprecated
 	public static double estimateSrtmElevation(String pathToTiles, Coord c){
 		pathToTiles += pathToTiles.endsWith("/") ? "" : "/";
 		String tileName = getSrtmTile(c);
 		String tileFileName = pathToTiles + tileName + ".hgt";
 		File tileFile = new File(tileFileName);
 		
 		/* Download the tile file if it does not exist. */
 		if(!tileFile.exists()){
 			LOG.warn("Tile " + tileFileName + " is not available locally. Downloading...");
 			Runtime rt = Runtime.getRuntime();
 			String url = SRTM_URL_AFRICA + tileName + ".hgt.zip";
 			try {
				Process p1 = rt.exec("curl -o " + tileFileName + ".zip " + url);
				while(p1.isAlive()){ /* Wait */ }
				Process p2 = rt.exec("unzip " + tileFileName + ".zip -d " + pathToTiles);
				while(p2.isAlive()){ /* Wait */ }
				Process p3 = rt.exec("rm " + tileFileName + ".zip");
				while(p3.isAlive()){ /* Wait */ }
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not download SRTM tile file.");
			}
 		}
 		
 		/* Estimate the elevation. */
		SrtmTile srtmTile = new SrtmTile(tileFile);
 		return srtmTile.getElevation(c.getX(), c.getY());
 	}
 	
 	
 	
 	
 	
	

}
