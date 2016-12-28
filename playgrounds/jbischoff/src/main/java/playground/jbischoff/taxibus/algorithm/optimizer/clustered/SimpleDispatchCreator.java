/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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
package playground.jbischoff.taxibus.algorithm.optimizer.clustered;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.router.Dijkstra;

import playground.jbischoff.drt.scheduler.tasks.DrtTask;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class SimpleDispatchCreator implements RequestDispatcher {
	private final ClusteringTaxibusOptimizerContext context;
	private final Dijkstra router;
	
	/**
	 * 
	 */
	public SimpleDispatchCreator(ClusteringTaxibusOptimizerContext context ) {
		this.context=context;
		this.router = new Dijkstra(context.scenario.getNetwork(), context.travelDisutility, context.travelTime);
	}
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.taxibus.algorithm.optimizer.clustered.RequestDispatcher#createDispatch(java.util.Set)
	 */
	@Override
	public TaxibusDispatch createDispatch(Set<TaxibusRequest> commonRequests) {
		Coord requestCentroid = calcRequestCentroid(commonRequests);
		Vehicle veh = findClosestIdleVehicle(requestCentroid);
		
		
		return createDispatchForVehicle(commonRequests,veh);
	}

	/**
	 * @param commonRequests
	 * @param veh
	 * @return
	 */
	private TaxibusDispatch createDispatchForVehicle(Set<TaxibusRequest> commonRequests, Vehicle veh) {
		ArrayList<TaxibusRequest> bestPickupOrder = null;
		ArrayList<TaxibusRequest> bestDropoffOrder = null;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0 ; i< context.clusteringRounds;i++){
			double distance = 0;

			ArrayList<TaxibusRequest> currentPickupOrder	= new ArrayList<>();
			currentPickupOrder.addAll(commonRequests);
			Collections.shuffle(currentPickupOrder);

			ArrayList<TaxibusRequest> currentDropoffOrder	= new ArrayList<>();
			currentDropoffOrder.addAll(commonRequests);
			Collections.shuffle(currentDropoffOrder);
			
			Coord lastCoord = veh.getStartLink().getCoord();
			for (TaxibusRequest r : currentPickupOrder){
				distance += DistanceUtils.calculateDistance(r.getFromLink().getCoord(),lastCoord);
				lastCoord = r.getFromLink().getCoord();
			}
			
			for (TaxibusRequest r : currentDropoffOrder){
				distance += DistanceUtils.calculateDistance(r.getToLink().getCoord(),lastCoord);
				lastCoord = r.getToLink().getCoord();
			}
			
			
			if(distance<bestDistance){
				bestPickupOrder = currentPickupOrder;
				bestDropoffOrder = currentDropoffOrder;
			}
			
		}
		Iterator<TaxibusRequest> it = bestPickupOrder.iterator();
		TaxibusRequest lastPU = it.next();
		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(veh.getStartLink(), lastPU.getFromLink(), context.timer.getTimeOfDay(), router, context.travelTime);
		TaxibusDispatch dispatch = new TaxibusDispatch(veh, lastPU, path);
		while (it.hasNext()){
			TaxibusRequest currentPU = it.next();
			dispatch.addRequestAndPath(currentPU, VrpPaths.calcAndCreatePath(lastPU.getFromLink(), currentPU.getFromLink(), currentPU.getT0(), router, context.travelTime));
			lastPU = currentPU;
		}
		
		Iterator<TaxibusRequest> dropoffs = bestDropoffOrder.iterator();
		TaxibusRequest lastDO = dropoffs.next();
		dispatch.addPath(VrpPaths.calcAndCreatePath(lastPU.getFromLink(), lastDO.getToLink(), lastPU.getT0(), router, context.travelTime));
		while (dropoffs.hasNext()){
			
			TaxibusRequest currentDO = dropoffs.next();
			dispatch.addPath(VrpPaths.calcAndCreatePath(lastDO.getToLink(), currentDO.getToLink(), currentDO.getT0(), router, context.travelTime));
			lastDO = currentDO;
		}
		dispatch.addPath(VrpPaths.calcAndCreatePath(lastDO.getToLink(), veh.getStartLink(), lastDO.getT0(), router, context.travelTime));
		return dispatch;
	}

	/**
	 * @param coord
	 * @return
	 */
	private Vehicle findClosestIdleVehicle(Coord coord) {
		double bestDistance = Double.MAX_VALUE;
		Vehicle bestVehicle = null;
		for (Vehicle veh : this.context.vrpData.getVehicles().values()){
			if (context.scheduler.isIdle(veh)){
				double distance = DistanceUtils.calculateSquaredDistance(veh.getStartLink().getCoord(),coord);
				if (distance<bestDistance){
					bestDistance=distance;
					bestVehicle = veh;
				}
				
			}
	
			}
		
		return bestVehicle;
	}

	/**
	 * @param commonRequests 
	 * @return
	 */
	private Coord calcRequestCentroid(Set<TaxibusRequest> commonRequests) {
		Set<Coord> coords = new HashSet<>();
		for (TaxibusRequest r : commonRequests){
			coords.add(r.getFromLink().getCoord());
		}
		return JbUtils.getCoordCentroid(coords);
	}

}
