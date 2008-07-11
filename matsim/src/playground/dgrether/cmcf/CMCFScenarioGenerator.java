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
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.IdFactory;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFScenarioGenerator {

	
	private static final Logger log = Logger
			.getLogger(CMCFScenarioGenerator.class);
		
	private static final String networkFile = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoNetwork.xml";

	private static final String plansOut = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoPlans.xml";

	public static final String configOut = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfig.xml";

	private Plans plans;

	private Config config;

	private NetworkLayer network;

	public CMCFScenarioGenerator() throws Exception {
		init();
		createPlans();
		MatsimIo.writePlans(plans, plansOut);
		//set scenario
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(plansOut);
		//configure scoring for plans
		config.charyparNagelScoring().setLateArrival(0.0);
		config.charyparNagelScoring().setPerforming(0.0);
		//this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
//		homeParams.setOpeningTime(0);
		config.charyparNagelScoring().addActivityParams(homeParams);
		//set it with f. strings
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0", "24:00:00");
		
		//configure controler
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(DgPaths.WSBASE + "testData/output/cmcf");
		//configure simulation and snapshot writing
		config.simulation().setSnapshotFormat("otfvis");
		config.simulation().setSnapshotFile("cmcf.mvi");
		config.simulation().setSnapshotPeriod(60.0);
		//configure strategies for replanning
		config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(IdFactory.get(1));
		selectExp.setProbability(0.9);
		selectExp.setModuleName("SelectExpBeta");
		config.strategy().addStrategySettings(selectExp);
		
		StrategyConfigGroup.StrategySettings reRoute = new StrategyConfigGroup.StrategySettings(IdFactory.get(2));
		reRoute.setProbability(0.1);
		reRoute.setModuleName("ReRoute");
		config.strategy().addStrategySettings(reRoute);
		
		MatsimIo.writerConfig(config, configOut);

		log.info("scenario written!");
	}

	private void init() {
		config = new Config();
		Gbl.setConfig(config);
		config.addCoreModules();

		config.plans().setOutputVersion("v4");
		config.plans().setOutputFile(plansOut);

		this.network = MatsimIo.loadNetwork(networkFile);
	}

	private void createPlans() throws Exception {
		this.plans = new Plans(false);
		int homeEndtime = 6 * 3600;
		Link l1 = network.getLink(IdFactory.get(1));
		Link l6 = network.getLink(IdFactory.get(6));
		
		for (int i = 1; i <= 3600; i++) {
			Person p = new Person(new IdImpl(i));
			Plan plan = new Plan(p);
			p.addPlan(plan);
			//home
			homeEndtime += i - 1;
			plan.createAct("h", l1.getCenter().getX(), l1.getCenter().getY(), l1, 0.0, homeEndtime, i, false);
			//leg to home
			Leg leg = plan.createLeg("car", null, null, null);
//			Route route = new Route();
//			route.setRoute("2 4 5");
//			leg.setRoute(route);
			plan.createAct("h", l6.getCenter().getX(), l6.getCenter().getY(), l6, 0.0, 0.0, 0.0, false);
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
