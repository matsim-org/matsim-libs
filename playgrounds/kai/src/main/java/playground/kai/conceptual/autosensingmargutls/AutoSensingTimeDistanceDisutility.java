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

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author nagel
 */
public class AutoSensingTimeDistanceDisutility implements TravelDisutility {

	protected final TravelTime timeCalculator;

	private final Map<Person,Double> margCstOfTimeLookup = new HashMap<Person,Double>() ;
	private final Map<Person,Double> margCstOfDistanceLookup = new HashMap<Person,Double>() ;
	private final Map<Person,Double> margUtlOfMoneyLookup = new HashMap<Person,Double>() ;

	private double marginalCostOfTimeMin = 0. ;
	private double marginalCostOfDistanceMin = 0. ;

	public AutoSensingTimeDistanceDisutility(final Scenario scenario, final TravelTime timeCalculator) {
		this.timeCalculator = timeCalculator;

		PopulationFactory pf = scenario.getPopulation().getFactory() ;

		// yy might be a bit far "inside", i.e. the following computation should probably be done outside this class and then passed in. kai, oct'13
		EventsToScore e2s = new EventsToScore(scenario, ControlerUtils.createDefaultScoringFunctionFactory(scenario) ) ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			List<Double> scores = new ArrayList<Double>() ;
			Activity firstAct = (Activity) person.getSelectedPlan().getPlanElements().get(0) ;
			// (need this because we need a valid activity type)

			scores.add(e2s.getScoringFunctionForAgent( person.getId() ).getScore()) ;
			for ( int ii=0 ; ii<=1 ; ii++ ) {
				e2s.handleEvent( new ActivityStartEvent(20.0*3600., person.getId(), null, null, firstAct.getType() ) ) ;
				e2s.handleEvent( new ActivityEndEvent((32.0+ii)*3600., person.getId(), null, null, firstAct.getType() ) ) ;
				e2s.handleEvent( new PersonDepartureEvent((32.0+ii)*3600., person.getId(), null, TransportMode.car ) );
				e2s.handleEvent( new PersonArrivalEvent(33.0*3600., person.getId(), null, TransportMode.car ) );
				scores.add( e2s.getScoringFunctionForAgent( person.getId() ).getScore() ) ;
			}
			System.out.println( " VTTS: " + ( (scores.get(2) - scores.get(1)) - (scores.get(1)-scores.get(0)) ) ) ;
			
			e2s.handleEvent( new PersonMoneyEvent( 33.*3600., person.getId(), 1. ) ) ;
			scores.add( e2s.getScoringFunctionForAgent( person.getId() ).getScore() ) ;
			
			System.out.println( "mum: " + ( scores.get(3)-scores.get(2) )  );


			//			for ( int ii=0 ; ii<=1 ; ii++ ) {
			//
			//				Leg testLeg = pf.createLeg(TransportMode.car) ;
			//				testLeg.setDepartureTime(7.*3600);
			//				testLeg.setTravelTime(3600. * ii);
			//
			//				sf.handleLeg( testLeg );
			//				
			//				Activity testAct = pf.createActivityFromCoord(firstAct.getType(), new CoordImpl(0.,0.) ) ;
			//				testAct.setStartTime( testLeg.getDepartureTime() + testLeg.getTravelTime() );
			//				double typicalDuration = 3600. ;
			//				testAct.setEndTime( testLeg.getDepartureTime() + typicalDuration ) ;
			//
			//				sf.handleActivity( testAct );
			//				
			//				double score = sf.getScore();
			//				scores.add( score ) ;
			//				
			//				sf.addMoney(1.);
			//				
			//				System.out.println( " margUtlOfMon: " + ( sf.getScore() - score ) );
			//				
			//			}
			//			System.out.println( " effMargDisutlOfTrav: " + ( scores.get(1) - scores.get(0) ) );

		}

	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

		double marginalCostOfTime = this.margCstOfTimeLookup.get(person) ;
		double marginalCostOfDistance = this.margCstOfDistanceLookup.get(person) ;
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
		TravelDisutility td = new AutoSensingTimeDistanceDisutility(scenario, tt ) ;
	}

}
