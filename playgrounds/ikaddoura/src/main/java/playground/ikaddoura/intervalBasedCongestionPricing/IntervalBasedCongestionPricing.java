/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ikaddoura.intervalBasedCongestionPricing;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionInfo;
import playground.ikaddoura.intervalBasedCongestionPricing.handler.DelayComputation;
import playground.ikaddoura.intervalBasedCongestionPricing.handler.PersonVehicleTracker;
import playground.ikaddoura.intervalBasedCongestionPricing.handler.TimeTracker;

/**
 * @author ikaddoura
 *
 */

public class IntervalBasedCongestionPricing implements StartupListener, AfterMobsimListener, IterationEndsListener {

	private final CongestionInfo congestionInfo;
	private final SortedMap<Integer, Double> iteration2totalDelay = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTollPayments = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2totalTravelTime = new TreeMap<>();
	private final SortedMap<Integer, Double> iteration2userBenefits = new TreeMap<>();
	
	private TimeTracker timeTracker;
	private DelayComputation delayComputation;

	public IntervalBasedCongestionPricing(Scenario scenario){
		this.congestionInfo = new CongestionInfo(scenario);
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.timeTracker = new TimeTracker(congestionInfo, event.getServices().getEvents());
		event.getServices().getEvents().addHandler(this.timeTracker);
						
		this.delayComputation = new DelayComputation(congestionInfo);
		event.getServices().getEvents().addHandler(this.delayComputation);
		
		event.getServices().getEvents().addHandler(new PersonVehicleTracker(congestionInfo));
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {				
		timeTracker.computeFinalTimeIntervals();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		this.iteration2totalDelay.put(event.getIteration(), this.delayComputation.getTotalDelay());
		this.iteration2totalTollPayments.put(event.getIteration(), this.timeTracker.getTotalTollPayments());
		this.iteration2totalTravelTime.put(event.getIteration(), this.delayComputation.getTotalTravelTime());
		
		double monetizedUserBenefits = 0.;
		for (Person person : this.congestionInfo.getScenario().getPopulation().getPersons().values()) {
			monetizedUserBenefits = monetizedUserBenefits + person.getSelectedPlan().getScore() / this.congestionInfo.getScenario().getConfig().planCalcScore().getMarginalUtilityOfMoney();
		}
		this.iteration2userBenefits.put(event.getIteration(), monetizedUserBenefits);
		
		CongestionInfoWriter.writeIterationStats(
				this.iteration2totalDelay,
				this.iteration2totalTollPayments,
				this.iteration2totalTravelTime,
				this.iteration2userBenefits,
				this.congestionInfo.getScenario().getConfig().controler().getOutputDirectory()
				);
	}
	
}
