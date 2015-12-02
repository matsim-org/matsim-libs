/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.scenario;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ArbitraryEventScoring;
import org.matsim.core.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters.Mode;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.jbischoff.taxibus.run.configuration.ConfigBasedTaxibusLaunchUtils;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;

/**
 * @author jbischoff
 *
 */
public class KNTaxibusExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/vw_rufbus/scenario/input/example/configVWTB.xml", new TaxibusConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		new ConfigBasedTaxibusLaunchUtils(controler).initiateTaxibusses();

//		controler.setScoringFunctionFactory(new ScoringFunctionFactory(){
//			@Override
//			public ScoringFunction createNewScoringFunction(Person person) {
//				SumScoringFunction sum = new SumScoringFunction() ;
//
//				// Score activities, legs, payments and being stuck
//				// with the default MATSim scoring based on utility parameters in the config file.
//				final CharyparNagelScoringParameters params =
//						CharyparNagelScoringParameters.getBuilder(
//								scenario,
//								person.getId() ).create();
//				sum.addScoringFunction(new CharyparNagelActivityScoring(params));
//				sum.addScoringFunction(new MyLegScoring(params, scenario.getNetwork()));
//				sum.addScoringFunction(new CharyparNagelMoneyScoring(params));
//				sum.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
//
//				return sum ;
//			}
//		});

		controler.run();
	}

	static class MyLegScoring implements org.matsim.core.scoring.SumScoringFunction.LegScoring, org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring {

		protected double score;

		/** The parameters used for scoring */
		protected final CharyparNagelScoringParameters params;
		protected Network network;
		private boolean nextEnterVehicleIsFirstOfTrip = true ;
		private boolean nextStartPtLegIsFirstOfTrip = true ;
		private boolean currentLegIsPtLeg = false;
		private double lastActivityEndTime = Time.UNDEFINED_TIME ;

		public MyLegScoring(final CharyparNagelScoringParameters params, Network network) {
			this.params = params;
			this.network = network;
		}

		@Override
		public void finish() {

		}

		@Override
		public double getScore() {
			return this.score;
		}

		private static int ccc=0 ;

		protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
			double tmpScore = 0.0;
			double travelTime = arrivalTime - departureTime; // travel time in seconds	
			Mode modeParams = this.params.modeParams.get(leg.getMode());
			if (modeParams == null) {
				if (leg.getMode().equals(TransportMode.transit_walk)) {
					modeParams = this.params.modeParams.get(TransportMode.walk);
				} else {
					modeParams = this.params.modeParams.get(TransportMode.other);
				}
			}
			tmpScore += travelTime * modeParams.marginalUtilityOfTraveling_s;
			if (modeParams.marginalUtilityOfDistance_m != 0.0
					|| modeParams.monetaryDistanceCostRate != 0.0) {
				Route route = leg.getRoute();
				double dist = route.getDistance(); // distance in meters
				if ( Double.isNaN(dist) ) {
					if ( ccc<10 ) {
						ccc++ ;
						Logger.getLogger(this.getClass()).warn("distance is NaN. Will make score of this plan NaN. Possible reason: Simulation does not report " +
								"a distance for this trip. Possible reason for that: mode is teleported and router does not " +
								"write distance into plan.  Needs to be fixed or these plans will die out.") ;
						if ( ccc==10 ) {
							Logger.getLogger(this.getClass()).warn(Gbl.FUTURE_SUPPRESSED) ;
						}
					}
				}
				tmpScore += modeParams.marginalUtilityOfDistance_m * dist;
				tmpScore += modeParams.monetaryDistanceCostRate * this.params.marginalUtilityOfMoney * dist;
			}
			tmpScore += modeParams.constant;
			// (yyyy once we have multiple legs without "real" activities in between, this will produce wrong results.  kai, dec'12)
			// (yy NOTE: the constant is added for _every_ pt leg.  This is not how such models are estimated.  kai, nov'12)
			return tmpScore;
		}

		@Override
		public void handleEvent(Event event) {
			if ( event instanceof ActivityEndEvent ) {
				// When there is a "real" activity, flags are reset:
				if ( !PtConstants.TRANSIT_ACTIVITY_TYPE.equals( ((ActivityEndEvent)event).getActType()) ) {
					this.nextEnterVehicleIsFirstOfTrip  = true ;
					this.nextStartPtLegIsFirstOfTrip = true ;
				}
				this.lastActivityEndTime = event.getTime() ;
			}

			if ( event instanceof PersonEntersVehicleEvent && currentLegIsPtLeg ) {
				if ( !this.nextEnterVehicleIsFirstOfTrip ) {
					// all vehicle entering after the first triggers the disutility of line switch:
					this.score  += params.utilityOfLineSwitch ;
				}
				this.nextEnterVehicleIsFirstOfTrip = false ;
				// add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
				this.score += (event.getTime() - this.lastActivityEndTime) * (this.params.marginalUtilityOfWaitingPt_s - this.params.modeParams.get(TransportMode.pt).marginalUtilityOfTraveling_s) ;
			}

			if ( event instanceof PersonDepartureEvent ) {
				this.currentLegIsPtLeg = TransportMode.pt.equals( ((PersonDepartureEvent)event).getLegMode() );
				if ( currentLegIsPtLeg ) {
					if ( !this.nextStartPtLegIsFirstOfTrip ) {
						this.score -= params.modeParams.get(TransportMode.pt).constant ;
						// (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
						// modification is minimally invasive.  kai, dec'12)
					}
					this.nextStartPtLegIsFirstOfTrip = false ;
				}
			}
		}

		@Override
		public void handleLeg(Leg leg) {
			double legScore = calcLegScore(leg.getDepartureTime(), leg.getDepartureTime() + leg.getTravelTime(), leg);
			this.score += legScore;
		}



	}
	
}
