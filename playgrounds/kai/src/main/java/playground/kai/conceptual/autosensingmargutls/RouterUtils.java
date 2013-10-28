/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.scoring.EventsToScore;

/**
 * @author nagel
 *
 */
class RouterUtils {
	
	static class MarginalUtilitiesContainer {
		final Map<Person,Double> margCstOfTimeLookup = new HashMap<Person,Double>() ;
		final Map<Person,Double> margCstOfDistanceLookup = new HashMap<Person,Double>() ;
		final Map<Person,Double> margUtlOfMoneyLookup = new HashMap<Person,Double>() ;
	}
	
	static MarginalUtilitiesContainer createMarginalUtilitiesContrainer( Scenario scenario ) {
		MarginalUtilitiesContainer muc = new MarginalUtilitiesContainer() ;
		
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
			double utts = (scores.get(2) - scores.get(1)) - (scores.get(1)-scores.get(0));
			System.out.println( " VTTS: " + utts ) ;
			muc.margCstOfTimeLookup.put( person, utts ) ;
			
			e2s.handleEvent( new PersonMoneyEvent( 33.*3600., person.getId(), 1. ) ) ;
			scores.add( e2s.getScoringFunctionForAgent( person.getId() ).getScore() ) ;
			double mum = scores.get(3)-scores.get(2);
			System.out.println( "mum: " + mum  );
			muc.margUtlOfMoneyLookup.put( person, mum )  ;

		}
		
		return muc ;
	}

}
