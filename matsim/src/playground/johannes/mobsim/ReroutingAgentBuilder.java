/* *********************************************************************** *
 * project: org.matsim.*
 * ReroutingAgentBuilder.java
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
package playground.johannes.mobsim;

import org.matsim.core.population.PopulationImpl;


/**
 * @author illenberger
 *
 */
public class ReroutingAgentBuilder extends DeliberateAgentBuilder {

	public static ReroutingAgentBuilder newInstance(PopulationImpl population, RouteProviderBuilder builder) {
		ReroutingStrategyBuilder sBuilder = new ReroutingStrategyBuilder(builder);
		return new ReroutingAgentBuilder(population, sBuilder);
	}
	
	private ReroutingAgentBuilder(PopulationImpl population,	IntradayStrategyBuilder builder) {
		super(population, builder);
	}

	private static class ReroutingStrategyBuilder implements IntradayStrategyBuilder {

		private final RouteProviderBuilder rBuilder;
		
		public ReroutingStrategyBuilder(RouteProviderBuilder rBuilder) {
			this.rBuilder = rBuilder;
		}
		
		public IntradayStrategy newIntradayStrategy(PlanAgent agent) {
			return new ReroutingStrategy(agent,rBuilder.newRouteProvider(agent));
		}
		
	}
}
