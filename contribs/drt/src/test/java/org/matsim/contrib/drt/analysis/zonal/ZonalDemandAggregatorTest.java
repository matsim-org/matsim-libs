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

import com.google.inject.internal.cglib.core.$MethodWrapper;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.examples.RunDrtExample;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.population.algorithms.TestsUtil;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;

public class ZonalDemandAggregatorTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	//TODO actually test something...
	public void EqualVehicleDensityZonalDemandAggregatorTest(){
		setupAndRun(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.EqualVehicleDensityZonalDemandAggregator);
	}

	@Test
	//TODO actually test something...
	public void PreviousIterationZonalDemandAggregatorTest(){
		setupAndRun(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.PreviousIterationZonalDemandAggregator);
	}

	@Test
	//TODO actually test something...
	public void ActivityLocationBasedZonalDemandAggregatorTest(){
		setupAndRun(MinCostFlowRebalancingParams.ZonalDemandAggregatorType.ActivityLocationBasedZonalDemandAggregator);
	}

	private void setupAndRun(MinCostFlowRebalancingParams.ZonalDemandAggregatorType aggregatorType) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "eight_shared_taxi_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		DrtConfigGroup drtCfg = DrtConfigGroup.getSingleModeDrtConfig(config);
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

		//this is the wrong way around (create controler before manipulationg scenario...
		Controler controler = DrtControlerCreator.createControlerWithSingleModeDrt(config, false);
		setupPopulation(controler.getScenario().getPopulation());

		controler.run();
	}


	/**
	 * we have eight zones, 2 rows 4 columns.
	 *
	 * 1) in the left column, there are half of the people, performing dummy - > car -> dummy
	 *    That should lead to half of the drt vehicles rebalanced to the left column when using ActivityLocationBasedZonalDemandAggregator.
	 * 2) in the right column, the other half of the people perform dummy -> drt -> dummy from top row to bottom row.
	 * 	  That should lead to all drt vehicles rebalanced to the right column when using PreviousIterationZonalDemandAggregator.
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

	}
}
