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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.southafrica.freight.digicore.algorithms.concaveHull.ConcaveHull;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.ClusterActivity;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.DigicoreCluster;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class DigicoreClusterRunner {
	private static final Logger log = Logger.getLogger(DigicoreClusterRunner.class);
	
	private final int numberOfThreads;
	private Map<Id, List<Coord>> zoneMap = null;
	private ActivityFacilitiesImpl facilities;
	private ObjectAttributes facilityAttributes;

	/** 
	 * Clustering the minor activities from Digicore vehicle chains. The following
	 * parameters are required, and in the following order:
	 * <ol>
	 * 		<li> absolute path of the folder containing the Digicore vehicle files,
	 * 			 in XML-format;
	 * 		<li> the shapefile within which activities will be clustered. Activities
	 * 			 outside the shapefile are ignored;
	 * 		<li> field of the shapefile that will be used as identifier;
	 * 		<li> radius parameter, <code>r</code> for the density clustering algorithm;
	 * 		<li> minimum-number-of-points parameter, <code>p<SUB>e</SUB></code>, for 
	 * 			 the density clustering algorithm;
	 * 		<li> number of threads to use for the run;
	 * 		<li> absolute path of the output file containing the MATSim facilities;
	 * 		<li> absolute path of the output file containing the facility attributes; and
	 * 		<li> absolute path of the output file containing the facilities in CSV 
	 * 			 format for QGis.  
	 * </ol>
	 * @param args
	 */
	public static void main(String[] args) {
		long jobStart = System.currentTimeMillis();
		Header.printHeader(DigicoreClusterRunner.class.toString(), args);

		String sourceFolder = args[0];
		String shapefile = args[1];
		int idField = Integer.parseInt(args[2]);
		int numberOfThreads = Integer.parseInt(args[3]);
		String outputFolderName = args[4];		
		
		/* Read all the `minor' DigicoreActivities from the *.xml.gz Vehicle files. */
		log.info(" Reading points to cluster...");
		DigicoreClusterRunner dcr = new DigicoreClusterRunner(numberOfThreads);
		try {
			dcr.buildPointLists(sourceFolder, shapefile, idField);
		} catch (IOException e) {
			throw new RuntimeException("Could not build minor points list.");
		}
		long readTime = System.currentTimeMillis() - jobStart;
		
		/* Cluster the points. */
		log.info("-------------------------------------------------------------");
		log.info(" Clustering the points...");
		
		/* These values should be set following Quintin's Design-of-Experiment inputs. */
		double[] radii = {40, 35, 30, 25, 20, 15, 10};
		int[] pmins = {1, 5, 10, 15, 20, 25};

		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				/* Just write some indication to the log file as to what we're 
				 * busy with at this point in time. */
				log.info("================================================================================");
				log.info("Executing clustering for radius " + thisRadius + ", and pmin of " + thisPmin);
				log.info("================================================================================");
				
				
				/* Create configuration-specific filenames. */
				String outputFolder = String.format("%s%.0f_%d/", outputFolderName, thisRadius, thisPmin);
				String theFacilityFile = outputFolder + String.format("%.0f_%d_facilities.xml.gz", thisRadius, thisPmin);
				String theFacilityAttributeFile = outputFolder + String.format("%.0f_%d_facilityAttributes.xml.gz", thisRadius, thisPmin);
				String theFacilityCsvFile = outputFolder + String.format("%.0f_%d_facilityCsv.csv", thisRadius, thisPmin);
				String facilityPointFolder = String.format("%sfacilityPoints/", outputFolder);
				
				/* Create the output folders. If it exists... first delete it. */
				File folder = new File(outputFolder);
				if(folder.exists()){
					log.warn("Output folder exists, and will be deleted. ");
					log.warn("  --> " + folder.getAbsolutePath());
					FileUtils.delete(folder);
				}
				folder.mkdirs();
				
				/* Cluster. */
				dcr.facilities = new ActivityFacilitiesImpl(String.format("Digicore clustered facilities: %.0f (radius); %d (pmin)",thisRadius, thisPmin));
				dcr.facilityAttributes = new ObjectAttributes();
				try{
					dcr.clusterPointLists(thisRadius, thisPmin, facilityPointFolder);
				} catch (Exception e){
					e.printStackTrace();	
					throw new RuntimeException(e.getMessage());
				}
				
				/* Write output. */
				dcr.writeOutput(theFacilityFile, theFacilityAttributeFile);
				dcr.writePrettyCsv(theFacilityCsvFile);
			}
		}
		long clusterTime = System.currentTimeMillis() - jobStart - readTime;

		long totalTime = System.currentTimeMillis() - jobStart;
		log.info("-------------------------------------------------------------");
		log.info("  Done.");
		log.info("-------------------------------------------------------------");
		log.info("    Read time (s): " + readTime/1000);
		log.info(" Cluster time (s): " + clusterTime/1000);
		log.info("   Total time (s): " + totalTime/1000);
		log.info("=============================================================");

	}



	public void writeOutput(String theFacilityFile, String theFacilityAttributeFile) {
		/* Write (for the current configuration) facilities, and the attributes, to file. */
		log.info("-------------------------------------------------------------");
		log.info(" Writing the facilities to file: " + theFacilityFile);
		FacilitiesWriter fw = new FacilitiesWriter(facilities);
		fw.write(theFacilityFile);				
		log.info(" Writing the facility attributes to file: " + theFacilityAttributeFile);
		ObjectAttributesXmlWriter ow = new ObjectAttributesXmlWriter(facilityAttributes);
		ow.putAttributeConverter(Point.class, new HullConverter());
		ow.putAttributeConverter(LineString.class, new HullConverter());
		ow.putAttributeConverter(Polygon.class, new HullConverter());
		ow.writeFile(theFacilityAttributeFile);
	}



	public void writePrettyCsv(String theFacilityCsvFile) {
		/* Write out pretty CSV file. */
		log.info(" Writing the facilities to csv: " + theFacilityCsvFile);
		BufferedWriter bw = IOUtils.getBufferedWriter(theFacilityCsvFile);
		try{
			bw.write("Id,Long,Lat,Count");
			bw.newLine();
			for(Id id : this.facilities.getFacilities().keySet()){
				ActivityFacility af = this.facilities.getFacilities().get(id);
				bw.write(id.toString());
				bw.write(",");
				bw.write(String.format("%.1f,%.1f,", af.getCoord().getX(), af.getCoord().getY()));
				bw.write(String.valueOf(this.facilityAttributes.getAttribute(id.toString(), "DigicoreActivityCount")));
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	


	private void clusterPointLists(double radius, int minimumPoints, String outputFolder) throws Exception {
		File folder = new File(outputFolder);
		if(folder.exists()){
			log.warn("Facility points folder exists, and will be deleted. ");
			log.warn("  --> " + folder.getAbsolutePath());
			FileUtils.delete(folder);
		}
		folder.mkdirs();
		
		/* Check that zone maps have been read. */
		if(this.zoneMap == null){
			throw new Exception("Must first read activities before you can cluster!");
		}
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool(this.numberOfThreads);
		List<Future<List<DigicoreCluster>>> listOfJobs = new ArrayList<Future<List<DigicoreCluster>>>();
		
		Counter counter = new Counter("   Zones completed: ");
		/* Submit the clustering jobs to the different threads. */
		for(Id id : zoneMap.keySet()){			
			Callable<List<DigicoreCluster>> job = new DigicoreClusterCallable(zoneMap.get(id), radius, minimumPoints, counter);
			Future<List<DigicoreCluster>> submit = threadExecutor.submit(job);
			listOfJobs.add(submit);
		}
			
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();

		int i = 0;
		for(Future<List<DigicoreCluster>> future : listOfJobs){
			try {
				List<DigicoreCluster> list = future.get();
				for(DigicoreCluster dc : list){
					Id facilityId = new IdImpl(i++);

					/* Construct the concave hull for the clustered points. */
					List<ClusterActivity> dcPoints = dc.getPoints();
					if(dcPoints.size() > 0){
						GeometryFactory gf = new GeometryFactory();
						Geometry[] ga = new Geometry[dcPoints.size()];
						for(int j = 0; j < dcPoints.size(); j++){
							ga[j] = gf.createPoint(new Coordinate(dcPoints.get(j).getCoord().getX(), dcPoints.get(j).getCoord().getY()));
						}
						
						GeometryCollection points = new GeometryCollection(ga, gf);
						
						ConcaveHull ch = new ConcaveHull(points, 10);
						Geometry hull = ch.getConcaveHull(facilityId.toString());
						
						/*FIXME For some reason there are empty hulls. For now 
						 * we are only creating facilities for those with a valid
						 * Geometry for a hull: point, line or polygon.*/
						if(!hull.isEmpty()){
							dc.setConcaveHull(hull);
							dc.setCenterOfGravity();
							
							facilities.createAndAddFacility(facilityId, dc.getCenterOfGravity());
							facilityAttributes.putAttribute(facilityId.toString(), "DigicoreActivityCount", String.valueOf(dc.getPoints().size()));
							facilityAttributes.putAttribute(facilityId.toString(), "concaveHull", hull);
						} else{
							log.debug("Facility " + facilityId.toString() + " is not added. Hull is an empty geometry!");
						}
					}
								
					/*TODO If we want to, we need to write all the cluster members out to file HERE. 
					 * Update (20130627): Or, rather write out the concave hull. */
					
					/* First, remove duplicate points. 
					 * TODO Consider the UniqueCoordinateArrayFilter class from vividsolutions.*/
					List<Coord> coordList = new ArrayList<Coord>();
					for(ClusterActivity ca : dc.getPoints()){
						if(!coordList.contains(ca.getCoord())){
							coordList.add(ca.getCoord());
						}
					}
					
					String clusterFile = String.format("%s%.0f_%d_points_%s.csv", outputFolder, radius, minimumPoints, facilityId.toString());
					BufferedWriter bw = IOUtils.getBufferedWriter(clusterFile);
					try{
						bw.write("Long,Lat");
						bw.newLine();
						for(Coord c : coordList){
							bw.write(String.format("%f, %f\n", c.getX(), c.getY()));
						}
					} catch (IOException e) {
						throw new RuntimeException("Could not write to " + clusterFile);
					} finally{
						try {
							bw.close();
						} catch (IOException e) {
							throw new RuntimeException("Could not close " + clusterFile);
						}
					}
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("InterruptedException caught in retieving thread results.");
			} catch (ExecutionException e) {
				throw new RuntimeException("ExecutionException caught in retieving thread results.");
			}				
		}
		
		facilities.printFacilitiesCount();	
	}


	/**
	 * Reads all activities from extracted Digicore vehicle files in a (possibly)
	 * multi-threaded manner. This used to only read in 'minor' points, but since
	 * July 2013, it now reads in <i>all</i> activity types.
	 * @param sourceFolder
	 * @param shapefile
	 * @param idField
	 * @throws IOException
	 */
	private void buildPointLists(String sourceFolder, String shapefile, int idField) throws IOException {
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		mfr.readMultizoneShapefile(shapefile, idField);
		List<MyZone> zoneList = mfr.getAllZones();
		
		/* Build a QuadTree of the Zones. */
		log.info(" Building QuadTree from zones...");
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(MyZone mz : zoneList){
			minX = Math.min(minX, mz.getEnvelope().getCoordinates()[0].x);
			maxX = Math.max(maxX, mz.getEnvelope().getCoordinates()[2].x);
			minY = Math.min(minY, mz.getEnvelope().getCoordinates()[0].y);
			maxY = Math.max(maxY, mz.getEnvelope().getCoordinates()[0].y);
		}
		QuadTree<MyZone> zoneQT = new QuadTree<MyZone>(minX, minY, maxX, maxY);
		for(MyZone mz : zoneList){
			zoneQT.put(mz.getEnvelope().getCentroid().getX(), mz.getEnvelope().getCentroid().getY(), mz);
		}
		log.info("Done building QuadTree.");
				
		/* Read the activities from vehicle files. */
		File folder = new File(sourceFolder);
		List<File> vehicleList = FileUtils.sampleFiles(folder, Integer.MAX_VALUE, FileUtils.getFileFilter("xml.gz"));
		int inActivities = 0;
		int outActivities = 0;
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool(this.numberOfThreads);
		
		long startTime = System.currentTimeMillis();
		List<DigicoreActivityReaderRunnable> threadList = new ArrayList<DigicoreActivityReaderRunnable>();
		
		//set a counter to keep track of number of threads that completed
		Counter counter = new Counter("   Vehicles completed: ");
		for(File vehicleFile : vehicleList){
			DigicoreActivityReaderRunnable rdar = new DigicoreActivityReaderRunnable(vehicleFile, zoneQT, counter);
			threadList.add(rdar);
			threadExecutor.execute(rdar);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		
		/* Get all the results. */
		log.info("Aggregating all vehicles' activity coordinates.");
		/* Create a new map with an empty list for each zone. These will be passed to threads later. */
		zoneMap = new HashMap<Id, List<Coord>>();
		for(MyZone mz : zoneList){
			zoneMap.put(mz.getId(), new ArrayList<Coord>());
		}
		Map<Id, List<Coord>> theMap = null;
		/* Add all the coordinates from each vehicle to the main map. */
		for(DigicoreActivityReaderRunnable rdar : threadList){
			theMap = rdar.getMap();
			for(Id id : theMap.keySet()){
				zoneMap.get(id).addAll(theMap.get(id));
			}
			inActivities += rdar.getInCount();
			outActivities += rdar.getOutCount();			
		}		
		
		long time = (System.currentTimeMillis() - startTime) / 1000;
		int totalPoints = inActivities + outActivities;
		log.info("Total number of activities checked: " + totalPoints);
		log.info("   In: " + inActivities);
		log.info("  Out: " + outActivities);
		log.info("Time (s): " + time);
	}

	public DigicoreClusterRunner(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		facilities = new ActivityFacilitiesImpl("Digicore facilities");
		facilityAttributes = new ObjectAttributes();
	}
	
	

}

