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

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.config.groups.StrategyConfigGroup;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationImpl;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.NetworkUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.IdFactory;
import playground.dgrether.utils.MatsimIo;

/**
 * @author dgrether
 *
 */
public class CMCFScenarioGeneratorNoReroute {

	private static final Logger log = Logger
			.getLogger(CMCFScenarioGeneratorNoReroute.class);

	private static final String networkFileOld = DgPaths.VSPSVNBASE
			+ "studies/dgrether/cmcf/daganzoNetworkOldRenamed.xml";

	public static final String networkFileNew = DgPaths.VSPSVNBASE
			+ "studies/dgrether/cmcf/daganzoNetworkLessBottleneck.xml";

	// public static final String networkFile = networkFileOld;

	public static final String networkFile = networkFileNew;

	private static final String plans1Out = DgPaths.VSPSVNBASE
			+ "studies/dgrether/cmcf/daganzoPlansNoReroute.xml";

	private static final String plans2Out = DgPaths.VSPSVNBASE
			+ "studies/dgrether/cmcf/daganzoPlansAltRouteNoReroute.xml";

	// public static final String config1Out = DgPaths.VSPSVNBASE +
	// "studies/dgrether/cmcf/daganzoConfig.xml";
	//
	// public static final String config2Out = DgPaths.VSPSVNBASE +
	// "studies/dgrether/cmcf/daganzoConfigAltRoute.xml";

	public static final String config1Out = DgPaths.VSPSVNBASE
			+ "studies/dgrether/cmcf/daganzoConfigNoReroute.xml";

	public static final String config2Out = DgPaths.VSPSVNBASE
			+ "studies/dgrether/cmcf/daganzoConfigAltRouteNoReroute.xml";

	public static String configOut, plansOut;

	private static final boolean isAlternativeRouteEnabled = true;

	private static final int iterations = 100;

	private static final int iterations2 = 0;

	// private static final int iterations = 1;

	private Population plans;

	private Config config;

	private NetworkLayer network;

	public CMCFScenarioGeneratorNoReroute() throws Exception {
		init();
		createPlans();
		MatsimIo.writePlans(this.plans, plansOut);
		// set scenario
		this.config.network().setInputFile(networkFile);
		this.config.plans().setInputFile(plansOut);
		// configure scoring for plans
		this.config.charyparNagelScoring().setLateArrival(0.0);
		this.config.charyparNagelScoring().setPerforming(6.0);
		// this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
		// homeParams.setOpeningTime(0);
		this.config.charyparNagelScoring().addActivityParams(homeParams);
		// set it with f. strings
		this.config.charyparNagelScoring().addParam("activityType_0", "h");
		this.config.charyparNagelScoring().addParam("activityTypicalDuration_0",
				"24:00:00");

		// configure controler
		this.config.controler().setTraveltimeBinSize(1);

//		config.controler().setTravelTimeCalculatorType("TravelTimeCalculatorHashMap".intern());
		this.config.controler().setLastIteration(iterations + iterations2);
		if (isAlternativeRouteEnabled)
			this.config.controler().setOutputDirectory(
					DgPaths.WSBASE + "testData/output/cmcfNewAltRouteNoReroute");
		else
			this.config.controler().setOutputDirectory(
					DgPaths.WSBASE + "testData/output/cmcfNewNoReroute");

		// configure simulation and snapshot writing
		this.config.simulation().setSnapshotFormat("otfvis");
		this.config.simulation().setSnapshotFile("cmcf.mvi");
		this.config.simulation().setSnapshotPeriod(60.0);
		// configure strategies for replanning
		this.config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(
				IdFactory.get(1));
		selectExp.setProbability(0.9);
		selectExp.setModuleName("SelectExpBeta");
		this.config.strategy().addStrategySettings(selectExp);

		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(
				IdFactory.get(2));
		reRoute.setProbability(0.10);
		reRoute.setModuleName("ReRoute");
		reRoute.setDisableAfter(iterations);
		this.config.strategy().addStrategySettings(reRoute);

		MatsimIo.writerConfig(this.config, configOut);

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
		this.config = new Config();
		Gbl.setConfig(this.config);
		this.config.addCoreModules();

		this.config.plans().setOutputVersion("v4");
		this.config.plans().setOutputFile(plansOut);

		this.network = MatsimIo.loadNetwork(networkFile);
	}

	private void createPlans() throws Exception {
		this.plans = new PopulationImpl(false);
		int firstHomeEndTime = 0;// 6 * 3600;
		int homeEndTime = firstHomeEndTime;
		Link l1 = this.network.getLink(IdFactory.get(1));
		Link l6 = this.network.getLink(IdFactory.get(6));

		for (int i = 1; i <= 7200; i++) {
			Person p = new PersonImpl(new IdImpl(i));
			Plan plan = new org.matsim.population.PlanImpl(p);
			p.addPlan(plan);
			// home % 2
			homeEndTime = homeEndTime + firstHomeEndTime + ((i - 1) % 2);
			Act act1 = plan.createAct("h", l1.getCenter());
			act1.setLink(l1);
			act1.setEndTime(homeEndTime);
			// leg to home
			Leg leg = plan.createLeg(BasicLeg.Mode.car);
			CarRoute route = new NodeCarRoute(l1, l6);
			if (!isAlternativeRouteEnabled) {
				route.setNodes(l1, NetworkUtils.getNodes(this.network, "2 3 5 6"), l6);
			}
			if (isAlternativeRouteEnabled) {
				route.setNodes(l1, NetworkUtils.getNodes(this.network, "2 3 4 5 6"), l6);
			}
			leg.setRoute(route);
			Act act2 = plan.createAct("h", l6.getCenter());
			act2.setLink(l6);
			this.plans.addPerson(p);
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new CMCFScenarioGeneratorNoReroute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
