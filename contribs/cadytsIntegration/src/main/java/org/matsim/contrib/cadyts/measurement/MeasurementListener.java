/* *********************************************************************** *
 * project: org.matsim.*
 * PlanToPlanStepBasedOnEvents.java
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

package org.matsim.contrib.cadyts.measurement;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.kai.DataMap;
import org.matsim.contrib.analysis.kai.Databins;
import org.matsim.contrib.cadyts.general.PlansTranslator;

import cadyts.demand.PlanBuilder;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

public class MeasurementListener implements PlansTranslator<Measurement>,  
		PersonDepartureEventHandler, PersonArrivalEventHandler, SimResults<Measurement> {
	
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(MeasurementListener.class);

	private final Scenario scenario;

	private final Map<Id<Person>,PersonDepartureEvent> driverAgents = new TreeMap<>() ;
	
	private int iteration = -1;

	// this is _only_ there for output:
	Set<Plan> plansEverSeen = new HashSet<>();

	private static final String STR_PLANSTEPFACTORY = "planStepFactory";
	private static final String STR_ITERATION = "iteration";

	private final Measurements  measurements ;

	private final Databins<Measurement> databins ;

	MeasurementListener(final Scenario scenario, Measurements measurements) {
		this.measurements = measurements ;
		this.scenario = scenario;
		
		double[] dataBoundaries = new double[24] ;
		for ( int ii=0 ; ii<dataBoundaries.length; ii++ ) {
			dataBoundaries[ii] = ii * 3600. ;
			// hourly bins, not connected to anything; this might be improved ...
		}
		this.databins = new Databins<Measurement>( "travel times for measurement facility at each hour" , dataBoundaries ) ;
	}

	private long plansFound = 0;
	private long plansNotFound = 0;

	@Override
	public final cadyts.demand.Plan<Measurement> getCadytsPlan(final Plan plan) {
		@SuppressWarnings("unchecked")
		PlanBuilder<Measurement> planStepFactory = (PlanBuilder<Measurement>) plan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		if (planStepFactory == null) {
			this.plansNotFound++;
			return null;
		}
		this.plansFound++;
		final cadyts.demand.Plan<Measurement> planSteps = planStepFactory.getResult();
		return planSteps;
	}

	@Override
	public void reset(final int iteration1) {
		this.iteration = iteration1;

		log.warn("found " + this.plansFound + " out of " + (this.plansFound + this.plansNotFound) + " ("
				+ (100. * this.plansFound / (this.plansFound + this.plansNotFound)) + "%)");
		log.warn("(above values may both be at zero for a couple of iterations if multiple plans per agent all have no score)");

		this.driverAgents.clear();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.driverAgents.put(event.getPersonId(), event ) ;
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		PersonDepartureEvent dpEvent = this.driverAgents.remove( event.getPersonId() ) ;
		double ttime = event.getTime() - dpEvent.getTime() ;
		
		// the travel time determines the measurement "facility":
		Measurement measurement = measurements.getMeasurementFromTTimeInSeconds(ttime) ;
		
		// the following will fill the cadyts plan:
		// get the planStepFactory for the plan (or create one):
		Person person = this.scenario.getPopulation().getPersons().get(event.getPersonId());
		PlanBuilder<Measurement> tmpPlanStepFactory = getPlanStepFactoryForPlan(person.getSelectedPlan());
		// add the measurement:
		if (tmpPlanStepFactory != null) {
			// can this happen?? Maybe in early time steps???
						
			tmpPlanStepFactory.addTurn( measurement, (int) event.getTime());
		}
		
		// the following will lead to getSimValue:
		int idx = this.databins.getIndex( dpEvent.getTime() ) ;
		this.databins.inc( measurement, idx);

	}

	// ###################################################################################
	// only private functions below here (low level functionality)

	private PlanBuilder<Measurement> getPlanStepFactoryForPlan(final Plan selectedPlan) {

		@SuppressWarnings("unchecked")
		PlanBuilder<Measurement> planStepFactory = (PlanBuilder<Measurement>) selectedPlan.getCustomAttributes().get(STR_PLANSTEPFACTORY);

		Integer factoryIteration = (Integer) selectedPlan.getCustomAttributes().get(STR_ITERATION);
		if (planStepFactory == null || factoryIteration == null || factoryIteration != this.iteration) {
			// attach the iteration number to the plan:
			selectedPlan.getCustomAttributes().put(STR_ITERATION, this.iteration);

			// construct a new PlanBulder and attach it to the plan:
			planStepFactory = new PlanBuilder<Measurement>();
			selectedPlan.getCustomAttributes().put(STR_PLANSTEPFACTORY, planStepFactory);

			// memorize the plan as being seen:
			this.plansEverSeen.add(selectedPlan);
		}

		return planStepFactory;
	}

	@Override
	public double getSimValue(Measurement mea, int startTime_s, int endTime_s, TYPE type) {
		Assert.assertNotNull( mea ); 
		return this.databins.getValue( mea, this.databins.getIndex( startTime_s ) ) ;
	}

	

}
