/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeListGenerator.java
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
package playground.nmviljoen.digicore;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;

import edu.uci.ics.jung.graph.util.Pair;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.utilities.Header;

/**
 * Class to generate edge lists from empirical activity chains, using different
 * generation procedures.
 *  
 * @author jwjoubert
 */
public class EdgeListGenerator {
	final private static Logger LOG = Logger.getLogger(EdgeListGenerator.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(EdgeListGenerator.class.toString(), args);
		
		String vehiclesFile = args[0];
		String shapefile = args[1];
		String outputUniqueVehicles = args[2];
		String outputAllTrips = args[3];
		
		/* Parse the vehicles. */
		DigicoreVehicles dvs = new DigicoreVehicles("WGS84_SA_Albers");
		new DigicoreVehiclesReader(dvs).readFile(vehiclesFile);
		
		/* Parse the shapefile geometry. */
		ShapeFileReader sr = new ShapeFileReader();
		sr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("There are multiple features in the shapefile. Only the first is used!");
		}
		SimpleFeature feature = features.iterator().next();
		MultiPolygon area = null;
		Object o = feature.getDefaultGeometry();
		if(o instanceof MultiPolygon){
			area = (MultiPolygon) o;
		} else{
			LOG.warn("The object is not a MultiPolygon, but " + o.getClass().getName());
		}
		
		EdgeListGenerator.buildEdgeList(dvs, area, outputUniqueVehicles, outputAllTrips);
		
		Header.printFooter();
	}
	
	private static void buildEdgeList(DigicoreVehicles vehicles, Geometry area, String filenameUnique, String filenameAll){
		LOG.info("Building edge list for unique vehicle IDs...");
		Map<Pair<Id<ActivityFacility>>, List<Id<Vehicle>>> vehicleIds = new HashMap<Pair<Id<ActivityFacility>>, List<Id<Vehicle>>>();
		DigicoreNetwork dnUnique = new DigicoreNetwork();
		DigicoreNetwork dnAll = new DigicoreNetwork();
		
		Map<Id<ActivityFacility>, Boolean> inAreaMap = new HashMap<>();
		GeometryFactory gf = new GeometryFactory();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		int totalTripsConsidered = 0;
		int ignoredTrips = 0;
		
		Counter counter = new Counter("   vehicles # ");
		for(DigicoreVehicle vehicle : vehicles.getVehicles().values()){
			for(DigicoreChain chain : vehicle.getChains()){
				for(int i = 0; i < chain.size()-1; i++){
					DigicoreActivity o = chain.get(i);
					DigicoreActivity d = chain.get(i+1);
					if(o.getFacilityId() != null && d.getFacilityId() != null){
						/* Check if either of the two facilities are in the 
						 * study area. */
						Boolean oInArea = inAreaMap.get(o.getFacilityId());
						if(oInArea == null){
							boolean check = area.covers(gf.createPoint(new Coordinate(o.getCoord().getX(), o.getCoord().getY())));
							oInArea = check;
							inAreaMap.put(o.getFacilityId(), new Boolean(check));
						}
						Boolean dInArea = inAreaMap.get(d.getFacilityId());
						if(dInArea == null){
							boolean check = area.covers(gf.createPoint(new Coordinate(d.getCoord().getX(), d.getCoord().getY())));
							dInArea = check;
							inAreaMap.put(d.getFacilityId(), new Boolean(check));
						}

						if(oInArea || dInArea){
							/* The pair should be considered. */
							Pair<Id<ActivityFacility>> pair = new Pair<Id<ActivityFacility>>(o.getFacilityId(), d.getFacilityId());
							if(!vehicleIds.containsKey(pair)){
								vehicleIds.put(pair, new ArrayList<Id<Vehicle>>());
							}
							List<Id<Vehicle>> thisList = vehicleIds.get(pair);
							
							/* ================================================
							 * Check if the pair has already been serviced by the 
							 * specific vehicle. */
							if(thisList.contains(vehicle.getId())){
								/* Ignore the pair. This vehicle has already been
								 * accounted for. */
								ignoredTrips++;
							} else{
								/* It is the first time this vehicle services this
								 * activity pair. First add it to the list. */
								thisList.add(vehicle.getId());
								
								/* Add the link to the graph, or increment its weight. */
								dnUnique.addArc(o, d);
							}

							/* ================================================
							 * Add the trip anyway to the 'All' network. */
							dnAll.addArc(o, d);
							totalTripsConsidered++;
						}
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done building the networks.");
		LOG.info("Total number of trips considered: " + totalTripsConsidered);
		LOG.info("   Total number of trips ignored: " + ignoredTrips);
		
		/* Write the unique vehicle Id edge list to file. */
		LOG.info("Writing the 'unique vehicle' network...");
		BufferedWriter bw = IOUtils.getBufferedWriter(filenameUnique);
		try{
			bw.write("oId,oLon,oLat,oX,oY,dId,dLon,dLat,dX,dY,Weight");
			bw.newLine();
			
			for(Pair<Id<ActivityFacility>> edge : dnUnique.getEdges()){
				Id<ActivityFacility> oId = edge.getFirst();
				Coord oCoord = dnUnique.getCoordinates().get(oId);
				Id<ActivityFacility> dId = edge.getSecond();
				Coord dCoord = dnUnique.getCoordinates().get(dId);
				int weight = dnUnique.getEdgeWeight(oId, dId);
				
				Coord oWgs = ct.transform(oCoord);
				Coord dWgs = ct.transform(dCoord);
				
				String s = String.format("%s,%.6f,%.6f,%.0f,%.0f," + 
									     "%s,%.6f,%.6f,%.0f,%.0f,%d\n", 
									     oId.toString(),
									     oWgs.getX(), oWgs.getY(),
									     oCoord.getX(), oCoord.getY(),
									     dId.toString(),
									     dWgs.getX(), dWgs.getY(),
									     dCoord.getX(), dCoord.getY(),
									     weight);
				bw.write(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filenameUnique);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filenameUnique);
			}
		}
		
		/* Write the complete, 'all trips' edge list to file. */
		bw = IOUtils.getBufferedWriter(filenameAll);
		try{
			bw.write("oId,oLon,oLat,oX,oY,dId,dLon,dLat,dX,dY,Weight");
			bw.newLine();
			
			for(Pair<Id<ActivityFacility>> edge : dnAll.getEdges()){
				Id<ActivityFacility> oId = edge.getFirst();
				Coord oCoord = dnAll.getCoordinates().get(oId);
				Id<ActivityFacility> dId = edge.getSecond();
				Coord dCoord = dnAll.getCoordinates().get(dId);
				int weight = dnAll.getEdgeWeight(oId, dId);
				
				Coord oWgs = ct.transform(oCoord);
				Coord dWgs = ct.transform(dCoord);
				
				String s = String.format("%s,%.6f,%.6f,%.0f,%.0f," + 
						"%s,%.6f,%.6f,%.0f,%.0f,%d\n", 
						oId.toString(),
						oWgs.getX(), oWgs.getY(),
						oCoord.getX(), oCoord.getY(),
						dId.toString(),
						dWgs.getX(), dWgs.getY(),
						dCoord.getX(), dCoord.getY(),
						weight);
				bw.write(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filenameAll);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filenameAll);
			}
		}
	}
	
	

}
