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
package playground.jbischoff.taxibus.scenario.run;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.jbischoff.taxibus.run.configuration.ConfigBasedTaxibusLaunchUtils;
import playground.jbischoff.taxibus.run.configuration.TaxibusConfigGroup;
import playground.jbischoff.taxibus.scenario.analysis.quick.TaxiBusTravelTimesAnalyzer;

/**
 * @author jbischoff
 *
 */
public class RunTaxibusPolicyCase {

	public static void main(String[] args) {

		String configFileName = "../../../shared-svn/projects/vw_rufbus/scenario/input/configs/ConfigPC85.xml";
//		String configFileName = "D:/runs-svn/vw_rufbus/VW60ML50/VW60ML50.output_config.xml.gz";
		if (args.length>0){
			configFileName = args[0];
		}
		Config config = ConfigUtils.loadConfig(configFileName, new TaxibusConfigGroup());
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

		new ConfigBasedTaxibusLaunchUtils(controler).initiateTaxibusses();

		controler.setScoringFunctionFactory(new ScoringFunctionFactory(){
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sum = new SumScoringFunction() ;

				// Score activities, legs, payments and being stuck
				// with the default MATSim scoring based on utility parameters in the config file.
				final CharyparNagelScoringParameters params =
						new CharyparNagelScoringParameters.Builder(scenario, person.getId()).build();
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
		
		public MyLegScoring(final CharyparNagelScoringParameters params, Network network) {
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
