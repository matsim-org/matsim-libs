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
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.matrixbasedptrouter.config.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.utils.MyBoundingBox;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

/**
 * <li> yyyy walk does not use the beeline distance although it should. kai, jul'13
 * </ul>
 *
 * @author thomas
 *
 */
public class PtMatrix {

	public static final Logger log = Logger.getLogger(PtMatrix.class);

	public static PtMatrix createPtMatrix(
			PlansCalcRouteConfigGroup plansCalcRoute, MyBoundingBox bb,
			MatrixBasedPtRouterConfigGroup ippcm) {

		String ptStopInputFile = ippcm.getPtStopsInputFile();
		QuadTree<PtStop> ptStops = PtStops.readPtStops(ptStopInputFile, bb);
		
		// init od matrix containing the travel times
		
		if(ippcm.getPtTravelTimesInputFile() == null || ippcm.getPtTravelDistancesInputFile() == null){
			// (yyyy wrong test; there is now a switch. kai, jul'13)
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
					double distance = CoordUtils.calcDistance(originCoord, destinationCoord) * plansCalcRoute.getBeelineDistanceFactor();
			
					double travelTime = distance / plansCalcRoute.getTeleportedModeSpeeds().get(TransportMode.pt);
			
					// create entry - travel times in seconds
					originDestinationTravelTimeMatrix.createEntry(originStop.getId(), destinationStop.getId(), travelTime);			
					// create entry - travel distances in meter
					originDestinationTravelDistanceMatrix.createEntry(originStop.getId(), destinationStop.getId(), distance);	
				}
			}
			log.info("Done creating OD matrices with pt stop to pt stop travel times and distances.");

			return new PtMatrix(plansCalcRoute, ptStops, originDestinationTravelTimeMatrix, originDestinationTravelDistanceMatrix);
		} else {
			Matrix originDestinationTravelTimeMatrix = new Matrix("PtStopTravelTimeMatrix", "Stop to stop origin destination travel time matrix");
			Matrix originDestinationTravelDistanceMatrix = new Matrix("PtStopTravelDistanceMatrix", "Stop to stop origin destination travel distance matrix");
			String ptTravelTimeInputFile = ippcm.getPtTravelTimesInputFile();
			String ptTravelDistanceInputFile = ippcm.getPtTravelDistancesInputFile();
			
			BufferedReader brTravelTimes = IOUtils.getBufferedReader(ptTravelTimeInputFile);
			PtMatrices.log.info("Creating travel time OD matrix from VISUM pt stop 2 pt stop travel times file: " + ptTravelTimeInputFile);
			try {
				PtMatrices.fillODMatrix(originDestinationTravelTimeMatrix, PtMatrices.convertQuadTree2HashMap(ptStops), brTravelTimes, true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			PtMatrices.log.info("Done creating travel time OD matrix. " + originDestinationTravelTimeMatrix.toString());
			
			PtMatrices.log.info("Creating travel distance OD matrix from VISUM pt stop 2 pt stop travel distance file: " + ptTravelDistanceInputFile);
			BufferedReader brTravelDistances = IOUtils.getBufferedReader(ptTravelDistanceInputFile);
			try {
				PtMatrices.fillODMatrix(originDestinationTravelDistanceMatrix, PtMatrices.convertQuadTree2HashMap(ptStops), brTravelDistances, false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			PtMatrices.log.info("Done creating travel distance OD matrix. " + originDestinationTravelDistanceMatrix.toString());
			
			PtMatrices.log.info("Done creating OD matrices with pt stop to pt stop travel times and distances.");

			return new PtMatrix(plansCalcRoute, ptStops, originDestinationTravelTimeMatrix, originDestinationTravelDistanceMatrix);
		}

	}

	private final Matrix originDestinationTravelTimeMatrix;
	private final Matrix originDestinationTravelDistanceMatrix;

	private final QuadTree<PtStop> ptStops;




	private double meterPerSecWalkSpeed;

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
		PtStop fromPtStop = this.ptStops.get(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.ptStops.get(toFacilityCoord.getX(), toFacilityCoord.getY());

		double walkTravelTimeFromFacility2FromPtStop = CoordUtils.calcDistance(fromFacilityCoord, fromPtStop.getCoord()) / meterPerSecWalkSpeed;
		double walkTravelTimeToPtStop2ToFacility = CoordUtils.calcDistance(toPtStop.getCoord(), toFacilityCoord) / meterPerSecWalkSpeed;

		double totalWalkTravelTime = walkTravelTimeFromFacility2FromPtStop + walkTravelTimeToPtStop2ToFacility;
		return totalWalkTravelTime;
	}

	public double getPtTravelTime_seconds(Coord fromFacilityCoord, Coord toFacilityCoord){

		// get pt stops
		PtStop fromPtStop = this.ptStops.get(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.ptStops.get(toFacilityCoord.getX(), toFacilityCoord.getY());

		Entry entry = originDestinationTravelTimeMatrix.getEntry(fromPtStop.getId(), toPtStop.getId());
		double ptTravelTime = Double.MAX_VALUE;
		if(entry != null)
			ptTravelTime = entry.getValue();
		else{
			// log.warn("No entry found in od travel times matrix for pt stops: " + fromPtStop.getId() + " " + toPtStop.getId());
			if(fromPtStop == toPtStop) {
				ptTravelTime = 0.;
			}
			else {
				throw new RuntimeException("Trying to route matrix based pt through a pair of od stops that is not contained in the travel time od matrix");
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

		PtStop fromPtStop = this.ptStops.get(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.ptStops.get(toFacilityCoord.getX(), toFacilityCoord.getY());

		double walkTravelDistanceFromFacility2FromPtStop = CoordUtils.calcDistance(fromFacilityCoord, fromPtStop.getCoord());
		double walkTravelDistanceToPtStop2ToFacility = CoordUtils.calcDistance(toPtStop.getCoord(), toFacilityCoord);

		double totalWalkTravelDistance = walkTravelDistanceFromFacility2FromPtStop + walkTravelDistanceToPtStop2ToFacility;
		return totalWalkTravelDistance;
	}


	public double getPtTravelDistance_meter(Coord fromCoord, Coord toCoord){

		PtStop fromPtStop = this.ptStops.get(fromCoord.getX(), fromCoord.getY());
		PtStop toPtStop   = this.ptStops.get(toCoord.getX(), toCoord.getY());

		Entry entry = originDestinationTravelDistanceMatrix.getEntry(fromPtStop.getId(), toPtStop.getId());
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

}
