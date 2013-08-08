/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.analysis.chain.chainSimilarity.geometric;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferBuilder;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

public class GeometricChainSimilarityAnalyser {
	final private static Logger LOG = Logger.getLogger(GeometricChainSimilarityAnalyser.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GeometricChainSimilarityAnalyser.class.toString(), args);
		
		String xmlFolder = args[0];
		String outputfolder = args[1];
		int numberOfThreads = Integer.parseInt(args[2]);
		
		double[] radii = {1, 5, 10, 15, 20, 25, 30, 35, 40};
		int[] pmins = {1, 5, 10, 15, 20, 25};
		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				/* Find configuration-specific filenames.*/
				String thisInput = String.format("%s%.0f_%d/xml2/", xmlFolder, thisRadius, thisPmin);
				String thisOutput = String.format("%s%.0f_%d/chainSimilarity/", xmlFolder, thisRadius, thisPmin);
				
				LOG.info("=======================================================================================");
				LOG.info(String.format(" Performing chain similarity analysis for radius %.0f and pmin %d", thisRadius, thisPmin));
				LOG.info("=======================================================================================");
				
				execute(thisInput, thisOutput, numberOfThreads);
			}
		}

		Header.printFooter();
	}
	
	private static void execute(String xmlFolder, String outputfolder,
			int numberOfThreads) {
		
		/* Check the existence of the output folder, delete if already there. */
		File output = new File(outputfolder);
		if(output.exists()){
			LOG.warn("Output folder exists and will be deleted... " + outputfolder);
			FileUtils.delete(output);
		}
		output.mkdirs();
		
		Counter counter = new Counter("   vehicles # ");
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		
		List<File> vehicleFiles = FileUtils.sampleFiles(new File(xmlFolder), numberOfThreads, FileUtils.getFileFilter("xml.gz"));
		for(File vehicleFile : vehicleFiles){
			GeometricChainSimilarityAnalysisRunnable job = new GeometricChainSimilarityAnalysisRunnable(vehicleFile, output, counter);
			threadExecutor.execute(job);
		}
	
		/* Terminate once finished. */
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
	}


	/**
	 * 
	 * @param chain
	 * @param d
	 * @return
	 */
	public static Polygon getBuffer(DigicoreChain chain, double d) {
		List<DigicoreActivity> list = chain.getAllActivities();
		GeometryFactory gf = new GeometryFactory();

		/* Build the linear string. */
		Coordinate[] ca = new Coordinate[list.size()];
		for(int i = 0; i < list.size(); i++){
			ca[i] = new Coordinate(list.get(i).getCoord().getX(), list.get(i).getCoord().getY());
		}
		LineString ls = gf.createLineString(ca);
		Polygon polygon = null;
		
		BufferParameters bp = new BufferParameters();
		bp.setEndCapStyle(BufferParameters.CAP_ROUND);
		bp.setJoinStyle(BufferParameters.JOIN_MITRE);
		bp.setSingleSided(false);
		BufferBuilder bb = new BufferBuilder(bp);
		
		Geometry g = ls.buffer(d, 50 );
		if(g instanceof Polygon){
			polygon = (Polygon) g;
		}
		
		return polygon;
	}

	
	/**
	 * Calculate the percentage overlap of two activity chains given a buffer
	 * area around the two activity chains.
	 * @param chain1
	 * @param chain2
	 * @param buffer
	 * @return the percentage of area overlap, calculated as the area of the 
	 * 		  intersection between the two chains' polygons, divided by the area
	 * 		  of the union of the two polygons.
	 */
	public static double getPercentageOverlap(DigicoreChain chain1, DigicoreChain chain2, double buffer){
		Geometry g1 = getBuffer(chain1, buffer);
		Geometry g2 = getBuffer(chain2, buffer);
		
		return getPercentageOfInterSectionToUnion(g1, g2);
	}
	
	
	/**
	 * Calculates the overlap percentage between two geometries as the area
	 * of overlap divided by the area of the union of the two geometries. This 
	 * class is mainly for testing purposes, and it is suggested you rather use
	 * {@link #getPercentageOverlap(DigicoreChain, DigicoreChain, double)}.
	 * @param g1
	 * @param g2
	 * @return
	 */
	public static double getPercentageOfInterSectionToUnion(Geometry g1, Geometry g2) {
		double unionArea = g1.union(g2).getArea();
		double intersectArea = g1.intersection(g2).getArea();
		return intersectArea / unionArea;
	}

}
