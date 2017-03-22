/* *********************************************************************** *
 * project: org.matsim.*
 * RunPointListClustering.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.freight.digicore.algorithms.djcluster.pointList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;

import playground.southafrica.freight.digicore.algorithms.djcluster.DJClusterCallable;
import playground.southafrica.freight.digicore.algorithms.djcluster.DJClusterUtils;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.DigicoreCluster;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;

/**
 * Clustering zones, using (possible) parallel infrastructure, from serialized
 * objects. 
 * 
 * @author jwjoubert
 */
public class RunPointListClustering {
	final private static Logger LOG = Logger.getLogger(RunPointListClustering.class);
	final private int numberOfThreads;
	final private File root;
	
	private ActivityFacilities facilities;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RunPointListClustering.class.toString(), args);
		
		String root = args[0];
		root += root.endsWith("/") ? "" : "/";
		double radius = Double.parseDouble(args[1]);
		int pmin = Integer.parseInt(args[2]);
		int numberOfThreads = Integer.parseInt(args[3]);
		
		RunPointListClustering run = new RunPointListClustering(root, numberOfThreads);
		run.cluster(radius, pmin);
		
		Header.printFooter();
	}
	
	public RunPointListClustering(String root, int numberOfThreads) {
		this.root = new File(root);
		if(!this.root.exists() & this.root.isDirectory()){
			throw new RuntimeException("The given root is not a directory: " + root);
		}
		this.numberOfThreads = numberOfThreads;
	}
	
	
	public void cluster(double radius, int pmin){
		LOG.info(String.format("Clustering (%.1f;%d)", radius, pmin));
		
		/* Create configuration-specific filenames. */
		String outputFolder = String.format("%s%.0f_%d/", 
				root.getAbsolutePath() + (root.getAbsolutePath().endsWith("/") ? "" : "/"), 
				radius, pmin);
		String theFacilityFile = outputFolder + String.format("%.0f_%d_facilities.xml.gz", radius, pmin);
		String theFacilityAttributeFile = outputFolder + String.format("%.0f_%d_facilityAttributes.xml.gz", radius, pmin);
		String theFacilityCsvFile = outputFolder + String.format("%.0f_%d_facilityCsv.csv.gz", radius, pmin);
		String facilityPointFolder = String.format("%sfacilityPoints/", outputFolder);
		
		/* Create the output folders. If it exists... first delete it. */
		File folder = new File(outputFolder);
		if(folder.exists()){
			LOG.warn("Output folder exists, and will be deleted. ");
			LOG.warn("  --> " + folder.getAbsolutePath());
			FileUtils.delete(folder);
		}
		folder.mkdirs();

		/* Deserialize the vehicle objects. */
		String serialPath = this.root.getAbsolutePath() + 
				(this.root.getAbsolutePath().endsWith("/") ? "" : "/") + 
				"serial/";
		PointListDeserializer pld = new PointListDeserializer(serialPath);
		Map<Id<MyZone>, List<Coord>> deserializedMap = pld.deserializeAll();
		
		/* Execute clustering on multi-threaded infrastructure. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<List<DigicoreCluster>>> listOfJobs = new ArrayList<Future<List<DigicoreCluster>>>();
		Counter counter = new Counter("   zone # ");
		for(Id<MyZone> zoneId : deserializedMap.keySet()){
			Callable<List<DigicoreCluster>> dj = new DJClusterCallable(deserializedMap.get(zoneId), radius, pmin, counter);
			Future<List<DigicoreCluster>> job = threadExecutor.submit(dj);
			listOfJobs.add(job);
		}
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		
		this.facilities = DJClusterUtils.consolidateMultihtreadedOutput(
				radius, pmin, facilityPointFolder, listOfJobs);
		
		LOG.info("    facility # " + facilities.getFacilities().size() );
		LOG.info("Done clustering.");
		
		/* Write the output to file. */
		DJClusterUtils.writeOutput(facilities, theFacilityFile, theFacilityAttributeFile);
		DJClusterUtils.writePrettyCsv(facilities, theFacilityCsvFile);
	}
	
}
