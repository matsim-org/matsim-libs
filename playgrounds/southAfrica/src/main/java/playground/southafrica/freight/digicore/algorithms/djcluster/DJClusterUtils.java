/* *********************************************************************** *
 * project: org.matsim.*
 * DJClusterUtils.java
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
package playground.southafrica.freight.digicore.algorithms.djcluster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.southafrica.freight.digicore.algorithms.concaveHull.ConcaveHull;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.ClusterActivity;
import playground.southafrica.freight.digicore.algorithms.djcluster.containers.DigicoreCluster;

/**
 * Utilities that are (re)used in different implementations of the clustering.
 * 
 * @author jwjoubert
 */
public class DJClusterUtils {
	final private static Logger LOG = Logger.getLogger(DJClusterUtils.class);

	public static ActivityFacilities consolidateMultihtreadedOutput(
			double radius, int minimumPoints, 
			String outputFolder,
			List<Future<List<DigicoreCluster>>> listOfJobs) {
		
		/* Create the folder where the individual facility files' cluster 
		 * points will be written to. * 
		 */
		File folder = new File(outputFolder);
		folder.mkdirs();
		
		ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities(
				String.format("Digicore clustered facilities: %.0f (radius); %d (pmin)",radius, minimumPoints));
		
		int i = 0;
		for(Future<List<DigicoreCluster>> future : listOfJobs){
			try {
				List<DigicoreCluster> list = future.get();
				for(DigicoreCluster dc : list){
					Id<ActivityFacility> facilityId = Id.create(i++, ActivityFacility.class);

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
							
							ActivityFacility af = facilities.getFactory().createActivityFacility(facilityId, dc.getCenterOfGravity());
							facilities.addActivityFacility(af);
							facilities.getFacilityAttributes().putAttribute(facilityId.toString(), "DigicoreActivityCount", String.valueOf(dc.getPoints().size()));
							facilities.getFacilityAttributes().putAttribute(facilityId.toString(), "concaveHull", hull);
						} else{
							LOG.debug("Facility " + facilityId.toString() + " is not added. Hull is an empty geometry!");
						}
					}
								
					/* First, remove duplicate points. 
					 * TODO Consider the UniqueCoordinateArrayFilter class from vividsolutions.*/
					List<Coord> coordList = new ArrayList<Coord>();
					for(ClusterActivity ca : dc.getPoints()){
						if(!coordList.contains(ca.getCoord())){
							coordList.add(ca.getCoord());
						}
					}

					/*TODO If we want to, we need to write all the cluster members out to file HERE. 
					 * Update (20130627): Or, rather write out the concave hull. */
					/* FIXME Consider 'not' writing the facilities to file, as 
					 * this takes up a HUGE amount of disk space (JWJ Nov '13) */
					String clusterFile = String.format("%s%.0f_%d_points_%s.csv.gz", outputFolder, radius, minimumPoints, facilityId.toString());
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
		return facilities;
	}

	public static void writeOutput(ActivityFacilities facilities, String theFacilityFile, String theFacilityAttributeFile) {
		/* Write (for the current configuration) facilities, and the attributes, to file. */
		LOG.info("-------------------------------------------------------------");
		LOG.info(" Writing the facilities to file: " + theFacilityFile);
		FacilitiesWriter fw = new FacilitiesWriter(facilities);
		fw.write(theFacilityFile);				
		LOG.info(" Writing the facility attributes to file: " + theFacilityAttributeFile);
		ObjectAttributesXmlWriter ow = new ObjectAttributesXmlWriter(facilities.getFacilityAttributes());
		ow.putAttributeConverter(Point.class, new HullConverter());
		ow.putAttributeConverter(LineString.class, new HullConverter());
		ow.putAttributeConverter(Polygon.class, new HullConverter());
		ow.writeFile(theFacilityAttributeFile);
	}



	public static void writePrettyCsv(ActivityFacilities facilities, String theFacilityCsvFile) {
		/* Write out pretty CSV file. */
		LOG.info(" Writing the facilities to csv: " + theFacilityCsvFile);
		BufferedWriter bw = IOUtils.getBufferedWriter(theFacilityCsvFile);
		try{
			bw.write("Id,Long,Lat,Count");
			bw.newLine();
			for(Id<ActivityFacility> id : facilities.getFacilities().keySet()){
				ActivityFacility af = facilities.getFacilities().get(id);
				bw.write(id.toString());
				bw.write(",");
				bw.write(String.format("%.1f,%.1f,", af.getCoord().getX(), af.getCoord().getY()));
				bw.write(String.valueOf(facilities.getFacilityAttributes().getAttribute(id.toString(), "DigicoreActivityCount")));
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
	

	
}
