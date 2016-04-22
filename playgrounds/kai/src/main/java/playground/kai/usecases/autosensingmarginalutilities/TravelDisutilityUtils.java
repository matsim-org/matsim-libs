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
package playground.kai.usecases.autosensingmarginalutilities;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * @author nagel
 *
 */
class TravelDisutilityUtils {
	private TravelDisutilityUtils(){} // do not instantiate
	
	private static Logger log = Logger.getLogger( TravelDisutilityUtils.class );
	
	private static int cnt = 0 ;
	static EffectiveMarginalUtilitiesContainer createAutoSensingMarginalUtilitiesContainer( Scenario scenario, ScoringFunctionFactory scoringFunctionFactory ) {
		// yy one might want to make the following replaceable. kai, oct'13
		log.warn("running ..."); 
		
		final int N_TESTS = 3 ;
		
		EffectiveMarginalUtilitiesContainer muc = new EffectiveMarginalUtilitiesContainer() ;
		List<EventsManager> es = new ArrayList<>();
		List<EventsToScore> e2s = new ArrayList<EventsToScore>() ;
		for ( int ii=0 ; ii<N_TESTS+1 ; ii++ ) {
			EventsManager eventsManager = EventsUtils.createEventsManager();
			es.add(eventsManager);
			EventsToScore oneE2s = EventsToScore.createWithoutScoreUpdating(scenario, scoringFunctionFactory, eventsManager);
			oneE2s.beginIteration(0);
			e2s.add(oneE2s) ;
		}
		
		double effMargUtlTTimeMAX = Double.NEGATIVE_INFINITY ;
		double effMargUtlDistanceMAX = Double.NEGATIVE_INFINITY ;
		double margUtlOfMoneyMAX = Double.NEGATIVE_INFINITY ;
		double mVTTSMAX = Double.NEGATIVE_INFINITY ;
		double mVTDSMAX = Double.NEGATIVE_INFINITY ;
		
		double effMargUtlTTimeMIN = Double.POSITIVE_INFINITY ;
		double effMargUtlDistanceMIN = Double.POSITIVE_INFINITY ;
		double margUtlOfMoneyMIN = Double.POSITIVE_INFINITY ;
		double mVTTSMIN = Double.POSITIVE_INFINITY ;
		double mVTDSMIN = Double.POSITIVE_INFINITY ;
		
		final double DEPARTURE = 7.5*3600., DELTA_TRIPTIME = 10. /*seconds*/, DELTA_DISTANCE = 100./*meters*/  ;
		
		for ( Person person : scenario.getPopulation().getPersons().values() ) {

			Activity firstAct = (Activity) person.getSelectedPlan().getPlanElements().get(0) ;
			// (need this because we need a valid activity type)
			/* 
			 * yy would certainly be better to play back the full plan
			 * yy would be even better to play back the experienced plan
			 */
			double typicalDuration = scenario.getConfig().planCalcScore().getActivityParams( firstAct.getType() ).getTypicalDuration() ;
			final Plan experiencedPlan = (Plan) person.getSelectedPlan().getCustomAttributes().get( PlanCalcScoreConfigGroup.EXPERIENCED_PLAN_KEY );
			if ( experiencedPlan != null ) {
				if ( cnt < 1 ) {
					cnt++ ;	
					log.warn( "using an experienced plan ... ") ;
				}
				StageActivityTypes stageActivities = null ; // yyyy should be set to something meaningful
				List<Activity> activities = PopulationUtils.getActivities(experiencedPlan, stageActivities ) ;
				typicalDuration = activities.get(1).getEndTime() - activities.get(1).getStartTime() ; 
			}

			double triptime=0, distance=0 ;
			for ( int ii=0 ; ii<N_TESTS ; ii++ ) {
				if ( ii==0 ) {
					triptime = 0. ; distance = 0. ;
				} else if ( ii==1 ) {
					triptime = DELTA_TRIPTIME ; distance = 0. ;
				} else if ( ii==2 ) {
					triptime = DELTA_TRIPTIME ; distance = DELTA_DISTANCE ;
				}
				double now = DEPARTURE ;
				es.get(ii).processEvent(new ActivityEndEvent(now, person.getId(), null, null, firstAct.getType())); ;
				es.get(ii).processEvent(new PersonDepartureEvent(now, person.getId(), null, TransportMode.car));
				now = DEPARTURE + triptime ;
				es.get(ii).processEvent(new TeleportationArrivalEvent(now, person.getId(), distance)) ;
				es.get(ii).processEvent(new PersonArrivalEvent(now, person.getId(), null, TransportMode.car));
				es.get(ii).processEvent(new ActivityStartEvent(now, person.getId(), null, null, firstAct.getType())) ;
				now = DEPARTURE + typicalDuration + 0.5*DELTA_TRIPTIME ;
				es.get(ii).processEvent(new ActivityEndEvent(now, person.getId(), null, null, firstAct.getType())) ;
				es.get(ii).processEvent(new PersonDepartureEvent(now, person.getId(), null, TransportMode.car));
				es.get(ii).processEvent(new TeleportationArrivalEvent(now, person.getId(), 0.)) ;
				es.get(ii).processEvent(new PersonArrivalEvent(now, person.getId(), null, TransportMode.car));
				es.get(ii).processEvent(new ActivityStartEvent(now, person.getId(), null, null, firstAct.getType())) ;
			}
			es.get(N_TESTS).processEvent( new PersonMoneyEvent( 33.*3600., person.getId(), 1. ) ) ;
		}
		for ( EventsToScore eee : e2s ) {
			eee.finish();
		}
		long cnt=-1 ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			cnt++ ;
			double utts = ( e2s.get(1).getAgentScore(person.getId()) - e2s.get(0).getAgentScore(person.getId()) ) / DELTA_TRIPTIME ;
			muc.putEffectiveMarginalUtilityOfTtime( person.getId(), utts ) ;
			effMargUtlTTimeMAX = Math.max( effMargUtlTTimeMAX, utts ) ;
			effMargUtlTTimeMIN = Math.min( effMargUtlTTimeMIN, utts ) ;
			
			double utds = ( e2s.get(2).getAgentScore(person.getId()) - e2s.get(1).getAgentScore(person.getId()) ) / DELTA_DISTANCE ;
			muc.putMarginalUtilityOfDistance( person.getId(), utds ) ;
			effMargUtlDistanceMAX = Math.max( effMargUtlDistanceMAX, utds ) ;
			effMargUtlDistanceMIN = Math.min( effMargUtlDistanceMIN, utds ) ;
			
			double mum = e2s.get(N_TESTS).getAgentScore(person.getId()) ;
			muc.putMarginalUtilityOfMoney( person.getId(), mum )  ;
			margUtlOfMoneyMAX = Math.max( margUtlOfMoneyMAX, mum ) ;
			margUtlOfMoneyMIN = Math.min( margUtlOfMoneyMIN, mum ) ;
			mVTTSMAX = Math.max( mVTTSMAX, -utts/mum ) ;
			mVTTSMIN = Math.min( mVTTSMIN, -utts/mum ) ;
			mVTDSMAX = Math.max( mVTDSMAX, -utds/mum ) ;
			mVTDSMIN = Math.min( mVTDSMIN, -utds/mum ) ;

			if ( cnt%1000==0 || utts*3600>-5. ) {
				log.info( "personId: " + person.getId() ) ;
				log.info( "eff marg utl of travel time: " + (utts * 3600.) + " per hr") ;
				log.info( "marg utl of travel distance: " + (utds * 1000.) + " per km"); 
				log.info( "marg utl of money: " + mum  + " per unit of money");
			}
		}
		
		log.info( "marginal utilities of money stretch from " + margUtlOfMoneyMIN + " to " + margUtlOfMoneyMAX + " per monetary unit");
		log.info( "effective marginal utilities of ttime stretch from " + 3600.*effMargUtlTTimeMIN + 
				" to " + 3600.*effMargUtlTTimeMAX + " per hour");
		log.info( "effective marginal utilities of distance stretch from " + 1000.*effMargUtlDistanceMIN + 
				" to " + 1000.*effMargUtlDistanceMAX + " per km") ;
		log.info( "mVTTS stretch from " + 3600.*mVTTSMIN + " to " + 3600.*mVTTSMAX + " [monetary units]/hour" );
		log.info( "mVTDS stretch from " + 1000.*mVTDSMIN + " to " + 1000.*mVTDSMAX + " [monetary units]/km" );
		
		muc.setMarginalUtilityOfTravelTimeMAX( effMargUtlTTimeMAX );
		muc.setEffectiveMarginalUtilityOfDistanceMAX( effMargUtlDistanceMAX );
		
		return muc ;
	}

}
