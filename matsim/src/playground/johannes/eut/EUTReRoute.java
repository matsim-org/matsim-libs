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

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author illenberger
 *
 */
public class EUTReRoute extends AbstractMultithreadedModule {
	
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
	public PlanAlgorithm getPlanAlgoInstance() {
		EUTRouter router = new EUTRouter(network, provider, utilFunction);
		router.setAnalyzer(analyzer);
		return new PlanAlgorithmDecorator(new PlansCalcRoute(network, router, router));
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
