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
package org.matsim.contrib.taxibus.algorithm.optimizer.prebooked.clustered;

import java.util.*;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.taxibus.algorithm.optimizer.TaxibusOptimizer;
import org.matsim.contrib.taxibus.algorithm.optimizer.prebooked.PrebookedTaxibusOptimizerContext;
import org.matsim.contrib.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ClusteringTaxibusOptimizer implements TaxibusOptimizer {

	final PrebookedTaxibusOptimizerContext context;
	final Collection<TaxibusRequest> unplannedRequests;
	final Random r = MatsimRandom.getLocalInstance();
	final RequestDispatcher dispatcher;

	/**
	 * 
	 */
	public ClusteringTaxibusOptimizer(PrebookedTaxibusOptimizerContext context, RequestDispatcher dispatcher) {
		this.context = context;
		this.unplannedRequests = new HashSet<>();
		this.dispatcher = dispatcher;
	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {

	}

	@Override
	public void requestSubmitted(Request request) {
		if (context.requestDeterminator.isRequestServable(request)) {
			// Logger.getLogger(getClass()).info("Submitting " + request);
			this.unplannedRequests.add((TaxibusRequest)request);
		}
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		context.scheduler.updateBeforeNextTask(schedule);
		schedule.nextTask();
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if ((e.getSimulationTime() % (context.clustering_period_min * 60)) == 0) {
			Set<TaxibusRequest> dueRequests = new HashSet<>();
			for (TaxibusRequest r : unplannedRequests) {
				if (e.getSimulationTime() >= r.getEarliestStartTime() - context.prebook_period_min * 60) {
					dueRequests.add(r);
				}
			}
			unplannedRequests.removeAll(dueRequests);
			if (dueRequests.size() > 0) {
				List<Set<TaxibusRequest>> filteredRequests = context.requestFilter.prefilterRequests(dueRequests);
				Set<Set<TaxibusRequest>> clusteredRides = clusterRequests(filteredRequests);
				for (Set<TaxibusRequest> cluster : clusteredRides) {
					TaxibusDispatch dispatch = dispatcher.createDispatch(cluster);
					if (dispatch != null) {
						context.scheduler.scheduleRequest(dispatch);
					} else {
						for (TaxibusRequest r : cluster) {
							this.unplannedRequests.add(r);
						}
					}

				}
			}
		}
	}

	/**
	 * @return
	 */
	private Set<Set<TaxibusRequest>> clusterRequests(List<Set<TaxibusRequest>> prefilteredDueRequests) {
		HashSet<Set<TaxibusRequest>> dispatchSet = new HashSet<>();
		for (Set<TaxibusRequest> dueRequests : prefilteredDueRequests) {
			HashSet<Set<TaxibusRequest>> bestSet = new HashSet<>();
			HashSet<Request> bestSetLeftOvers = new HashSet<>();
			double bestDispatchScore = Double.MAX_VALUE;
			for (int i = 0; i < context.clusteringRounds; i++) {
				HashSet<Set<TaxibusRequest>> currentSet = new HashSet<>();

				List<Request> allOpenRequests = new ArrayList<>();
				allOpenRequests.addAll(dueRequests);
				Collections.shuffle(allOpenRequests, r);
				int vehiclesOnDispatch = (int)Math.min(context.vehiclesAtSameTime,
						Math.max(1, allOpenRequests.size() / context.minOccupancy));
				// Logger.getLogger(getClass()).info("vehicles on dispatch "+ vehiclesOnDispatch+" requests "+
				// allOpenRequests);
				int occupancy = Math.min((allOpenRequests.size() / vehiclesOnDispatch), context.capacity);
				for (int c = 0; c < vehiclesOnDispatch; c++) {
					Set<TaxibusRequest> currentBus = new HashSet<>();
					for (int o = 0; o < occupancy; o++) {
						if (!allOpenRequests.isEmpty()) {
							currentBus.add((TaxibusRequest)allOpenRequests.remove(r.nextInt(allOpenRequests.size())));
						}
					}

					currentSet.add(currentBus);
				}

				for (Set<TaxibusRequest> bus : currentSet) {
					if (!allOpenRequests.isEmpty()) {
						if (bus.size() < context.capacity) {
							bus.add((TaxibusRequest)allOpenRequests.remove(r.nextInt(allOpenRequests.size())));
						} else {
							continue;
						}
					}
				}
				// scoreSet
				double score = scoreSet(currentSet);
				if (score < bestDispatchScore) {
					bestDispatchScore = score;
					bestSet = currentSet;
					bestSetLeftOvers.clear();
					bestSetLeftOvers.addAll(allOpenRequests);
				}
			}
			dispatchSet.addAll(bestSet);
			for (Request r : bestSetLeftOvers) {
				unplannedRequests.add((TaxibusRequest)r);
			}
		}

		return dispatchSet;
	}

	/**
	 * @param currentSet
	 * @return
	 */
	private double scoreSet(HashSet<Set<TaxibusRequest>> currentSet) {
		double score = 0;
		for (Set<TaxibusRequest> bus : currentSet) {
			double cscore = 0;
			double xFromMed = 0;
			double yFromMed = 0;
			double xToMed = 0;
			double yToMed = 0;
			for (TaxibusRequest r : bus) {
				xFromMed += r.getFromLink().getCoord().getX();
				yFromMed += r.getFromLink().getCoord().getY();
				xToMed += r.getToLink().getCoord().getX();
				yToMed += r.getToLink().getCoord().getY();
			}
			Coord fromCentroid = new Coord(xFromMed / bus.size(), yFromMed / bus.size());
			Coord toCentroid = new Coord(xToMed / bus.size(), yToMed / bus.size());
			for (TaxibusRequest r : bus) {
				cscore += DistanceUtils.calculateSquaredDistance(r.getFromLink().getCoord(), fromCentroid);
				cscore += DistanceUtils.calculateSquaredDistance(r.getToLink().getCoord(), toCentroid);
			}
			cscore = cscore / (double)bus.size();
			score += cscore;
		}
		return score;
	}

}
