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
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.config.groups.StrategyConfigGroup;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.NetworkUtils;

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
		
	private static final String networkFileOld = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoNetworkOldRenamed.xml";
	
	public static final String networkFileNew = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoNetwork.xml";
	
//	public static final String networkFile 	= networkFileOld;

	public static final String networkFile 	= networkFileNew;
	
	private static final String plans1Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoPlans.xml";

	private static final String plans2Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoPlansAltRoute.xml";

//	public static final String config1Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfig.xml";
//
//	public static final String config2Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfigAltRoute.xml";

	public static final String config1Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfig.xml";

	public static final String config2Out = DgPaths.VSPSVNBASE + "studies/dgrether/cmcf/daganzoConfigAltRoute.xml";

	
	public static String configOut, plansOut;

	private static final boolean isAlternativeRouteEnabled = false;
	
	private static final int iterations = 500;
	
	private static final int iterations2 = 0;
	
//	private static final int iterations = 1;
	
	private Population plans;

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
		config.charyparNagelScoring().setPerforming(6.0);
		//this is unfortunately not working at all....
		ActivityParams homeParams = new ActivityParams("h");
//		homeParams.setOpeningTime(0);
		config.charyparNagelScoring().addActivityParams(homeParams);
		//set it with f. strings
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0", "24:00:00");
		
		//configure controler
	// configure controler
		config.controler().setTraveltimeBinSize(1);
		
		config.controler().setLastIteration(iterations + iterations2);
		if (isAlternativeRouteEnabled)
			config.controler().setOutputDirectory(DgPaths.WSBASE + "testData/output/cmcfAltRoute");
		else 
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
		reRoute.setDisableAfter(iterations);
		config.strategy().addStrategySettings(reRoute);
		
		MatsimIo.writerConfig(config, configOut);

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
		config = new Config();
		Gbl.setConfig(config);
		config.addCoreModules();

		config.plans().setOutputVersion("v4");
		config.plans().setOutputFile(plansOut);

		this.network = MatsimIo.loadNetwork(networkFile);
	}

	private void createPlans() throws Exception {
		this.plans = new Population(false);
		int firstHomeEndTime = 0;//6 * 3600;
		int homeEndTime = firstHomeEndTime;
		Link l1 = network.getLink(IdFactory.get(1));
		Link l6 = network.getLink(IdFactory.get(6));
		
		for (int i = 1; i <= 3600; i++) {
			Person p = new PersonImpl(new IdImpl(i));
			Plan plan = new Plan(p);
			p.addPlan(plan);
			//home
//			homeEndTime = homeEndTime +  ((i - 1) % 3);
			if ((i-1) % 3 == 0){
				homeEndTime++;				
			}
			Act act1 = plan.createAct("h", l1.getCenter());
			act1.setLink(l1);
			act1.setEndTime(homeEndTime);
			//leg to home
			Leg leg = plan.createLeg(BasicLeg.Mode.car);
			CarRoute route = new NodeCarRoute(l1, l6);
			if (isAlternativeRouteEnabled) {
				route.setNodes(l1, NetworkUtils.getNodes(this.network, "2 3 4 5 6"), l6);
			}
			else {
				route.setNodes(l1, NetworkUtils.getNodes(this.network, "2 3 5 6"), l6);
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
			new CMCFScenarioGenerator();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
