/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsInformer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.mobsim;

import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.events.HouseholdInformationEventImpl;
import playground.christoph.evacuation.events.PersonInformationEventImpl;
import playground.christoph.evacuation.utils.DeterministicRNG;

/**
 * Informs the households which then start adapting their plans.
 * 
 * It is implemented as MobsimEngine since this simplifies the events handling in 
 * other classes. The informed events are created during the "doSimStep(...)" phase. 
 * The SimStepParallelEventsManager ensures, that all of them have been processed 
 * before the AfterStimStepListeners are called.
 * 
 * @author cdobler
 */
public class HouseholdsInformer implements MobsimEngine {

	/*package*/ EventsManager eventsManager;
	/*package*/ final int totalHouseholds;	// number of households with more than 0 members
	/*package*/ final Households households;
	/*package*/ final PriorityBlockingQueue<Tuple<Id, Double>> informationTime;
	/*package*/ final double sigma;
	/*package*/ final DeterministicRNG rng;
	
	public HouseholdsInformer(Households households, double sigma, long rngInitialValue) {
		
		this.households = households;
		this.sigma = sigma;
		this.rng = new DeterministicRNG(rngInitialValue);

		this.informationTime = new PriorityBlockingQueue<Tuple<Id, Double>>(500, new InformationTimeComparator());
		/*
		 * We ignore households with 0 members. Therefore we cannot use
		 * households.getHouseholds().size()
		 */
		int num = 0;
		for (Household household : households.getHouseholds().values()) if (household.getMemberIds().size() > 0) num++;
		totalHouseholds = num;
	}

	/*
	 * Define for each household when it is informed that an evacuation is
	 * required. So far this is done randomly. In the future this could also
	 * include information like the geographical positions of the households
	 * members.
	 */
	private void selectInformationTimes(Households households) {

		for (Household household : households.getHouseholds().values()) {
						
			double delay = calculateInformationDelay(household.getId());

			/*
			 * We have to add one second here. This ensure that some code which is executed
			 * at the end of a time step is executed when the simulation has started.
			 */
			this.informationTime.add(new Tuple<Id, Double>(household.getId(), EvacuationConfig.evacuationTime + delay + 1.0));
		}
	}

	/*
	 * So far use a Rayleigh Distribution with a sigma of 300. After 353s ~ 50%
	 * of all households have been informed.
	 */
	private final double upperLimit = 0.999999;

	private double calculateInformationDelay(Id householdId) {

		double rand = this.rng.idToRandomDouble(householdId);

		if (rand == 0.0) return 0.0;
		else if (rand > upperLimit) rand = upperLimit;

		return Math.floor(Math.sqrt(-2 * sigma*sigma * Math.log(1 - rand)));
	}

	/*
	 * Calculate each households information time.
	 */
	@Override
	public void onPrepareSim() {
		this.informationTime.clear();
		selectInformationTimes(households);
	}
	
	/*
	 * Mark households as informed.
	 */
	@Override
	public void doSimStep(double time) {
	
		while (this.informationTime.peek() != null) {
			Tuple<Id, Double> tuple = informationTime.peek();
			if (tuple.getSecond() <= time) {
				this.informationTime.poll();
				Household household = households.getHouseholds().get(tuple.getFirst());
				this.eventsManager.processEvent(new HouseholdInformationEventImpl(time, household.getId()));
				
				for (Id memberId : household.getMemberIds()) {
					this.eventsManager.processEvent(new PersonInformationEventImpl(time, memberId));
				}
			} else {
				break;
			}
		}
	}

	@Override
	public void afterSim() {
		// Nothing to do here so far.
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.eventsManager = ((QSim) internalInterface.getMobsim()).getEventsManager();
	}
	
	private class InformationTimeComparator implements Comparator<Tuple<Id, Double>>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Tuple<Id, Double> t1, Tuple<Id, Double> t2) {
			int cmp = Double.compare(t1.getSecond(), t2.getSecond());
			if (cmp == 0) {
				// Both are informed at the same time -> let the one with the larger id be first (=smaller)
				return t2.getFirst().compareTo(t1.getFirst());
			}
			return cmp;
		}
	}
}