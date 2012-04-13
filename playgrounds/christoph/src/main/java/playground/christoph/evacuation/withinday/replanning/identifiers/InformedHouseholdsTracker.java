/* *********************************************************************** *
 * project: org.matsim.*
 * InformedHouseholdsTracker.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.utils.DeterministicRNG;

public class InformedHouseholdsTracker extends InformedAgentsTracker {

	/*package*/ final int totalHouseholds;	// number of households with more than 0 members
	/* package*/ final Households households;
	/* package*/ final DeterministicRNG rng;
	/* package*/ final Set<Id> informedHouseholds;
	/* package*/ final Queue<Id> informedHouseholdsInCurrentTimeStep;
	/* package*/ final PriorityBlockingQueue<Tuple<Id, Double>> informationTime;

	private boolean allHouseholdsInformed = false;
	
	public InformedHouseholdsTracker(Households households) {
		this.households = households;
		this.informedHouseholdsInCurrentTimeStep = new ConcurrentLinkedQueue<Id>();

		this.rng = new DeterministicRNG();
		this.informedHouseholds = new HashSet<Id>();
		this.informationTime = new PriorityBlockingQueue<Tuple<Id, Double>>(500, new InformationTimeComparator());

		/*
		 * We ignore households with 0 members. Therefore we cannot use
		 * households.getHouseholds().size()
		 */
		int num = 0;
		for (Household household : households.getHouseholds().values()) if (household.getMemberIds().size() > 0) num++;
		totalHouseholds = num;
		
		selectInformationTimes(households);
	}

	public Queue<Id> getInformedHouseholdsInCurrentTimeStep() {
		return informedHouseholdsInCurrentTimeStep;
	}

	public boolean allHouseholdsInformed() {
		return this.allHouseholdsInformed;
	}
	
	public boolean isHouseholdInformed(Id id) {
		return this.informedHouseholds.contains(id);
	}

	/*
	 * Define for each household when it is informed that an evacuation is
	 * required. So far this is done randomly. In the future this could also
	 * include information like the geographical positions of the households
	 * members.
	 */
	private void selectInformationTimes(Households households) {

		int totalAgents = 0;

		for (Household household : households.getHouseholds().values()) {
			
			// skip households without members
			if (household.getMemberIds().size() == 0) continue;
			
			double delay = calculateInformationDelay(household.getId());

			/*
			 * We have to add one second here. This ensure that some code which is executed
			 * at the end of a time step is executed when the simulation has started.
			 */
			this.informationTime.add(new Tuple<Id, Double>(household.getId(), EvacuationConfig.evacuationTime + delay + 1.0));
			totalAgents += household.getMemberIds().size();
		}

		this.setTotalAgentCount(totalAgents);
	}

	/*
	 * So far use a Rayleigh Distribution with a sigma of 300. After 353s ~ 50%
	 * of all households have been informed.
	 */
	private final double sigma = 300;
	private final double upperLimit = 0.999999;

	private double calculateInformationDelay(Id householdId) {

		double rand = this.rng.hashCodeToRandomDouble(householdId);

		if (rand == 0.0) return 0.0;
		else if (rand > upperLimit) rand = upperLimit;

		return Math.floor(Math.sqrt(-2 * Math.pow(sigma, 2) * Math.log(1 - rand)));
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		if (this.allHouseholdsInformed && this.allAgentsInformed()) return;
		
		/*
		 * If all households and agents have been informed
		 */
		if (this.informedHouseholds.size() == this.totalHouseholds && this.allAgentsInformed()) {
			this.allHouseholdsInformed = true;
			log.info("All households have been informed at " + Time.writeTime(e.getSimulationTime()));
			return;
		}
		
		/*
		 * Clear the list of households who have been informed in the last time
		 * step.
		 */
		this.informedHouseholds.addAll(this.informedHouseholdsInCurrentTimeStep);
		this.informedHouseholdsInCurrentTimeStep.clear();

		double time = e.getSimulationTime();

		/*
		 * Register households to be initially replanned.
		 */
		while (this.informationTime.peek() != null) {
			Tuple<Id, Double> tuple = informationTime.peek();
			if (tuple.getSecond() <= time) {
				this.informationTime.poll();
				Household household = households.getHouseholds().get(tuple.getFirst());
				informedHouseholdsInCurrentTimeStep.add(tuple.getFirst());
				
				for (Id memberId : household.getMemberIds()) {
					this.toBeInitiallyReplannedAgents.add(memberId);
					this.informedAgents.add(memberId);
				}
			} else {
				break;
			}
		}

		super.notifyMobsimBeforeSimStep(e);
	}

	private class InformationTimeComparator implements
			Comparator<Tuple<Id, Double>>, Serializable, MatsimComparator {

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
