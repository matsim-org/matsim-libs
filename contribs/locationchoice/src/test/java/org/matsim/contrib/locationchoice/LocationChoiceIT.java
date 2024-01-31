/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoice.java
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

package org.matsim.contrib.locationchoice;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype;
import org.matsim.contrib.locationchoice.frozenepsilons.FrozenTastesConfigGroup;
import org.matsim.contrib.locationchoice.timegeography.LocationChoicePlanStrategy;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import com.google.inject.Inject;

public class LocationChoiceIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * This is, as far as I can see, testing the {@link LocationChoicePlanStrategy}.  It will use the algo from the config, which is "random".  It is thus not using the frozen
	 * epsilon approach.  kai, mar'19
	 */
	@Test
	void testLocationChoice() {

		final Config config = localCreateConfig( utils.getPackageInputDirectory() + "config2.xml");

		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		// setup network
		Network network = scenario.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, (double) 1000, (double) 10, (double) 3600, (double) 1 );
		ActivityFacility facility1 = ((ActivityFacilitiesImpl) scenario.getActivityFacilities()).createAndAddFacility(Id.create(1, ActivityFacility.class), new Coord((double) 0, (double) 500));
		facility1.addActivityOption(new ActivityOptionImpl("initial-work"));
		ActivityFacility facility2 = ((ActivityFacilitiesImpl) scenario.getActivityFacilities()).createAndAddFacility(Id.create(2, ActivityFacility.class), new Coord((double) 0, (double) 400));
		facility2.addActivityOption(new ActivityOptionImpl("work"));
		ActivityFacility facility3 = ((ActivityFacilitiesImpl) scenario.getActivityFacilities()).createAndAddFacility(Id.create(3, ActivityFacility.class), new Coord((double) 0, (double) 300));
		facility3.addActivityOption(new ActivityOptionImpl("work"));

		Person person = localCreatePopWOnePerson(scenario, facility1, 17.*60.*60. );

		Controler controler = new Controler(scenario);

		// set locachoice strategy:
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				addPlanStrategyBinding("MyLocationChoice").toProvider(new jakarta.inject.Provider<PlanStrategy>() {
					@Inject TimeInterpretation timeInterpretation;

					@Override
					public PlanStrategy get() {
						return new LocationChoicePlanStrategy(scenario, tripRouterProvider, timeInterpretation);
					}
				});
			}
		});
		// (this is now only necessary since the config for all three tests sets MyLocationChoice instead of LocationChoice. Probably
		// should pull the best response test away from the other (old) test.  kai, feb'13

		controler.getConfig().controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		controler.run();

		// test that everything worked as expected
		// The initial facility is *not* part of the choice set (its supported activity type is called "initial-work" for that reason)
		// so that the test can notice that there is a difference. In my earlier attempt, the random facility chosen would always be the one
		// on which I already am, so the test was no good.
		// Secondly, I need to give it two facilities to choose from, because a choice set of size 1 is treated specially
		// (it is assumed that the one element is the one I'm already on, so nothing is done).
		// I tricked it. :-)   michaz
		assertEquals(2, person.getPlans().size(), "number of plans in person.");
		Plan newPlan = person.getSelectedPlan();
		Activity newWork = (Activity) newPlan.getPlanElements().get(2);
		if (!config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none) ) {
			newWork = (Activity) newPlan.getPlanElements().get(6);
		}
		assertNotNull( newWork ) ;
		assertNotNull( newWork.getFacilityId() ) ;
		assertTrue(newWork.getFacilityId().equals(Id.create(2, ActivityFacility.class)) || newWork.getFacilityId().equals(Id.create(3, ActivityFacility.class)));
	}

	/**
	 * setup population with one person
	 * @param workActEndTime TODO
	 */
	public static Person localCreatePopWOnePerson( Scenario scenario, ActivityFacility facility1, double workActEndTime ) {

		Population population = scenario.getPopulation();

		Person person = population.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);

		Plan plan = population.getFactory().createPlan();
		person.addPlan(plan) ;

		{
			Activity act = population.getFactory().createActivityFromCoord("home", new Coord((double) 0, (double) 0)) ;
			//		act.setLinkId(link.getId());
			act.setEndTime(8.0 * 3600);
			plan.addActivity(act) ;
		}
		plan.addLeg(population.getFactory().createLeg(TransportMode.car)) ;
		{
			Activity act = population.getFactory().createActivityFromCoord("work", scenario.getActivityFacilities().getFacilities().get(facility1.getId()).getCoord() ) ;
			act.setEndTime(workActEndTime);
			((Activity)act).setFacilityId(facility1.getId());
			plan.addActivity(act) ;
		}
		plan.addLeg(population.getFactory().createLeg(TransportMode.car)) ;
		{
			//		act = plan.createAndAddActivity("home", new CoordImpl(0, 0));
			//		act.setLinkId(link.getId());
			Activity act = population.getFactory().createActivityFromCoord("home", new Coord((double) 0, (double) 0)) ;
			plan.addActivity(act) ;
		}

		return person;
	}

	static Config localCreateConfig( String configFileName ) {
		// setup config
		Config config = ConfigUtils.loadConfig(configFileName, new DestinationChoiceConfigGroup() , new FrozenTastesConfigGroup() ) ;

		config.global().setNumberOfThreads(0);
		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(1);
		config.controller().setMobsim("qsim");
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue) ;

		final DestinationChoiceConfigGroup dccg = ConfigUtils.addOrGetModule(config, DestinationChoiceConfigGroup.class ) ;
		dccg.setAlgorithm(Algotype.random );
		dccg.setFlexibleTypes("work" );

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(12*60*60);
		config.scoring().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(12*60*60);
		config.scoring().addActivityParams(work);
		ActivityParams shop = new ActivityParams("shop");
		shop.setTypicalDuration(1.*60*60);
		config.scoring().addActivityParams(shop);

		final StrategySettings strategySettings = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettings.setStrategyName("MyLocationChoice");
		strategySettings.setWeight(1.0);
		config.replanning().addStrategySettings(strategySettings);

		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setEffectiveLaneWidth(1.) ;
		config.qsim().setLinkWidthForVis((float)1.) ;
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true) ;
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawNonMovingItems(true) ;

		return config;
	}

	class FactoryForMobsimWithOTFVis implements MobsimFactory {
		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			QSim qSim = new QSimBuilder(sc.getConfig()).useDefaults().build(sc, eventsManager);
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
			OTFClientLive.run(sc.getConfig(), server);
			return qSim ;
		}
	}

}
