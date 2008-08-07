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
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.router.PlansCalcRoute;

/**
 * @author illenberger
 *
 */
public class EUTReRoute2 extends MultithreadedModuleA {
	
//	private static final int rho = 0;
	
	private final ArrowPrattRiskAversionI utilFunction;
	
	private EUTRouterAnalyzer analyzer;
	
	private NetworkLayer network;
	
	private TwoStateTTKnowledge provider;
	
	
	public EUTReRoute2(NetworkLayer network, TwoStateTTKnowledge provider, double rho) {
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
	public PlanAlgorithm getPlanAlgoInstance() {
		EUTRouter2 router = new EUTRouter2(network, provider, utilFunction);
		router.setAnalyzer(analyzer);
		return new PlanAlgorithmDecorator(new PlansCalcRoute(router, router));
	}

//	@Override
//	public void handlePlan(Plan plan) {
//		analyzer.setNextPerson(plan.getPerson());
//		super.handlePlan(plan);
//	}

	private class PlanAlgorithmDecorator implements PlanAlgorithm {

		private PlanAlgorithm algo;
		
		public PlanAlgorithmDecorator(PlanAlgorithm algo) {
			this.algo = algo;
		}
		
		public void run(Plan plan) {
			analyzer.setNextPerson(plan.getPerson());
			algo.run(plan);
		}
		
	}
}
