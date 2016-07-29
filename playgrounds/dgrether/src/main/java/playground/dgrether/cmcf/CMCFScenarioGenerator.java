/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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

package playground.dgrether.cmcf;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFScenarioGenerator {


	private static final Logger log = Logger
			.getLogger(CMCFScenarioGenerator.class);

	private static final String networkFileOld = DgPaths.REPOS + "studies/dgrether/cmcf/daganzoNetworkOldRenamed.xml";

	public static final String networkFileNew = DgPaths.REPOS + "studies/dgrether/cmcf/daganzoNetwork.xml";

//	public static final String networkFile 	= networkFileOld;

	public static final String networkFile 	= networkFileNew;

	private static final String plans1Out = DgPaths.REPOS + "studies/dgrether/cmcf/daganzoPlans.xml";

	private static final String plans2Out = DgPaths.REPOS + "studies/dgrether/cmcf/daganzoPlansAltRoute.xml";

//	public static final String config1Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfig.xml";
//
//	public static final String config2Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfigAltRoute.xml";

	public static final String config1Out = DgPaths.REPOS + "studies/dgrether/cmcf/daganzoConfig.xml";

	public static final String config2Out = DgPaths.REPOS + "studies/dgrether/cmcf/daganzoConfigAltRoute.xml";


	public static String configOut, plansOut;

	private static final boolean isAlternativeRouteEnabled = false;

	private static final int iterations = 500;

	private static final int iterations2 = 0;

//	private static final int iterations = 1;

	private Population plans;

	private Config config;

	private Network network;

	public CMCFScenarioGenerator() throws Exception {
		init();
		createPlans();
		MatsimIo.writePlans(this.plans, this.network, plansOut);
		//set scenario
		this.config.network().setInputFile(networkFile);
		this.config.plans().setInputFile(plansOut);
		//configure scoring for plans
		this.config.planCalcScore().setLateArrival_utils_hr(0.0);
		this.config.planCalcScore().setPerforming_utils_hr(6.0);
		//this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
//		homeParams.setOpeningTime(0);
		this.config.planCalcScore().addActivityParams(homeParams);
		//set it with f. strings
		this.config.planCalcScore().addParam("activityType_0", "h");
		this.config.planCalcScore().addParam("activityTypicalDuration_0", "24:00:00");

		//configure controler
	// configure controler
		this.config.travelTimeCalculator().setTraveltimeBinSize(1);

		this.config.controler().setLastIteration(iterations + iterations2);
		if (isAlternativeRouteEnabled)
			this.config.controler().setOutputDirectory(DgPaths.WSBASE + "testData/output/cmcfAltRoute");
		else
			this.config.controler().setOutputDirectory(DgPaths.WSBASE + "testData/output/cmcf");

		//configure simulation and snapshot writing
		this.config.controler().setSnapshotFormat(Arrays.asList("otfvis"));
		this.config.qsim().setSnapshotPeriod(60.0);
		//configure strategies for replanning
		this.config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(Id.create(1, StrategySettings.class));
		selectExp.setWeight(0.9);
		selectExp.setStrategyName("SelectExpBeta");
		this.config.strategy().addStrategySettings(selectExp);

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(Id.create(2, StrategySettings.class));
		reRoute.setWeight(0.1);
		reRoute.setStrategyName("ReRoute");
		reRoute.setDisableAfter(iterations);
		this.config.strategy().addStrategySettings(reRoute);

		MatsimIo.writeConfig(this.config, configOut);

		log.info("scenario written!");
	}

	private void init() {
		if (isAlternativeRouteEnabled) {
			plansOut = plans2Out;
			configOut = config2Out;
		}
		else {
			plansOut = plans1Out;
			configOut = config1Out;
		}
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = scenario.getConfig();

		this.network = scenario.getNetwork();
		MatsimIo.loadNetwork(networkFile, scenario);
	}

	private void createPlans() throws Exception {
		this.plans = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		int firstHomeEndTime = 0;//6 * 3600;
		int homeEndTime = firstHomeEndTime;
		Link l1 = this.network.getLinks().get(Id.create(1, Link.class));
		Link l6 = this.network.getLinks().get(Id.create(6, Link.class));

		for (int i = 1; i <= 3600; i++) {
			Person p = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PopulationUtils.createPlan(p);
			p.addPlan(plan);
			//home
//			homeEndTime = homeEndTime +  ((i - 1) % 3);
			if ((i-1) % 3 == 0){
				homeEndTime++;
			}
			Activity act1 = PopulationUtils.createAndAddActivityFromCoord(plan, (String) "h", l1.getCoord());
			act1.setLinkId(l1.getId());
			act1.setEndTime(homeEndTime);
			//leg to home
			Leg leg = PopulationUtils.createAndAddLeg( plan, (String) TransportMode.car );
			NetworkRoute route = new LinkNetworkRouteImpl(l1.getId(), l6.getId());
			if (isAlternativeRouteEnabled) {
				route.setLinkIds(l1.getId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(this.network, "2 3 4 5 6"))), l6.getId());
			}
			else {
				route.setLinkIds(l1.getId(), NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(NetworkUtils.getNodes(this.network, "2 3 5 6"))), l6.getId());
			}
			leg.setRoute(route);
			Activity act2 = PopulationUtils.createAndAddActivityFromCoord(plan, (String) "h", l6.getCoord());
			act2.setLinkId(l6.getId());
			this.plans.addPerson(p);
		}
	}





	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new CMCFScenarioGenerator();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
