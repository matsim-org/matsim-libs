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

package playground.christoph.evacuation.mobsim;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.events.HouseholdInformationEvent;
import playground.christoph.evacuation.events.handler.HouseholdInformationEventHandler;

/*
 * Tracks the information level of all households and their members.
 */
public class InformedHouseholdsTracker extends InformedAgentsTracker implements HouseholdInformationEventHandler {

	/*package*/ final int totalHouseholds;	// number of households with more than 0 members
	/*package*/ final Households households;
	/*package*/ final Set<Id> informedHouseholds;

	private int infoTime = 0;
	private boolean allHouseholdsInformed = false;
	private double allHouseholdsInformedTime = Double.NaN;
	
	private Set<Id> informedInLastTimeStep = new LinkedHashSet<Id>();
	private Set<Id> informedInCurrentTimeStep = new LinkedHashSet<Id>();
	
	public InformedHouseholdsTracker(Population population, Households households) {
		super(population);
		
		this.households = households;
		this.informedHouseholds = new HashSet<Id>();

		/*
		 * We ignore households with 0 members. Therefore we cannot use
		 * households.getHouseholds().size()
		 */
		int num = 0;
		for (Household household : households.getHouseholds().values()) if (household.getMemberIds().size() > 0) num++;
		totalHouseholds = num;
	}

	public boolean allHouseholdsInformed() {
		return this.allHouseholdsInformed;
	}
	
	public double getAllHouseholdsInformedTime() {
		return this.allHouseholdsInformedTime;
	}
	
	public boolean isHouseholdInformed(Id id) {
		if (allHouseholdsInformed) return true;
		return this.informedHouseholds.contains(id);
	}
	
	public Set<Id> getHouseholdsInformedInLastTimeStep() {
		return Collections.unmodifiableSet(this.informedInLastTimeStep);
	}

	@Override
	public void handleEvent(HouseholdInformationEvent event) {
		this.informedHouseholds.add(event.getHouseholdId());
		this.informedInCurrentTimeStep.add(event.getHouseholdId());
	}
	
	@Override
	public void reset(int iteration) {
		this.informedHouseholds.clear();
		this.informedInLastTimeStep.clear();
		this.informedInCurrentTimeStep.clear();
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {

		super.notifyMobsimAfterSimStep(e);
		
		this.informedInLastTimeStep = this.informedInCurrentTimeStep;
		this.informedInCurrentTimeStep = new LinkedHashSet<Id>();
		
		if (this.allHouseholdsInformed) return;
		
		double time = e.getSimulationTime();
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			
			if (time >= EvacuationConfig.evacuationTime) this.printStatistics(time);
		}
		
		/*
		 * If all households and agents have been informed
		 */
		if (this.informedHouseholds.size() == this.totalHouseholds) {
			log.info("All households have been informed at " + Time.writeTime(e.getSimulationTime()));
			printStatistics(time);
			this.allHouseholdsInformed = true;
			this.allHouseholdsInformedTime = time;
			return;
		}
	}
	
	private void printStatistics(double time) {
		
		int informed = this.informedHouseholds.size();
		int notInformed = this.totalHouseholds - informed;
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		log.info("Simulation at " + Time.writeTime(time) + ", Informed Households Statistics: # total Households=" + totalHouseholds
			+ ", # informed Households=" + informed + "(" + df.format((100.0*informed)/totalHouseholds) + "%)"
			+ ", # not informed Households=" + notInformed + "(" + df.format((100.0*notInformed)/totalHouseholds) + "%)");
	}

}