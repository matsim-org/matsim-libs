/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.kai.conceptual.autosensingmargutls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.vehicles.Vehicle;

import playground.kai.conceptual.autosensingmargutls.RouterUtils.MarginalUtilitiesContainer;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author nagel
 */
public class PersonIndividualTimeDistanceDisutility implements TravelDisutility {

	protected final TravelTime timeCalculator;

	private MarginalUtilitiesContainer muc ;
	
	private double marginalCostOfTimeMin = 0. ;
	private double marginalCostOfDistanceMin = 0. ;

	public PersonIndividualTimeDistanceDisutility(final TravelTime timeCalculator, MarginalUtilitiesContainer muc) {
		this.timeCalculator = timeCalculator;


	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

		double marginalCostOfTime = muc.margCstOfTimeLookup.get(person) ;
		double marginalCostOfDistance = muc.margCstOfDistanceLookup.get(person) ;
		return marginalCostOfTime * travelTime + marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {

		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTimeMin
				+ this.marginalCostOfDistanceMin * link.getLength();
	}

	public static void main( String[] args ) {
		Config config = ConfigUtils.createConfig() ;
		ActivityParams params = new ActivityParams("h") ;
		params.setTypicalDuration(12.*3600.);
		config.planCalcScore().addActivityParams(params);

		Scenario scenario = ScenarioUtils.createScenario(config) ;

		Population pop = scenario.getPopulation() ;
		PopulationFactory pf = pop.getFactory() ;

		Person person = pf.createPerson(new IdImpl(1)) ;
		pop.addPerson(person); 

		Plan plan = pf.createPlan() ;
		person.addPlan(plan) ;

		Activity act = pf.createActivityFromCoord("h", new CoordImpl(0.,0.) ) ;
		plan.addActivity(act); 

		TravelTime tt = new FreeSpeedTravelTime() ;
		MarginalUtilitiesContainer muc = RouterUtils.createMarginalUtilitiesContrainer(scenario) ;
		TravelDisutility td = new PersonIndividualTimeDistanceDisutility(tt, muc ) ;
	}

}
