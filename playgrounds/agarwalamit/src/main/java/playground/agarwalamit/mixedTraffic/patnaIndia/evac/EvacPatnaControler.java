/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;

/**
 * @author amit
 */

public class EvacPatnaControler {
	
	private final boolean isUsingSeepage = false;
	
	public static void main(String[] args) {
		EvacPatnaControler evacPatna = new EvacPatnaControler();
		Scenario sc = evacPatna.getPatnaEvacScenario(); 
		
		sc.getConfig().qsim().setUseDefaultVehicles(false);
		((ScenarioImpl) sc).createVehicleContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
		car.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("car"));
		car.setPcuEquivalents(1.0);
		modesType.put("car", car);
		sc.getVehicles().addVehicleType(car);

		VehicleType motorbike = VehicleUtils.getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
		motorbike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("motorbike"));
		motorbike.setPcuEquivalents(0.25);
		modesType.put("motorbike", motorbike);
		sc.getVehicles().addVehicleType(motorbike);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
		bike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("bike"));
		bike.setPcuEquivalents(0.25);
		modesType.put("bike", bike);
		sc.getVehicles().addVehicleType(bike);

		VehicleType walk = VehicleUtils.getFactory().createVehicleType(Id.create("walk",VehicleType.class));
		walk.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("walk"));
		//		walk.setPcuEquivalents(0.10);  			
		modesType.put("walk",walk);
		sc.getVehicles().addVehicleType(walk);

		VehicleType pt = VehicleUtils.getFactory().createVehicleType(Id.create("pt",VehicleType.class));
		pt.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("pt"));
		//		pt.setPcuEquivalents(5);  			
		modesType.put("pt",pt);
		sc.getVehicles().addVehicleType(pt);

		for(Person p:sc.getPopulation().getPersons().values()){
			Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
			String travelMode = null;
			for(PlanElement pe :p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg) {
					travelMode = ((Leg)pe).getMode();
					break;
				}
			}
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId,modesType.get(travelMode));
			sc.getVehicles().addVehicle(vehicle);
		}
		
		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		controler.setDumpDataAtEnd(true);

		if(evacPatna.isUsingSeepage){
			controler.setMobsimFactory(new SeepageMobsimfactory());
		}
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl)controler.getScenario()));
		controler.run();
		
	}
	
	private Scenario getPatnaEvacScenario(){
		Collection <String> mainModes = Arrays.asList("car","motorbike","bike");
		
		String dir = "../../../repos/runs-svn/patnaIndia/";
		
		String networkFile = dir+"/run105/input/evac_network.xml.gz";
		String plansFile = dir+"/run105/input/evac_plans.xml.gz";
		String outputDir;
		
		if(isUsingSeepage) outputDir = dir+"run105/evac_seepage/";
		else outputDir = dir+"run105/evac_passing/";
		
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(plansFile, networkFile);
		
		//add config parameters
		Config config = sc.getConfig();
		
		config.controler().setMobsim("qsim");
		config.controler().setOutputDirectory(outputDir);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setWritePlansInterval(20);
		config.controler().setWriteEventsInterval(20);
		
		config.qsim().setEndTime(100*3600);
		config.qsim().setFlowCapFactor(0.011);
		config.qsim().setStorageCapFactor(0.011*3);
		config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ.name());
		config.qsim().setTrafficDynamics(QSimConfigGroup.TRAFF_DYN_W_HOLES);
		config.qsim().setMainModes(mainModes);
		
		StrategySettings reRoute = new StrategySettings(Id.create("1",StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);
		
		StrategySettings expChangeBeta = new StrategySettings(Id.create("2",StrategySettings.class));
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.9);
		
		config.strategy().addStrategySettings(expChangeBeta);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		
		//===vsp defaults
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.setParam("TimeAllocationMutator", "mutationAffectsDuration", "false");
		config.setParam("TimeAllocationMutator", "mutationRange", "7200");
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		//===
		
		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(homeAct);
		
		ActivityParams evacAct = new ActivityParams("evac");
		evacAct.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(evacAct);
		
		config.plansCalcRoute().setNetworkModes(mainModes);
		
		ModeRoutingParams walk = new ModeRoutingParams("walk");
		walk.setTeleportedModeSpeed(4/3.6);
		walk.setBeelineDistanceFactor(1.);
		config.plansCalcRoute().addModeRoutingParams(walk);
		
		ModeRoutingParams pt = new ModeRoutingParams("pt");
		pt.setTeleportedModeSpeed(20/3.6);
		pt.setBeelineDistanceFactor(1.);
		config.plansCalcRoute().addModeRoutingParams(pt);
		
		
		if(isUsingSeepage){
			config.setParam("seepage", "isSeepageAllowed", "true");
			config.setParam("seepage", "seepMode", "bike");
			config.setParam("seepage", "isSeepModeStorageFree", "false");
		}
	return sc;
	}
}
