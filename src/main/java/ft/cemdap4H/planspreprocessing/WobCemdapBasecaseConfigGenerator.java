/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package ft.cemdap4H.planspreprocessing;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class WobCemdapBasecaseConfigGenerator {
	public static void main(String[] args) {
		String basefolder = "D:/cemdap-vw/";
		new WobCemdapBasecaseConfigGenerator().run(basefolder,1.0,1.0);
	}
	
	public void run(String basefolder, double flowCap, double storageCap){		
		Config config = ConfigUtils.loadConfig(basefolder+"cemdap_output/activityConfig.xml");
		
	
		//network
		config.network().setInputFile("input/networkpt-av-nov17_cleaned.xml.gz");
		config.counts().setInputFile("input/counts_added_bs_wvi.xml");
	
		config.transit().setTransitScheduleFile("input/transitschedule.xml");
		config.transit().setUseTransit(true);
		config.transit().setVehiclesFile("input/transitvehicles.xml");
		
		ControlerConfigGroup ccg = config.controler();
		ccg.setRunId("vw203"+"."+flowCap);
		ccg.setOutputDirectory("output/"+ccg.getRunId()+"/");
		ccg.setFirstIteration(0);
		int lastIteration = 300;
		ccg.setLastIteration(lastIteration);
		ccg.setMobsim("qsim");
		ccg.setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		ccg.setWriteEventsInterval(100);
		ccg.setWritePlansInterval(100);
		config.global().setNumberOfThreads(16);
		
		QSimConfigGroup qsc = config.qsim();
		qsc.setUsingFastCapacityUpdate(true);
		qsc.setTrafficDynamics(TrafficDynamics.withHoles);
		qsc.setNumberOfThreads(6);
		qsc.setStorageCapFactor(storageCap);
		qsc.setFlowCapFactor(flowCap);
		qsc.setEndTime(30*3600);
		
		config.parallelEventHandling().setNumberOfThreads(6);
		
		config.counts().setCountsScaleFactor(1.0/flowCap);
		
		config.plans().setInputFile("cemdap_output/mergedPlans_filtered_"+flowCap+".xml.gz");
		Counts<Link> counts = new Counts<>();
		
		new MatsimCountsReader(counts).readFile(basefolder+"/input/counts_added_bs_wvi.xml");
		
		Set<String> countLinks = new HashSet<>();
		for (Id<Link> lid : counts.getCounts().keySet()){
			countLinks.add(lid.toString());
		}
		CadytsConfigGroup cadyts = new CadytsConfigGroup();
		config.addModule(cadyts);
		cadyts.setStartTime(6*3600);
		cadyts.setEndTime(21*3600+1);
		cadyts.setTimeBinSize(3600);
		cadyts.setCalibratedItems(countLinks);
//		cadyts.setFreezeIteration(freezeIteration);
		
		ModeParams car = config.planCalcScore().getModes().get(TransportMode.car);
		car.setMonetaryDistanceRate(-0.0001);
		car.setMarginalUtilityOfTraveling(-5);
		car.setConstant(-5);
		
		ModeParams ride = config.planCalcScore().getModes().get(TransportMode.ride);
		ride.setMonetaryDistanceRate(-0.0001);
		ride.setMarginalUtilityOfTraveling(-5);
		ride.setConstant(-6);
		
		ModeParams pt = config.planCalcScore().getModes().get(TransportMode.pt);
		pt.setMarginalUtilityOfTraveling(-0.5);
		pt.setConstant(-1.5);
	
		ModeParams walk= config.planCalcScore().getModes().get(TransportMode.walk);
		walk.setMarginalUtilityOfTraveling(-2);
		
		ModeParams bike = config.planCalcScore().getModes().get(TransportMode.bike);
		bike.setMarginalUtilityOfTraveling(-6);
		bike.setConstant(-2);
		
		ModeRoutingParams bikeP = config.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.bike);
		bikeP.setBeelineDistanceFactor(1.3);
		bikeP.setTeleportedModeSpeed(3.333);
		
		ModeRoutingParams walkP = config.plansCalcRoute().getOrCreateModeRoutingParams(TransportMode.walk);
		walkP.setBeelineDistanceFactor(1.3);
		walkP.setTeleportedModeSpeed(1.0);
		
		StrategySettings subtour = new StrategySettings();
		subtour.setStrategyName(DefaultStrategy.SubtourModeChoice.toString());
		subtour.setWeight(0.1);
		config.strategy().addStrategySettings(subtour);
		
		StrategySettings reroute = new StrategySettings();
		reroute.setStrategyName(DefaultStrategy.ReRoute.toString());
		reroute.setWeight(0.1);
		config.strategy().addStrategySettings(reroute);
		
		StrategySettings timeAllocation = new StrategySettings();
		timeAllocation.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
		timeAllocation.setWeight(0.1);
		config.strategy().addStrategySettings(timeAllocation);
		
		StrategySettings timeAllocationReroute = new StrategySettings();
		timeAllocationReroute.setStrategyName(DefaultStrategy.TimeAllocationMutator_ReRoute.toString());
		timeAllocationReroute.setWeight(0.1);
		config.strategy().addStrategySettings(timeAllocationReroute);
		
		StrategySettings changeExpBeta = new StrategySettings();
		changeExpBeta.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
		changeExpBeta.setWeight(0.6);
		config.strategy().addStrategySettings(changeExpBeta);
		
		
		config.strategy().setFractionOfIterationsToDisableInnovation(.8);
		
		config.strategy().setMaxAgentPlanMemorySize(8);
		config.timeAllocationMutator().setMutationRange(7200);
		
		//these activities are not really used, because types are usually with typed timing. However, they exist in the config for various reasons
		config.planCalcScore().getActivityParams("home").setTypicalDuration(14*3600);
		config.planCalcScore().getActivityParams("work").setTypicalDuration(8*3600);
		config.planCalcScore().getActivityParams("education").setTypicalDuration(8*3600);
		config.planCalcScore().getActivityParams("other").setTypicalDuration(1*3600);
		config.planCalcScore().getActivityParams("shopping").setTypicalDuration(1*3600);
		config.planCalcScore().getActivityParams("leisure").setTypicalDuration(1*3600);
		
		config.subtourModeChoice().setConsiderCarAvailability(true);
		config.subtourModeChoice().setModes(new String[]{"car","bike","walk","pt","ride"});
		
		new ConfigWriter(config).write(basefolder+"/config_"+flowCap+".xml");

		
		
	}
}
