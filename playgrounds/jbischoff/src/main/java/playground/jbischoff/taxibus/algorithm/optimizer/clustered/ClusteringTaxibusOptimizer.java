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

import java.time.chrono.MinguoChronology;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.jbischoff.drt.scheduler.tasks.DrtTask;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ClusteringTaxibusOptimizer implements TaxibusOptimizer {

	final ClusteringTaxibusOptimizerContext context;
    final Collection<TaxibusRequest> unplannedRequests;
    final Random r = MatsimRandom.getLocalInstance();
    final RequestDispatcher dispatcher;
	/**
	 * 
	 */
	public ClusteringTaxibusOptimizer(ClusteringTaxibusOptimizerContext context, RequestDispatcher dispatcher) {
		this.context=context;
		this.unplannedRequests = new HashSet<>();
		this.dispatcher = dispatcher;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking#nextLinkEntered(org.matsim.contrib.dvrp.schedule.DriveTask)
	 */
	@Override
	public void nextLinkEntered(DriveTask driveTask) {
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dvrp.optimizer.VrpOptimizer#requestSubmitted(org.matsim.contrib.dvrp.data.Request)
	 */
	@Override
	public void requestSubmitted(Request request) {
		if (context.requestDeterminator.isRequestServable(request)){
//			Logger.getLogger(getClass()).info("Submitting " + request);
			this.unplannedRequests.add((TaxibusRequest) request);
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.dvrp.optimizer.VrpOptimizer#nextTask(org.matsim.contrib.dvrp.schedule.Schedule)
	 */
	@Override
	public void nextTask(Schedule<? extends Task> schedule) {
		@SuppressWarnings("unchecked")
		Schedule<DrtTask> taxibusSchedule = (Schedule<DrtTask>) schedule;
        context.scheduler.updateBeforeNextTask(taxibusSchedule);
        
        DrtTask newCurrentTask = taxibusSchedule.nextTask();		
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener#notifyMobsimBeforeSimStep(org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent)
	 */
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		if ((e.getSimulationTime() % (context.clustering_period_min*60))==0){
			Set<TaxibusRequest> dueRequests = new HashSet<>();
			for (TaxibusRequest r : unplannedRequests){
				if (e.getSimulationTime()>=r.getT0()-context.prebook_period_min*60){
					dueRequests.add(r);
				}
			}
			unplannedRequests.removeAll(dueRequests);
			if (dueRequests.size()>0){
			Set<Set<TaxibusRequest>> clusteredRides = clusterRequests(dueRequests);
			for (Set<TaxibusRequest> cluster : clusteredRides){
				TaxibusDispatch dispatch = dispatcher.createDispatch(cluster);
				context.scheduler.scheduleRequest(dispatch);
			}}
		}
	}

	/**
	 * @return
	 */
	private Set<Set<TaxibusRequest>> clusterRequests(Set<TaxibusRequest> dueRequests) {
		HashSet<Set<TaxibusRequest>> bestSet = new HashSet<>();
		double bestDispatchScore = Double.MAX_VALUE;
		
		for (int i = 0; i < context.clusteringRounds;i++){
			HashSet<Set<TaxibusRequest>> currentSet = new HashSet<>();
			List<Request> allOpenRequests = new ArrayList<>();
			allOpenRequests.addAll(dueRequests);
			Collections.shuffle(allOpenRequests, r);
			int vehiclesOnDispatch = (int) Math.min(context.vehiclesAtSameTime,Math.max(1, allOpenRequests.size()/context.minOccupancy));
//			Logger.getLogger(getClass()).info("vehicles on dispatch "+ vehiclesOnDispatch+" requests "+ allOpenRequests);
			int occupancy = (allOpenRequests.size()/vehiclesOnDispatch);
			for (int c = 0 ; c<vehiclesOnDispatch;c++){
				Set<TaxibusRequest> currentBus = new HashSet<>();
				for ( int o = 0; o<occupancy;o++){
					if (!allOpenRequests.isEmpty()){
					currentBus.add((TaxibusRequest) allOpenRequests.remove(r.nextInt(allOpenRequests.size())));
					}
					}
					
				currentSet.add(currentBus);
			}
			
			for (Set<TaxibusRequest> bus: currentSet){
				if (!allOpenRequests.isEmpty()){
					bus.add((TaxibusRequest) allOpenRequests.remove(r.nextInt(allOpenRequests.size())));
				}
			}
			//scoreSet
			double score = scoreSet(currentSet);
			if (score<bestDispatchScore){
				bestDispatchScore=score;
				bestSet=currentSet;
			}
		}
		
		
		

		return bestSet;
	}

	/**
	 * @param currentSet
	 * @return
	 */
	private double scoreSet(HashSet<Set<TaxibusRequest>> currentSet) {
		double score = 0;
		for (Set<TaxibusRequest> bus : currentSet){
			double cscore = 0;
			double xFromMed=0;
			double yFromMed=0;
			double xToMed=0;
			double yToMed=0;
			for (TaxibusRequest r: bus){
				xFromMed+=r.getFromLink().getCoord().getX();
				yFromMed+=r.getFromLink().getCoord().getY();
				xToMed+=r.getToLink().getCoord().getX();
				yToMed+=r.getToLink().getCoord().getY();
			}
			Coord fromCentroid = new Coord(xFromMed/bus.size(),yFromMed/bus.size());
			Coord toCentroid = new Coord(xToMed/bus.size(),yToMed/bus.size());
			for (TaxibusRequest r: bus){
				cscore += DistanceUtils.calculateSquaredDistance(r.getFromLink().getCoord(), fromCentroid);
				cscore += DistanceUtils.calculateSquaredDistance(r.getToLink().getCoord(), toCentroid);
			}
			cscore = cscore/(double)bus.size();
			score+=cscore;
		}
		return score;
	}
	

}
