/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreClusterRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.djcluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.FacilitiesUtils;

import playground.southafrica.freight.digicore.algorithms.djcluster.containers.DigicoreCluster;
import playground.southafrica.freight.digicore.algorithms.postclustering.ClusteredChainGenerator;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

/**
 * Class to cluster the activities of Digicore vehicles' activity chains using
 * the {@link DJCluster} approach. Once clustered, the activity chains are <i>not</i>
 * adjusted. Rather, the clustering outputs can be used as inputs to a class 
 * such as {@link ClusteredChainGenerator}. 
 *
 * @author jwjoubert
 */
@SuppressWarnings("deprecation")
public class DigicoreClusterRunner {
	private final static Logger LOG = Logger.getLogger(DigicoreClusterRunner.class);
	private final static int BLOCK_SIZE = 100; 
	
	private final int numberOfThreads;
	private Map<Id<MyZone>, List<Coord>> zoneMap = null;
	private ActivityFacilities facilities;

	/** 
	 * Clustering the minor activities from Digicore vehicle chains. The following
	 * parameters are required, and in the following order:
	 * <ol>
	 * 		<li> the input source. This may be an absolute path of the folder 
	 * 			 containing the Digicore vehicle files, in XML-format, or the
	 			 {@link DigicoreVehicles} container file. The former (XML folder)
	 			 is deprecated but still retained for backward compatibility.
	 * 		<li> The shapefile within which activities will be clustered. Activities
	 * 			 outside the shapefile are ignored. NOTE: It is actually recommended
	 * 			 that smaller demarcation areas, such as the Geospatial Analysis 
	 * 			 Platform (GAP) zones, be used.
	 * 		<li> Field of the shapefile that will be used as identifier;
	 * 		<li> Number of threads to use for the run;
	 * 		<li> Absolute path of the output folder to which the facilities, 
	 * 		     facility attributes, and the facility CSV file will be written.
	 * </ol>
	 * @param args
	 */
	public static void main(String[] args) {
		long jobStart = System.currentTimeMillis();
		Header.printHeader(DigicoreClusterRunner.class.toString(), args);

		String input = args[0];
		String shapefile = args[1];
		int idField = Integer.parseInt(args[2]);
		int numberOfThreads = Integer.parseInt(args[3]);
		String outputFolderName = args[4];		
		
		/* Read all the `minor' DigicoreActivities from the *.xml.gz Vehicle files. */
		LOG.info(" Reading points to cluster...");
		DigicoreClusterRunner dcr = new DigicoreClusterRunner(numberOfThreads);
		try {
			dcr.buildPointLists(input, shapefile, idField);
		} catch (IOException e) {
			throw new RuntimeException("Could not build minor points list.");
		}
		long readTime = System.currentTimeMillis() - jobStart;
		
		/* Cluster the points. */
		LOG.info("-------------------------------------------------------------");
		LOG.info(" Clustering the points...");
		
		/* These values should be set following Meintjes and Joubert, City Logistics paper? */
		double[] radii = {10}; ////, 10, 15, 20, 25, 30, 35, 40};
		int[] pmins = {10}; //, 10, 15, 20, 25};

		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				/* Just write some indication to the log file as to what we're 
				 * busy with at this point in time. */
				LOG.info("================================================================================");
				LOG.info("Executing clustering for radius " + thisRadius + ", and pmin of " + thisPmin);
				LOG.info("================================================================================");
				
				/* Create configuration-specific filenames. */
				String outputFolder = String.format("%s%.0f_%d/", outputFolderName, thisRadius, thisPmin);
				String theFacilityFile = outputFolder + String.format("%.0f_%d_facilities.xml.gz", thisRadius, thisPmin);
				String theFacilityAttributeFile = outputFolder + String.format("%.0f_%d_facilityAttributes.xml.gz", thisRadius, thisPmin);
				String theFacilityCsvFile = outputFolder + String.format("%.0f_%d_facilityCsv.csv.gz", thisRadius, thisPmin);
				String facilityPointFolder = String.format("%sfacilityPoints/", outputFolder);
				
				/* Create the output folders. If it exists... first delete it. */
				File folder = new File(outputFolder);
				if(folder.exists()){
					LOG.warn("Output folder exists, and will be deleted. ");
					LOG.warn("  --> " + folder.getAbsolutePath());
					FileUtils.delete(folder);
				}
				folder.mkdirs();
				
				/* Cluster. */
				try{
					dcr.clusterPointLists(thisRadius, thisPmin, facilityPointFolder);
				} catch (Exception e){
					e.printStackTrace();	
					throw new RuntimeException(e.getMessage());
				}
				
				/* Write output. */
				DJClusterUtils.writeOutput(dcr.facilities, theFacilityFile, theFacilityAttributeFile);
				DJClusterUtils.writePrettyCsv(dcr.facilities, theFacilityCsvFile);
			}
		}
		long clusterTime = System.currentTimeMillis() - jobStart - readTime;

		long totalTime = System.currentTimeMillis() - jobStart;
		LOG.info("-------------------------------------------------------------");
		LOG.info("  Done.");
		LOG.info("-------------------------------------------------------------");
		LOG.info("    Read time (s): " + readTime/1000);
		LOG.info(" Cluster time (s): " + clusterTime/1000);
		LOG.info("   Total time (s): " + totalTime/1000);
		LOG.info("=============================================================");

	}
	

	private void clusterPointLists(double radius, int minimumPoints, String outputFolder) throws Exception {
		File folder = new File(outputFolder);
		if(folder.exists()){
			LOG.warn("Facility points folder exists, and will be deleted. ");
			LOG.warn("  --> " + folder.getAbsolutePath());
			FileUtils.delete(folder);
		}
		folder.mkdirs();
		
		/* Check that zone maps have been read. */
		if(this.zoneMap == null){
			throw new Exception("Must first read activities before you can cluster!");
		}
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool(this.numberOfThreads);
		
		/* Break up the thread execution into blocks. */
		
		
		List<Future<List<DigicoreCluster>>> listOfJobs = new ArrayList<Future<List<DigicoreCluster>>>();
		
		Counter counter = new Counter("   Zones completed: ");
		/* Submit the clustering jobs to the different threads. */
		for(Id<MyZone> id : zoneMap.keySet()){			
			Callable<List<DigicoreCluster>> job = new DJClusterCallable(zoneMap.get(id), radius, minimumPoints, counter);
			Future<List<DigicoreCluster>> submit = threadExecutor.submit(job);
			listOfJobs.add(submit);
		}
			
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();

		this.facilities = DJClusterUtils.consolidateMultihtreadedOutput(radius, minimumPoints, outputFolder, listOfJobs);
		ActivityFacilitiesImpl r = ((ActivityFacilitiesImpl)facilities);
		LOG.info("    facility # " + r.getFacilities().size() );
	}


	/**
	 * Reads all activities from extracted Digicore vehicle files in a (possibly)
	 * multi-threaded manner. This used to only read in 'minor' points, but since
	 * July 2013, it now reads in <i>all</i> activity types.
	 * @param source
	 * @param shapefile
	 * @param idField
	 * @throws IOException
	 */
	private void buildPointLists(String source, String shapefile, int idField) throws IOException {
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		mfr.readMultizoneShapefile(shapefile, idField);
		List<MyZone> zoneList = mfr.getAllZones();
		
		/* Build a QuadTree of the Zones. */
		LOG.info(" Building QuadTree from zones...");
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(MyZone mz : zoneList){
			minX = Math.min(minX, mz.getEnvelope().getCoordinates()[0].x);
			maxX = Math.max(maxX, mz.getEnvelope().getCoordinates()[2].x);
			minY = Math.min(minY, mz.getEnvelope().getCoordinates()[0].y);
			maxY = Math.max(maxY, mz.getEnvelope().getCoordinates()[2].y);
		}
		QuadTree<MyZone> zoneQT = new QuadTree<MyZone>(minX, minY, maxX, maxY);
		for(MyZone mz : zoneList){
			zoneQT.put(mz.getEnvelope().getCentroid().getX(), mz.getEnvelope().getCentroid().getY(), mz);
		}
		LOG.info("Done building QuadTree.");
				
		/* Read the activities from vehicle files. If the input is a single 
		 * DigicoreVehicles file, then the single (V2) container will be read, 
		 * and each vehicle will be passed to the multi-threaded infrastructure. 
		 * Alternatively, if the input is a folder containing individual (V1) 
		 * DigicoreVehicle files, then they will be sampled, and each will be
		 * read by the multi-threaded infrastructure. */
		long startTime = System.currentTimeMillis();

		
		List<Object> vehicles = new ArrayList<>();
		File folder = new File(source);
		if(folder.isFile() && source.endsWith("xml.gz")){
			/* It is a V2 DigicoreVehicles container. */
			DigicoreVehicles dvs = new DigicoreVehicles();
			new DigicoreVehiclesReader(dvs).readFile(source);
			vehicles.addAll(dvs.getVehicles().values());
		} else if(folder.isDirectory()){
			/* It is a folder with individual V1 DigicoreVehicle files. */
			List<File> vehicleList = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter("xml.gz"));
			vehicles.addAll(vehicleList);
		}
		int inActivities = 0;
		int outActivities = 0;

		
		/* Set up the infrastructure so that threaded code is executed in blocks. */
		ExecutorService threadExecutor = null;
		List<DigicoreActivityReaderRunnable> threadList = null;
		int vehicleCounter = 0;
		Counter counter = new Counter("   Vehicles completed: ");

		/* Set up the output infrastructure:
		 * Create a new map with an empty list for each zone. These will be 
		 * passed to threads later. */
		zoneMap = new HashMap<Id<MyZone>, List<Coord>>();
		for(MyZone mz : zoneList){
			zoneMap.put(mz.getId(), new ArrayList<Coord>());
		}
		Map<Id<MyZone>, List<Coord>> theMap = null;
		
		while(vehicleCounter < vehicles.size()){
			int blockCounter = 0;
			threadExecutor = Executors.newFixedThreadPool(this.numberOfThreads);
			threadList = new ArrayList<DigicoreActivityReaderRunnable>();
			
			/* Assign the jobs in blocks. */
			while(blockCounter++ < BLOCK_SIZE && vehicleCounter < vehicles.size()){
				Object o = vehicles.get(vehicleCounter++);
				DigicoreActivityReaderRunnable rdar;
				if(o instanceof DigicoreVehicle){
					DigicoreVehicle vehicle = (DigicoreVehicle)o;
					rdar = new DigicoreActivityReaderRunnable(vehicle, zoneQT, counter);
				} else if(o instanceof File){
					// This is just kept for backward compatability.
					File vehicleFile = (File)o;
					rdar = new DigicoreActivityReaderRunnable(vehicleFile, zoneQT, counter);
				} else{
					throw new RuntimeException("Don't know what to do with a list with types " + o.getClass().toString());
				}

				threadList.add(rdar);
				threadExecutor.execute(rdar);
			}

			/* Shut down the thread executor for this block, and wait until it
			 * is finished before proceeding. */
			threadExecutor.shutdown();
			while(!threadExecutor.isTerminated()){
			}
			
			/* Aggregate the results of the current block. */
			/* Add all the coordinates from each vehicle to the main map. */
			for(DigicoreActivityReaderRunnable rdar : threadList){
				theMap = rdar.getMap();
				for(Id<MyZone> id : theMap.keySet()){
					zoneMap.get(id).addAll(theMap.get(id));
				}
				inActivities += rdar.getInCount();
				outActivities += rdar.getOutCount();			
			}		
		}
		counter.printCounter();
		
		long time = (System.currentTimeMillis() - startTime) / 1000;
		int totalPoints = inActivities + outActivities;
		LOG.info("Total number of activities checked: " + totalPoints);
		LOG.info("   In: " + inActivities);
		LOG.info("  Out: " + outActivities);
		LOG.info("Time (s): " + time);
	}

	public DigicoreClusterRunner(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		facilities = FacilitiesUtils.createActivityFacilities("Digicore facilities");
	}
	
	

}

