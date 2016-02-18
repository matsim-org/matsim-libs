/* *********************************************************************** *
 * project: org.matsim.													   *
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

/**
 * 
 */
package org.matsim.contrib.matrixbasedptrouter;


import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

/**
 *
 * Calculates travel times and distances from Coord to Coord based on a file containing transit stops with coordinates,
 * and, optionally, two files containing travel times and distances between each pair of stops. If files for travel times and distances are not provided,
 * they are calculated based on the teleportedModeSpeed of the pt mode, and the beelineDistanceFactor.
 * 
 * @author thomas
 *
 */
public final class PtMatrix {

	private static final Logger log = Logger.getLogger(PtMatrix.class);

	/**
	 * Creates an instance of this class. The config group contains the input file names, and whether speed/distance matrices are provided or not.
	 * Speed and beeline distance factors are taken from the router configuration.
	 * 
	 * A bounding box must be provided, which is used to filter the transit stop file while reading it. It is mandatory. If you just want to read the file, 
	 * take the bounding box of your study area.
	 * 
	 * The transit stop file must always be provided, although it technically should not be necessary when you provide
	 * distance and speed matrices. It seems to be used only to give out warnings if transit stops are encountered which are not in the 
	 * transit stop file.
	 * 
	 * The calculated access/egress times are used without beeline distance correction, even though the pt travel times and distances are.
	 * 
	 * Even though the parameters are taken from the teleportation router config, the teleportation router is not actually used to calculate 
	 * times and distances. I think this would be the more correct thing to do.
	 * 
	 */
	public static PtMatrix createPtMatrix(
			PlansCalcRouteConfigGroup plansCalcRoute, BoundingBox bb,
			MatrixBasedPtRouterConfigGroup ippcm) {

		String ptStopInputFile = ippcm.getPtStopsInputFile();
		QuadTree<PtStop> ptStops = FileUtils.readPtStops(ptStopInputFile, bb);

		if(ippcm.isUsingTravelTimesAndDistances()) {
			Matrix originDestinationTravelTimeMatrix = new Matrix("PtStopTravelTimeMatrix", "Stop to stop origin destination travel time matrix");
			Matrix originDestinationTravelDistanceMatrix = new Matrix("PtStopTravelDistanceMatrix", "Stop to stop origin destination travel distance matrix");
			String ptTravelTimeInputFile = ippcm.getPtTravelTimesInputFile();
			String ptTravelDistanceInputFile = ippcm.getPtTravelDistancesInputFile();

			BufferedReader brTravelTimes = IOUtils.getBufferedReader(ptTravelTimeInputFile);
			log.info("Creating travel time OD matrix from VISUM pt stop 2 pt stop travel times file: " + ptTravelTimeInputFile);
			final Map<Id<PtStop>, PtStop> ptStopsMap = PtMatrix.convertQuadTree2HashMap(ptStops);
			FileUtils.fillODMatrix(originDestinationTravelTimeMatrix, ptStopsMap, brTravelTimes, true);
			log.info("Done creating travel time OD matrix. " + originDestinationTravelTimeMatrix.toString());

			log.info("Creating travel distance OD matrix from VISUM pt stop 2 pt stop travel distance file: " + ptTravelDistanceInputFile);
			BufferedReader brTravelDistances = IOUtils.getBufferedReader(ptTravelDistanceInputFile);
			FileUtils.fillODMatrix(originDestinationTravelDistanceMatrix, ptStopsMap, brTravelDistances, false);
			log.info("Done creating travel distance OD matrix. " + originDestinationTravelDistanceMatrix.toString());

			log.info("Done creating OD matrices with pt stop to pt stop travel times and distances.");

			return new PtMatrix(plansCalcRoute, ptStops, originDestinationTravelTimeMatrix, originDestinationTravelDistanceMatrix);
		} else {
			// if travel times and distances are not provided by external files ...
			Matrix originDestinationTravelTimeMatrix = new Matrix("PtStopTravelTimeMatrix", "Stop to stop origin destination travel time matrix");
			Matrix originDestinationTravelDistanceMatrix = new Matrix("PtStopTravelDistanceMatrix", "Stop to stop origin destination travel distance matrix");

			PtStop ptStopIds[] = ptStops.values().toArray( new PtStop[0] );

			for(int origin = 0; origin < ptStopIds.length; origin++) {

				PtStop originStop = ptStopIds[origin];
				Coord originCoord = originStop.getCoord();

				for(int destination = 0; destination < ptStopIds.length; destination++) {

					PtStop destinationStop = ptStopIds[destination];
					Coord destinationCoord = destinationStop.getCoord();
					double distance = CoordUtils.calcEuclideanDistance(originCoord, destinationCoord)
							* plansCalcRoute.getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor() ;
//							* plansCalcRoute.getBeelineDistanceFactor();

					double travelTime = distance / plansCalcRoute.getTeleportedModeSpeeds().get(TransportMode.pt);

					// create entry - travel times in seconds
					originDestinationTravelTimeMatrix.createEntry(originStop.getId().toString(), destinationStop.getId().toString(), travelTime);			
					// create entry - travel distances in meter
					originDestinationTravelDistanceMatrix.createEntry(originStop.getId().toString(), destinationStop.getId().toString(), distance);	
				}
			}
			log.info("Done creating OD matrices with pt stop to pt stop travel times and distances.");

			return new PtMatrix(plansCalcRoute, ptStops, originDestinationTravelTimeMatrix, originDestinationTravelDistanceMatrix);
		}

	}

	private final Matrix originDestinationTravelTimeMatrix;
	private final Matrix originDestinationTravelDistanceMatrix;
	private final QuadTree<PtStop> ptStops;
	private final double meterPerSecWalkSpeed;

	private PtMatrix(PlansCalcRouteConfigGroup plansCalcRoute, QuadTree<PtStop> ptStops, Matrix originDestinationTravelTimeMatrix, Matrix originDestinationTravelDistanceMatrix){
		this.meterPerSecWalkSpeed = plansCalcRoute.getTeleportedModeSpeeds().get(TransportMode.walk) ;
		this.ptStops = ptStops;		
		this.originDestinationTravelTimeMatrix = originDestinationTravelTimeMatrix;
		this.originDestinationTravelDistanceMatrix = originDestinationTravelDistanceMatrix;
	}

	/**
	 * total travel times (origin location > pt > destination location) in seconds. the travel times are composed as follows:
	 * 
	 * (O)-------(PT)===========(PT)--------(D)
	 * 
	 * O = origin location
	 * D = destination location
	 * PT= next pt station 
	 * 
	 * total travel times = walk travel time from origin O to next pt stop +
	 *                      pt travel time +
	 *                      walk travel time from destination pt stop to destination D
	 * 
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getTotalTravelTime_seconds(Coord fromFacilityCoord, Coord toFacilityCoord){

		double totalWalkTravelTime = getTotalWalkTravelTime_seconds(fromFacilityCoord, toFacilityCoord);
		// the above method is (to my taste) a bit oddly named and used; I would have used separate methods for
		// access and egress.  kai, dec'13

		double ptTravelTime = getPtTravelTime_seconds(fromFacilityCoord, toFacilityCoord);

		double totalTravelTime = totalWalkTravelTime + ptTravelTime;
		return totalTravelTime;
	}

	/**
	 * returns the total walk travel times in seconds including
	 * - walk travel time from given coordinate to next pt stop
	 * - walk travel time from destination pt stop to given destination coordinate
	 * 
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getTotalWalkTravelTime_seconds(Coord fromFacilityCoord, Coord toFacilityCoord){	
		PtStop fromPtStop = this.ptStops.getClosest(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.ptStops.getClosest(toFacilityCoord.getX(), toFacilityCoord.getY());

		double walkTravelTimeFromFacility2FromPtStop = CoordUtils.calcEuclideanDistance(fromFacilityCoord, fromPtStop.getCoord()) / meterPerSecWalkSpeed;
		double walkTravelTimeToPtStop2ToFacility = CoordUtils.calcEuclideanDistance(toPtStop.getCoord(), toFacilityCoord) / meterPerSecWalkSpeed;

		double totalWalkTravelTime = walkTravelTimeFromFacility2FromPtStop + walkTravelTimeToPtStop2ToFacility;
		return totalWalkTravelTime;
	}

	public double getPtTravelTime_seconds(Coord fromFacilityCoord, Coord toFacilityCoord){

		// get pt stops
		PtStop fromPtStop = this.ptStops.getClosest(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.ptStops.getClosest(toFacilityCoord.getX(), toFacilityCoord.getY());

		Entry entry = originDestinationTravelTimeMatrix.getEntry(fromPtStop.getId().toString(), toPtStop.getId().toString());
		double ptTravelTime = Double.MAX_VALUE;
		if(entry != null)
			ptTravelTime = entry.getValue();
		else{
			// log.warn("No entry found in od travel times matrix for pt stops: " + fromPtStop.getId() + " " + toPtStop.getId());
			if(fromPtStop == toPtStop) {
				ptTravelTime = 0.;
			}
			else {
				throw new RuntimeException("Trying to route matrix based pt through a pair of od stops that is not contained in the"
						+ "travel time od matrix. fromPtStop = " + fromPtStop.getId() + " -- toPtStop = " + toPtStop.getId());
			}
		}
		return ptTravelTime;
	}

	/**
	 * total travel distance (origin location > pt > destination location) in seconds. the travel distances are composed as follows:
	 * 
	 * (O)-------(PT)===========(PT)--------(D)
	 * 
	 * O = origin location
	 * D = destination location
	 * PT= next pt station 
	 * 
	 * total travel distance = walk travel distance from origin O to next pt stop +
	 *                         pt travel distance +
	 *                         walk travel distance from destination pt stop to destination D
	 * 
	 * travel distances in meter
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getTotalTravelDistance_meter(Coord fromFacilityCoord, Coord toFacilityCoord){

		double totalWalkTravelDistance = getTotalWalkTravelDistance_meter(fromFacilityCoord, toFacilityCoord);
		double ptTravelDistance = getPtTravelDistance_meter(fromFacilityCoord, toFacilityCoord); 

		double totalTravelDistance = totalWalkTravelDistance + ptTravelDistance;
		return totalTravelDistance;
	}


	public double getTotalWalkTravelDistance_meter(Coord fromFacilityCoord, Coord toFacilityCoord){

		PtStop fromPtStop = this.ptStops.getClosest(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.ptStops.getClosest(toFacilityCoord.getX(), toFacilityCoord.getY());

		double walkTravelDistanceFromFacility2FromPtStop = CoordUtils.calcEuclideanDistance(fromFacilityCoord, fromPtStop.getCoord());
		double walkTravelDistanceToPtStop2ToFacility = CoordUtils.calcEuclideanDistance(toPtStop.getCoord(), toFacilityCoord);

		double totalWalkTravelDistance = walkTravelDistanceFromFacility2FromPtStop + walkTravelDistanceToPtStop2ToFacility;
		return totalWalkTravelDistance;
	}


	public double getPtTravelDistance_meter(Coord fromCoord, Coord toCoord){

		PtStop fromPtStop = this.ptStops.getClosest(fromCoord.getX(), fromCoord.getY());
		PtStop toPtStop   = this.ptStops.getClosest(toCoord.getX(), toCoord.getY());

		Entry entry = originDestinationTravelDistanceMatrix.getEntry(fromPtStop.getId().toString(), toPtStop.getId().toString());
		double ptTravelDistance = Double.MAX_VALUE;
		if(entry != null)
			ptTravelDistance = entry.getValue();
		else{
			// log.warn("No entry found in od travel distances matrix for pt stops: " + fromPtStop.getId() + " " + toPtStop.getId());
			if(fromPtStop == toPtStop)
				ptTravelDistance = 0.;
		}

		return ptTravelDistance;
	}

	private static Map<Id<PtStop>, PtStop> convertQuadTree2HashMap(QuadTree<PtStop> qTree){

		Iterator<PtStop> ptStopIterator = qTree.values().iterator();
		Map<Id<PtStop>, PtStop> ptStopHashMap = new ConcurrentHashMap<>();

		while(ptStopIterator.hasNext()){
			PtStop ptStop = ptStopIterator.next();
			ptStopHashMap.put(ptStop.getId(), ptStop);
		}
		return ptStopHashMap;
	}

}
