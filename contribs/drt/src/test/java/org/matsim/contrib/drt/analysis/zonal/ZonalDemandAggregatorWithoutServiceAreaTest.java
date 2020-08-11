/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.contrib.drt.analysis.zonal;

import java.net.URL;
import java.util.function.ToIntFunction;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class ZonalDemandAggregatorWithoutServiceAreaTest {

	//TODO write test with service area !!
	// (with an service are, demand estimation zones are not spread over the entire network but restricted to the service are (plus a little surrounding))

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void EqualVehicleDensityZonalDemandAggregatorTest(){
		Controler controler = setupControler(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.EqualVehicleDensity, "");
		controler.run();
		ZonalDemandAggregator aggregator = controler.getInjector().getInstance(DvrpModes.key(ZonalDemandAggregator.class, "drt"));
		for(double ii = 0; ii < 16 * 3600; ii+=1800){
			ToIntFunction<String> demandFunction = aggregator.getExpectedDemandForTimeBin(ii + 60); //inside DRT, the demand is actually estimated for rebalancing time + 60 seconds..
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 1", 1, demandFunction.applyAsInt("1"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 2", 1, demandFunction.applyAsInt("2"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 3", 1, demandFunction.applyAsInt("3"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 4", 1, demandFunction.applyAsInt("4"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 5", 1, demandFunction.applyAsInt("5"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 6", 1, demandFunction.applyAsInt("6"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 7", 1, demandFunction.applyAsInt("7"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 8", 1, demandFunction.applyAsInt("8"), MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void PreviousIterationZonalDemandAggregatorTest(){
		Controler controler = setupControler(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.PreviousIteration, "");
		controler.run();
		ZonalDemandAggregator aggregator = controler.getInjector().getInstance(DvrpModes.key(ZonalDemandAggregator.class, "drt"));
		for(double ii = 1800; ii < 16 * 3600; ii+=1800){
			ToIntFunction<String> demandFunction = aggregator.getExpectedDemandForTimeBin(ii + 60); //inside DRT, the demand is actually estimated for rebalancing time + 60 seconds..
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 1", 0, demandFunction.applyAsInt("1"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 2", 0, demandFunction.applyAsInt("2"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 3", 0, demandFunction.applyAsInt("3"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 4", 0, demandFunction.applyAsInt("4"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 5", 0, demandFunction.applyAsInt("5"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 6", 0, demandFunction.applyAsInt("6"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 7", 0, demandFunction.applyAsInt("7"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 8", 3, demandFunction.applyAsInt("8"), MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void PreviousIterationZonalDemandAggregatorWithSpeedUpModeTest(){
		Controler controler = setupControler(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.PreviousIteration, "drt_teleportation");
		controler.run();
		ZonalDemandAggregator aggregator = controler.getInjector().getInstance(DvrpModes.key(ZonalDemandAggregator.class, "drt"));
		for(double ii = 1800; ii < 16 * 3600; ii+=1800){
			ToIntFunction<String> demandFunction = aggregator.getExpectedDemandForTimeBin(ii + 60); //inside DRT, the demand is actually estimated for rebalancing time + 60 seconds..
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 1", 0, demandFunction.applyAsInt("1"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 2", 0, demandFunction.applyAsInt("2"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 3", 0, demandFunction.applyAsInt("3"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 4", 3, demandFunction.applyAsInt("4"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 5", 0, demandFunction.applyAsInt("5"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 6", 0, demandFunction.applyAsInt("6"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 7", 0, demandFunction.applyAsInt("7"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 8", 3, demandFunction.applyAsInt("8"), MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void ActivityLocationBasedZonalDemandAggregatorTest(){
		Controler controler = setupControler(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.TimeDependentActivityBased, "");
		controler.run();
		ZonalDemandAggregator aggregator = controler.getInjector().getInstance(DvrpModes.key(ZonalDemandAggregator.class, "drt"));
		for(double ii = 1800; ii < 16 * 3600; ii+=1800){
			ToIntFunction<String> demandFunction = aggregator.getExpectedDemandForTimeBin(ii + 60); //inside DRT, the demand is actually estimated for rebalancing time + 60 seconds..
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 1", 0, demandFunction.applyAsInt("1"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 2", 3, demandFunction.applyAsInt("2"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 3", 0, demandFunction.applyAsInt("3"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 4", 3, demandFunction.applyAsInt("4"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 5", 0, demandFunction.applyAsInt("5"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 6", 0, demandFunction.applyAsInt("6"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 7", 0, demandFunction.applyAsInt("7"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 8", 3, demandFunction.applyAsInt("8"), MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void FleetSizeWeightedByPopulationShareDemandAggregatorTest(){
		Controler controler = setupControler(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.FleetSizeWeightedByPopulationShare, "");
		controler.run();
		ZonalDemandAggregator aggregator = controler.getInjector().getInstance(DvrpModes.key(ZonalDemandAggregator.class, "drt"));
		for(double ii = 0; ii < 16 * 3600; ii+=1800){
			ToIntFunction<String> demandFunction = aggregator.getExpectedDemandForTimeBin(ii + 60); //inside DRT, the demand is actually estimated for rebalancing time + 60 seconds..
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 1", 0, demandFunction.applyAsInt("1"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 2", 99, demandFunction.applyAsInt("2"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 3", 0, demandFunction.applyAsInt("3"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 4", 99, demandFunction.applyAsInt("4"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 5", 0, demandFunction.applyAsInt("5"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 6", 0, demandFunction.applyAsInt("6"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 7", 0, demandFunction.applyAsInt("7"), MatsimTestUtils.EPSILON);
			Assert.assertEquals("wrong estimation of demand at time=" + (ii+60) + " in zone 8", 99, demandFunction.applyAsInt("8"), MatsimTestUtils.EPSILON);
		}
	}

	private Controler setupControler(MinCostFlowRebalancingParams.ZonalDemandAggregatorType aggregatorType, String drtSpeedUpModeForRebalancingConfiguration) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "eight_shared_taxi_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtCfg.setDrtSpeedUpMode(drtSpeedUpModeForRebalancingConfiguration);
		MinCostFlowRebalancingParams rebalancingParams = new MinCostFlowRebalancingParams();
		rebalancingParams.setCellSize(500);
		rebalancingParams.setTargetAlpha(1);
		rebalancingParams.setTargetBeta(0);
		drtCfg.addParameterSet(rebalancingParams);
		rebalancingParams.setZonalDemandAggregatorType(aggregatorType);

		drtCfg.setChangeStartLinkToLastLinkInSchedule(false); //do not take result from last iteration...

		config.controler().setLastIteration(1);
		config.qsim().setStartTime(0.);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		PlansCalcRouteConfigGroup.ModeRoutingParams pseudoDrtSpeedUpModeRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("drt_teleportation");
		pseudoDrtSpeedUpModeRoutingParams.setBeelineDistanceFactor(1.3);
		pseudoDrtSpeedUpModeRoutingParams.setTeleportedModeSpeed(8.0);
		config.plansCalcRoute().addModeRoutingParams(pseudoDrtSpeedUpModeRoutingParams);

		// if adding a new mode (drtSpeedUpMode), some default modes are deleted, so re-insert them...
		PlansCalcRouteConfigGroup.ModeRoutingParams walkRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.walk);
		walkRoutingParams.setBeelineDistanceFactor(1.3);
		walkRoutingParams.setTeleportedModeSpeed(3.0 / 3.6);
		config.plansCalcRoute().addModeRoutingParams(walkRoutingParams);

		PlanCalcScoreConfigGroup.ModeParams pseudoDrtSpeedUpModeScoreParams = new PlanCalcScoreConfigGroup.ModeParams("drt_teleportation");
		config.planCalcScore().addModeParams(pseudoDrtSpeedUpModeScoreParams);

		//this is the wrong way around (create controler before manipulating scenario...
		Controler controler = DrtControlerCreator.createControler(config, false);
		setupPopulation(controler.getScenario().getPopulation());
		return controler;
	}


	/**
	 * we have eight zones, 2 rows 4 columns.
	 * order of zones:
	 *	2	4	6	8
	 *	1	3	5	7
	 *
	 * 1) in the left column, there are half of the people, performing dummy - > car -> dummy
	 *    That should lead to half of the drt vehicles rebalanced to the left column when using TimeDependentActivityBasedZonalDemandAggregator.
	 * 2) in the right column, the other half of the people perform dummy -> drt -> dummy from top row to bottom row.
	 * 	  That should lead to all drt vehicles rebalanced to the right column when using PreviousIterationZonalDRTDemandAggregator.
	 * 3) in the center, there is nothing happening.
	 *    But, when using EqualVehicleDensityZonalDemandAggregator, one vehicle should get sent to every zone..
	 */
	private void setupPopulation(Population population){
		//delete what's there
		population.getPersons().clear();

		PopulationFactory factory = population.getFactory();

		Id<Link> left1 = Id.createLinkId(344);
		Id<Link> left2 = Id.createLinkId(112);

		for(int i = 1; i < 100; i++){
			Person person = factory.createPerson(Id.createPersonId("leftColumn_" + i));

			Plan plan = factory.createPlan();
			Activity dummy1 = factory.createActivityFromLinkId("dummy", left1);
			dummy1.setEndTime(i * 10 * 60);
			plan.addActivity(dummy1);

			plan.addLeg(factory.createLeg(TransportMode.car));
			plan.addActivity(factory.createActivityFromLinkId("dummy", left2));

			person.addPlan(plan);
			population.addPerson(person);
		}

		Id<Link> right1 = Id.createLinkId(151);
		Id<Link> right2 = Id.createLinkId(319);

		for(int i = 1; i < 100; i++){
			Person person = factory.createPerson(Id.createPersonId("rightColumn_" + i));

			Plan plan = factory.createPlan();
			Activity dummy1 = factory.createActivityFromLinkId("dummy", right1);
			dummy1.setEndTime(i * 10 * 60);
			plan.addActivity(dummy1);

			plan.addLeg(factory.createLeg(TransportMode.drt));
			plan.addActivity(factory.createActivityFromLinkId("dummy", right2));

			person.addPlan(plan);
			population.addPerson(person);
		}

		Id<Link> center1 = Id.createLinkId(147);
		Id<Link> center2 = Id.createLinkId(315);

		for(int i = 1; i < 100; i++){
			Person person = factory.createPerson(Id.createPersonId("centerColumn_" + i));

			Plan plan = factory.createPlan();
			Activity dummy1 = factory.createActivityFromLinkId("dummy", center1);
			dummy1.setEndTime(i * 10 * 60);
			plan.addActivity(dummy1);

			plan.addLeg(factory.createLeg("drt_teleportation"));
			plan.addActivity(factory.createActivityFromLinkId("dummy", center2));

			person.addPlan(plan);
			population.addPerson(person);
		}
	}

}
