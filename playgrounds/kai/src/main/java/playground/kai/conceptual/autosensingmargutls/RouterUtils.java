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
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * @author nagel
 *
 */
class RouterUtils {
	
	static EffectiveMarginalUtilitiesContainer createMarginalUtilitiesContrainer( Scenario scenario, ScoringFunctionFactory scoringFunctionFactory ) {
		// yy one might want to make the following replaceable. kai, oct'13
		
		EffectiveMarginalUtilitiesContainer muc = new EffectiveMarginalUtilitiesContainer() ;
		
		EventsToScore e2s = new EventsToScore(scenario, scoringFunctionFactory ) ;
		
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			List<Double> scores = new ArrayList<Double>() ;
			Activity firstAct = (Activity) person.getSelectedPlan().getPlanElements().get(0) ;
			// (need this because we need a valid activity type)

			double triptime=0, distance=0, deltaTriptime = 3600. , deltaDistance = 100. * 1000. ;
			double arrival = 32.5*3600. ;
			scores.add(e2s.getScoringFunctionForAgent( person.getId() ).getScore()) ;
			for ( int ii=1 ; ii<=3 ; ii++ ) {
				if ( ii==1 ) {
					triptime = 0. ; distance = 0. ;
				} else if ( ii==2 ) {
					triptime = deltaTriptime ; distance = 0. ;
				} else if ( ii==3 ) {
					triptime = deltaTriptime ; distance = deltaDistance ;
				}
				e2s.handleEvent( new ActivityStartEvent(20.0*3600., person.getId(), null, null, firstAct.getType() ) ) ;
				double now = arrival - triptime ;
				e2s.handleEvent( new ActivityEndEvent(now, person.getId(), null, null, firstAct.getType() ) ) ;
				e2s.handleEvent( new PersonDepartureEvent(now, person.getId(), null, TransportMode.car ) );
				now = arrival ;
				e2s.handleEvent( new TeleportationArrivalEvent( now, person.getId(), distance ) ) ;
				e2s.handleEvent( new PersonArrivalEvent( now, person.getId(), null, TransportMode.car ) );
				scores.add( e2s.getScoringFunctionForAgent( person.getId() ).getScore() ) ;
			}
			double utts = (  (scores.get(2) - scores.get(1)) - (scores.get(1)-scores.get(0)) ) / deltaTriptime ;
			System.out.println( "eff marg utl of travel time: " + (utts * 3600.) + " per hr") ;
			muc.getEffectiveMarginalUtilityOfTravelTime().put( person, utts ) ;
			
			double utds = - ( (scores.get(3) - scores.get(2)) - ( scores.get(2)-scores.get(1)) ) / deltaDistance ;
			System.out.println( "marg utl of travel distance: " + (utds * 1000.) + " per km"); 
			muc.getMarginalUtilityOfDistance().put( person, utds ) ;
			
			e2s.handleEvent( new PersonMoneyEvent( 33.*3600., person.getId(), 1. ) ) ;
			scores.add( e2s.getScoringFunctionForAgent( person.getId() ).getScore() ) ;
			double mum = scores.get(4)-scores.get(3);
			System.out.println( "marg utl of money: " + mum  + " per unit of money");
			muc.getMarginalUtilityOfMoney().put( person, mum )  ;

		}
		
		return muc ;
	}

}
