/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerConfigLoaderTest.java
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
package playground.mfeil;


import org.matsim.core.controler.Controler;


/**
 * @author Matthias Feil
 * Extends the current StrategyManagerConfigLoader in order to include an additional 
 * strategy. DEPRECATED!
 */

@Deprecated
public class StrategyManagerConfigLoaderTest extends org.matsim.core.replanning.StrategyManagerConfigLoader {


	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param controler the {@link Controler} that provides miscellaneous data for the replanning modules
	 * @param config the {@link Config} object containing the configuration for the strategyManager
	 * @param manager the {@link StrategyManager} to be configured according to the configuration
	 */
	
	public StrategyManagerConfigLoaderTest(){
		super();
	}
	
//	@Override
//	public void load(final Controler controler, final Config config, final StrategyManager manager) {
//	
//		NetworkLayer network = controler.getNetwork();
//		TravelCost travelCostCalc = controler.getTravelCostCalculator();
//		TravelTime travelTimeCalc = controler.getTravelTimeCalculator();
//		LegTravelTimeEstimator legTravelTimeEstimator = controler.getLegTravelTimeEstimator();
//		Facilities facilities = controler.getFacilities();
//		
//		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());
//	
//		int externalCounter = 0;
//	
//		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
//			double rate = settings.getProbability();
//			if (rate == 0.0) {
//				continue;
//			}
//			String classname = settings.getModuleName();
//	
//			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
//				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
//			}
//			PlanStrategy strategy = null;
//			if (classname.equals("KeepLastSelected")) {
//				strategy = new PlanStrategy(new KeepSelected());
//			} else if (classname.equals("ReRoute") || classname.equals("threaded.ReRoute")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				strategy.addStrategyModule(new ReRoute(controler));
//			} else if (classname.equals("ReRoute_Dijkstra")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				strategy.addStrategyModule(new ReRouteDijkstra(network, travelCostCalc, travelTimeCalc));
//			} else if (classname.equals("ReRoute_Landmarks")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
//				preProcessRoutingData.run(network);
//				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
//			} else if (classname.equals("TimeAllocationMutator") || classname.equals("threaded.TimeAllocationMutator")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				strategy.addStrategyModule(new TimeAllocationMutator());
//			} else if (classname.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				strategy.addStrategyModule(new TimeAllocationMutator(7200));
//				PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
//				preProcessRoutingData.run(network);
//				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
//			} else if (classname.equals("ExternalModule")) {
//				externalCounter++;
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				String exePath = settings.getExePath();
//				strategy.addStrategyModule(new ExternalModule(exePath, "ext" + externalCounter));
//			} else if (classname.equals("PlanomatExe")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				String exePath = settings.getExePath();
//				strategy.addStrategyModule(new PlanomatExe(exePath));
//				strategy.addStrategyModule(new ReRoute(controler));
//			} else if (classname.equals("PlanomatTimeRouteExe")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				String exePath = settings.getExePath();
//				strategy.addStrategyModule(new PlanomatExe(exePath));
//			} else if (classname.equals("Planomat")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				StrategyModule planomatStrategyModule = new PlanomatOptimizeTimes(legTravelTimeEstimator);
//				strategy.addStrategyModule(planomatStrategyModule);
	//			setDecayingModuleProbability(manager, strategy, 100, rate); // FIXME [KM] Why "100" and not controler.firstIteration as in "PlanomatReRoute"
//		
//			} else if (classname.equals("PlanomatX")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				StrategyModule planomatStrategyModule = new PlanomatXInitialiser(legTravelTimeEstimator);
//				strategy.addStrategyModule(planomatStrategyModule);
	//			setDecayingModuleProbability(manager, strategy, 100, rate); // FIXME [KM] Why "100" and not controler.firstIteration as in "PlanomatReRoute"
//
//			} else if (classname.equals("PlanomatReRoute")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				StrategyModule planomatStrategyModule = new PlanomatOptimizeTimes(legTravelTimeEstimator);
//				strategy.addStrategyModule(planomatStrategyModule);
//				strategy.addStrategyModule(new ReRoute(controler));
	//			setDecayingModuleProbability(manager, strategy, Gbl.getConfig().controler().getFirstIteration(), rate);
//			} else if (classname.equals("BestScore")) {
//				strategy = new PlanStrategy(new BestPlanSelector());
//			} else if (classname.equals("SelectExpBeta")) {
//				strategy = new PlanStrategy(new ExpBetaPlanSelector());
//			} else if (classname.equals("ChangeExpBeta")) {
//				strategy = new PlanStrategy(new ExpBetaPlanChanger());
//			} else if (classname.equals("SelectRandom")) {
//				strategy = new PlanStrategy(new RandomPlanSelector());
//			} else if (classname.equals("SelectPathSizeLogit")) {
//				strategy = new PlanStrategy(new PathSizeLogitSelector());
//				// JH
//			} else if (classname.equals("SNSecLoc")){
	//			System.out.println(" #### Choosing social network replanning algorithm");
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				StrategyModule socialNetStrategyModule= new SNRandomFacilitySwitcher(network, travelCostCalc, travelTimeCalc);
//				strategy.addStrategyModule(socialNetStrategyModule);
//			} else if (classname.equals("SecLoc")){
//				strategy = new PlanStrategy(new RandomPlanSelector());
//				StrategyModule socialNetStrategyModule= new RandomFacilitySwitcher(network, travelCostCalc, travelTimeCalc, facilities);
//				strategy.addStrategyModule(socialNetStrategyModule);
//			} else if (classname.equals("LocationChoice")) {
//		    	strategy = new PlanStrategy(new RandomPlanSelector());
//		    	strategy.addStrategyModule(new LocationChoice(controler.getNetwork(), controler));
//				final PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
//				preProcessRoutingData.run(network);
//				strategy.addStrategyModule(new ReRouteLandmarks(network, travelCostCalc, travelTimeCalc, preProcessRoutingData));
//				strategy.addStrategyModule(new TimeAllocationMutator());
//			}
//			//if none of the strategies above could be selected we try to load the class by name
//			else {
//				//classes loaded by name must not be part of the matsim core
//				if (classname.startsWith("org.matsim")) {
	//				log.error("Strategies in the org.matsim package must not be loaded by name!");
//				}
//				else {
//					try {
//						Class[] args = {Controler.class};
//						Class<? extends PlanStrategy> klas = (Class<? extends PlanStrategy>) Class.forName(classname);
//						Constructor<? extends PlanStrategy> c = klas.getConstructor(args);
//						strategy = c.newInstance(controler);
//					} catch (ClassNotFoundException e) {
//						e.printStackTrace();
//					} catch (InstantiationException e) {
//						e.printStackTrace();
//					} catch (IllegalAccessException e) {
//						e.printStackTrace();
//					} catch (SecurityException e) {
//						e.printStackTrace();
//					} catch (NoSuchMethodException e) {
//						e.printStackTrace();
//					} catch (IllegalArgumentException e) {
//						e.printStackTrace();
//					} catch (InvocationTargetException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//	
//			if (strategy == null) {
//				Gbl.errorMsg("Could not initialize strategy named " + classname);
//			}
//	
//			manager.addStrategy(strategy, rate);
//	
//			// now check if this modules should be disabled after some iterations
//			if (settings.getDisableAfter() >= 0) {
//				int maxIter = settings.getDisableAfter();
//				if (maxIter >= config.controler().getFirstIteration()) {
//					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
//				} else {
//					/* The controler starts at a later iteration than this change request is scheduled for.
//					 * make the change right now.					 */
//					manager.changeStrategy(strategy, 0.0);
//				}
//			}
//		}
//	}
}
