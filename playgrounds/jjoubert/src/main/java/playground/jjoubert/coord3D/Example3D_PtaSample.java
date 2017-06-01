/* *********************************************************************** *
 * project: org.matsim.*
 * RunCoord3D.java                                                                        *
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
import java.util.Map;

import org.apache.log4j.Logger;
import org.jzy3d.maths.Coord3d;
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
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.osmtools.srtm.SrtmTile;

import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class Example3D_PtaSample {
	final private static Logger LOG = Logger.getLogger(Coord3d.class);
	private static String path;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Example3D_PtaSample.class.toString(), args);
		
		path = args[0];
		path += path.endsWith("/") ? "" : "/";
		
		trySrtmOnPtaSample("full");
		trySrtmOnPtaSample("clean");
		
		Header.printFooter();
	}
	
	
	
	private static void trySrtmOnPtaSample(String network){
		LOG.info("Applying SRTM to Pretoria sample...");
		CoordinateTransformation coordinateTransformer = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");

		/* Parse MATSim network. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(path + "data/networks/pta_" + network + ".xml.gz");
		
		/* Build the regular grid. */
		buildElevationGrid(sc.getNetwork(), -5.0);
		
		/* Set up SRTM tile. */
		Coord blc = Utils3D.getBottomLeftCoordinate(sc.getNetwork());
		LOG.info("Bottom-left corner: " + blc.toString());
		String tileName = playground.southafrica.utilities.coord3D.Utils3D.getSrtmTile(blc);
		File srtmFile = new File(path + "data/tiles/" + tileName + ".hgt");
		if(!srtmFile.exists() || !srtmFile.canRead()){
			LOG.error("Cannot read SRTM tile from " + srtmFile.getAbsolutePath());
		} else{
			LOG.info("Can read SRTM file from " + srtmFile.getAbsolutePath());
		}
		SrtmTile srtmTile = new SrtmTile(srtmFile);
		int nodesWithElevation = 0;
		for(Node n : sc.getNetwork().getNodes().values()){
			Coord c = coordinateTransformer.transform(n.getCoord());
			try{
				double elevation = srtmTile.getElevation(c.getX(), c.getY());
				n.setCoord(CoordUtils.createCoord(c.getX(), c.getY(), elevation));
				nodesWithElevation++;
			} catch(Exception e){
				/* Ignore the node. */
			}
		}
		LOG.info(String.format("Total number of nodes with elevation: %d (%.1f%%)", nodesWithElevation, ((double)nodesWithElevation) / ((double)sc.getNetwork().getNodes().size())*100));
		
		/* Writing elevation data to file. */
		BufferedWriter bw = IOUtils.getBufferedWriter(path + "data/pta_" + network + "_elevation.csv.gz");
		Counter counter = new Counter("  link # ");
		ObjectAttributes linkAttributes = new ObjectAttributes();
		try{
			bw.write("lid,fx,fy,fz,tx,ty,tz,length,grade");
			bw.newLine();
			for(Link l : sc.getNetwork().getLinks().values()){
				Coord cf = l.getFromNode().getCoord();
				Coord cft = coordinateTransformer.transform(cf);
				Coord ct = l.getToNode().getCoord();
				Coord ctt = coordinateTransformer.transform(ct);
				
				double grade = Utils3D.calculateGrade(l);
				linkAttributes.putAttribute(l.getId().toString(), "grade", grade);
				
				String line = String.format("%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.1f,%.6f\n", 
						l.getId().toString(),
						cft.getX(), cft.getY(), cf.getZ(),
						ctt.getX(), ctt.getY(), ct.getZ(),
						l.getLength(),
						grade);
				bw.write(line);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to elevation file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close elevation file.");
			}
		}
		counter.printCounter();
		
		/* Next I want to see what the grade difference between two consecutive 
		 * links are. I am, though, ignoring return trips. That is, U-turns.
		 */
		LOG.info("Calculating grade differences...");
		BufferedWriter bwGrade = IOUtils.getBufferedWriter(path + "data/pta_" + network + "_gradeChange.csv.gz");
		counter = new Counter("  nodes # ");
		try{
			bwGrade.write("fromLink,toLink,gradeChange");
			bwGrade.newLine();
			for(Node n : sc.getNetwork().getNodes().values()){
				Map<Id<Link>, ? extends Link> inLinks = n.getInLinks();
				Map<Id<Link>, ? extends Link> outLinks = n.getOutLinks();
				for(Link in : inLinks.values()){
					for(Link out : outLinks.values()){
						if(in.getFromNode().getId() != out.getToNode().getId()){
							bwGrade.write(String.format("%s,%s,%.6f\n", 
									in.getId().toString(), 
									out.getId().toString(),
									(double)linkAttributes.getAttribute(out.getId().toString(), "grade") - (double)linkAttributes.getAttribute(in.getId().toString(), "grade")));
						}
					}
				}
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to grade change file.");
		} finally{
			try {
				bwGrade.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close grade change file.");
			}
		}
		counter.printCounter();
		
		LOG.info("Done with Pretoria sample.");
	}
	
		
	/**
	 * Build a regular grid of elevation points 
	 * @param network the given network (using a projected coordinate reference system);
	 * @param gridSize in metres;
	 * @param delta the amount (in metres) by which the calculated elevation should be offset; 
	 */
	private static void buildElevationGrid(Network network, double delta){
		LOG.info("Generating a regular grid for the network...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		double gridSize = 0.001;

		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		for(Node n : network.getNodes().values()){
			Coord c = ct.transform(n.getCoord());
			xMin = Math.min(xMin, c.getX());
			xMax = Math.max(xMax, c.getX());
			yMin = Math.min(yMin, c.getY());
			yMax = Math.max(yMax, c.getY());
		}

		/* Set up SRTM tile. */
		Coord blc = Utils3D.getBottomLeftCoordinate(network);
		LOG.info("Bottom-left corner: " + blc.toString());
		String tileName = playground.southafrica.utilities.coord3D.Utils3D.getSrtmTile(blc);
		File srtmFile = new File(path + "data/tiles/" + tileName + ".hgt");
		if(!srtmFile.exists() || !srtmFile.canRead()){
			LOG.error("Cannot read SRTM tile from " + srtmFile.getAbsolutePath());
		} else{
			LOG.info("Can read SRTM file from " + srtmFile.getAbsolutePath());
		}
		SrtmTile srtmTile = new SrtmTile(srtmFile);

		/* Calculate the elevation of each grid cell, and write to file */
		Counter counter = new Counter("  cells # ");
		BufferedWriter bw = IOUtils.getBufferedWriter(path + "/data/pta_grid.csv.gz");
		try{
			bw.write("x,y,z");
			bw.newLine();
			for(double x = xMin-0.5*gridSize; x <= xMax+0.5*gridSize; x+=gridSize){
				for(double y = yMin-0.5*gridSize; y <= yMax+0.5*gridSize; y+=gridSize){
					double z = srtmTile.getElevation(x, y);
					bw.write(String.format("%.5f,%.5f,%.0f\n", x, y, z+delta));
					
					counter.incCounter();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to grid file.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close grid file.");
			}
		}
		counter.printCounter();
		LOG.info("Done with grid.");
	}

}
