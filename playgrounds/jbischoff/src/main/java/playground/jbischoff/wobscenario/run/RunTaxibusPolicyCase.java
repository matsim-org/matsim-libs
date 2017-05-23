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
package playground.jbischoff.wobscenario.run;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxibus.run.configuration.*;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.*;
import org.matsim.core.scoring.functions.*;

import playground.jbischoff.wobscenario.analysis.TaxiBusTravelTimesAnalyzer;

/**
 * @author jbischoff
 *
 */
public class RunTaxibusPolicyCase {

	public static void main(String[] args) {

		String configFileName = "../../../shared-svn/projects/vw_rufbus/projekt2/input/ConfigPC112.xml";
		if (args.length>0){
			configFileName = args[0];
		}
		Config config = ConfigUtils.loadConfig(configFileName, new TaxibusConfigGroup(), new DvrpConfigGroup());
//		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

		new TaxibusControlerCreator(controler).initiateTaxibusses();

		controler.setScoringFunctionFactory(new ScoringFunctionFactory(){
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sum = new SumScoringFunction() ;

				// Score activities, legs, payments and being stuck
				// with the default MATSim scoring based on utility parameters in the config file.
				final ScoringParameters params =
						new ScoringParameters.Builder(scenario, person.getId()).build();
				sum.addScoringFunction(new CharyparNagelActivityScoring(params));
				sum.addScoringFunction(new MyLegScoring(params, scenario.getNetwork()));
				sum.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sum.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				return sum ;
			}
		});
		final TaxiBusTravelTimesAnalyzer taxibusTravelTimesAnalyzer = new TaxiBusTravelTimesAnalyzer();
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
			addEventHandlerBinding().toInstance(taxibusTravelTimesAnalyzer);				
			}
		});
		
		
		controler.run();
	}

	static class MyLegScoring extends CharyparNagelLegScoring {

		protected double score;
		private final Set<Id<Link>> parkingLinks;
		private Random rand = MatsimRandom.getRandom();
		
		public MyLegScoring(final ScoringParameters params, Network network) {
			super(params,network);
			this.parkingLinks = new HashSet<>();
			//Links where car legs receive an extra punishment for parking search, walking and other car-related annoyances
			 Id<Link> vwGateFE1LinkID = Id.createLinkId("vw2");
			 Id<Link> vwGateFE2LinkID = Id.createLinkId("vw222");
			 Id<Link> vwGateNHSLinkID = Id.createLinkId("vw10");
			 Id<Link> vwGateWestLinkID = Id.createLinkId("vw7");
			 Id<Link> vwGateEastID = Id.createLinkId("vw14");
			 Id<Link> vwGateNorthID = Id.createLinkId("vwno");
			 Id<Link> vwGateNorthITVID = Id.createLinkId("46193");
			this.parkingLinks.add(vwGateFE1LinkID);
			this.parkingLinks.add(vwGateFE2LinkID);
			this.parkingLinks.add(vwGateNHSLinkID);
			this.parkingLinks.add(vwGateWestLinkID);
			this.parkingLinks.add(vwGateEastID);
			this.parkingLinks.add(vwGateNorthID);
			this.parkingLinks.add(vwGateNorthID);
			this.parkingLinks.add(vwGateNorthITVID);
			
			
			
			
		}

		@Override
		public void finish() {

		}

		@Override
		public double getScore() {
			return this.score;
		}


		protected double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double legScore = super.calcLegScore(departureTime, arrivalTime, leg);		
		if (leg.getMode().equals(TransportMode.car)){
			Route route = leg.getRoute();
		Id<Link> startLinkId = route.getStartLinkId() ;
		Id<Link> endLinkId = route.getEndLinkId();
		if (this.parkingLinks.contains(startLinkId)||this.parkingLinks.contains(endLinkId)){
			double punishment = 5*60+rand.nextInt(15*60);
			//minimum 5 minutes of walking time
			
			legScore += punishment*params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s;
		}
		
		}
		return legScore;
		}

	

		@Override
		public void handleLeg(Leg leg) {
			double legScore = calcLegScore(leg.getDepartureTime(), leg.getDepartureTime() + leg.getTravelTime(), leg);
			this.score += legScore;
			
			
		}



	}
	
}
