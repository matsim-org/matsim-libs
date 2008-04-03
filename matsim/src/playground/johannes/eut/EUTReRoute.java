/* *********************************************************************** *
 * project: org.matsim.*
 * EUTReRoute.java
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

/**
 * 
 */
package playground.johannes.eut;

import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.PlansCalcRoute;

/**
 * @author illenberger
 *
 */
public class EUTReRoute extends MultithreadedModuleA {
	
//	private static final int rho = 0;
	
	private final ArrowPrattRiskAversionI utilFunction;
	
	private EUTRouterAnalyzer analyzer;
	
	private NetworkLayer network;
	
	private TravelTimeMemory provider;
	
	
	public EUTReRoute(NetworkLayer network, TravelTimeMemory provider, double rho) {
		super(1);
		utilFunction = new CARAFunction(rho);
		this.network = network;
		this.provider = provider;
	}

	public ArrowPrattRiskAversionI getUtilFunction() {
		return utilFunction;
	}

	public void setRouterAnalyzer(EUTRouterAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		EUTRouter router = new EUTRouter(network, provider, utilFunction);
		router.setAnalyzer(analyzer);
		return new PlanAlgorithmDecorator(new PlansCalcRoute(router, router));
	}

//	@Override
//	public void handlePlan(Plan plan) {
//		analyzer.setNextPerson(plan.getPerson());
//		super.handlePlan(plan);
//	}

	private class PlanAlgorithmDecorator implements PlanAlgorithmI {

		private PlanAlgorithmI algo;
		
		public PlanAlgorithmDecorator(PlanAlgorithmI algo) {
			this.algo = algo;
		}
		
		public void run(Plan plan) {
			analyzer.setNextPerson(plan.getPerson());
			algo.run(plan);
		}
		
	}
}
