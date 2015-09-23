/* *********************************************************************** *
 * project: org.matsim.*
 * SocialCostsController.java
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

package playground.christoph.socialcosts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

/**
 * E
 *  
 * @author cdobler
 */
public class SocialCostsController {
	
	public static void main(String[] args) {
		
		Controler controler = null;
		if (args.length == 0) {
			controler = new Controler(initSampleScenario());
		} else controler = new Controler(args);
		
		/*
		 * Scoring also has to take the social costs into account.
		 * This cannot be moved to the initializer since the scoring functions
		 * are created even before the startup event is created.
		 */
		controler.setScoringFunctionFactory(new TimeAndMoneyDependentScoringFunctionFactory());
		
		Initializer initializer = new Initializer();
		controler.addControlerListener(initializer);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}
	
	/*
	 * Use the existing Sioux Falls scenario but change some config parameters.
	 * Additionally add two more activities to all agents' plans (by default,
	 * only home activities are contained).
	 */
	private static Scenario initSampleScenario() {
		Config config = ConfigUtils.loadConfig("../../matsim/examples/evacuation-tutorial/withinDayEvacuationConf.xml");
		config.network().setInputFile("../../matsim/examples/evacuation-tutorial/siouxfalls_net.xml.gz");
		config.plans().setInputFile("../../matsim/examples/evacuation-tutorial/siouxfalls_plans.xml.gz");
		config.controler().setOutputDirectory("../../matsim/examples/evacuation-tutorial/output");
		config.controler().setLastIteration(50);
		config.qsim().setStuckTime(10);
		config.qsim().setFlowCapFactor(0.15);
		config.qsim().setStorageCapFactor(0.5);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		Id[] linkIds = new Id[scenario.getNetwork().getLinks().size()];
		scenario.getNetwork().getLinks().keySet().toArray(linkIds);
		
		int roundRobin = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				if (plan.getPlanElements().size() == 1) {
					Activity homeActivity = ((Activity) plan.getPlanElements().get(0));
					homeActivity.setMaximumDuration(homeActivity.getEndTime() - homeActivity.getStartTime());
					
					Leg leg = factory.createLeg(TransportMode.car);
					plan.addLeg(leg);
					
					Activity activity = factory.createActivityFromLinkId("w", linkIds[roundRobin % linkIds.length]);
					activity.setStartTime(homeActivity.getEndTime());
					activity.setEndTime(activity.getStartTime() + 8*3600);
					plan.addActivity(activity);
					roundRobin++;
					
					Leg leg2 = factory.createLeg(TransportMode.car);
					plan.addLeg(leg2);
					
					Activity activity2 = factory.createActivityFromLinkId("h", homeActivity.getLinkId());
					activity2.setStartTime(activity.getEndTime());
					plan.addActivity(activity2);
				}
			}
		}
		
		return scenario;
	}
	
	/*
	 * Initialize the necessary components for the social cost calculation.
	 */
	private static class Initializer implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			Controler controler = event.getControler();
			
			// initialize the social costs calculator
            SocialCostCalculator scc = new SocialCostCalculator(controler.getScenario().getNetwork(), controler.getEvents(), controler.getLinkTravelTimes());
			controler.addControlerListener(scc);
			controler.getEvents().addHandler(scc);
			
			// initialize the social costs disutility calculator
			final SocialCostTravelDisutilityFactory factory = new SocialCostTravelDisutilityFactory(scc);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(factory);
				}
			});

			// create a plot containing the mean travel times
			Set<String> transportModes = new HashSet<String>();
			transportModes.add(TransportMode.car);
			MeanTravelTimeCalculator mttc = new MeanTravelTimeCalculator(controler.getScenario(), transportModes);
			controler.addControlerListener(mttc);
			controler.getEvents().addHandler(mttc);
		}
	}
	
	private static class SocialCostTravelDisutility implements TravelDisutility {

		private final TravelTime travelTime;
		private final SocialCostCalculator scc;
		
		public SocialCostTravelDisutility(TravelTime travelTime, SocialCostCalculator scc) {
			this.travelTime = travelTime;
			this.scc = scc;
		}
		
		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			double disutility = 0.0;
	
			disutility += this.travelTime.getLinkTravelTime(link, time, person, vehicle);
			disutility += this.scc.getLinkTravelDisutility(link, time, person, vehicle); 
			return disutility;
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException();
		}

	}
	
	private static class SocialCostTravelDisutilityFactory implements TravelDisutilityFactory {

		private final SocialCostCalculator scc;
		
		public SocialCostTravelDisutilityFactory(SocialCostCalculator scc) {
			this.scc = scc;
		}
		
		@Override
		public TravelDisutility createTravelDisutility(TravelTime travelTime, PlanCalcScoreConfigGroup cnScoringGroup) {
			return new SocialCostTravelDisutility(travelTime, scc);
		}
	}
}
