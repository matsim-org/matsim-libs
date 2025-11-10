/**
 * se.vti.atap.framework
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.minimalframework;

import java.util.Set;

/**
 * 
 * @author GunnarF
 *
 */
public class Runner<P extends Plan, A extends Agent<P>, T extends NetworkConditions> {

	private Set<A> agents = null;

	private NetworkLoading<A, T> networkLoading = null;

	private UtilityFunction<P, A, T> utilityFunction = null;

	private PlanInnovation<A, T> planInnovation = null;

	private PlanSelection<A, T> planSelection = null;

	private Integer maxIterations = null;

	private Logger<A, T> logger = null;

	private boolean verbose = true;
	
	public Runner() {
	}

	public Runner<P, A, T> setAgents(Set<A> agents) {
		this.agents = agents;
		return this;
	}

	public Runner<P, A, T> setNetworkLoading(NetworkLoading<A, T> networkLoading) {
		this.networkLoading = networkLoading;
		return this;
	}

	public Runner<P, A, T> setUtilityFunction(UtilityFunction<P, A, T> utilityFunction) {
		this.utilityFunction = utilityFunction;
		return this;
	}

	public Runner<P, A, T> setPlanInnovation(PlanInnovation<A, T> planInnovation) {
		this.planInnovation = planInnovation;
		return this;
	}

	public Runner<P, A, T> setPlanSelection(PlanSelection<A, T> planSelection) {
		this.planSelection = planSelection;
		return this;
	}

	public Runner<P, A, T> setIterations(int maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	public Runner<P, A, T> setLogger(Logger<A, T> logger) {
		this.logger = logger;
		return this;
	}
	
	public Runner<P,A,T> setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public void run() {

		this.agents.stream().forEach(a -> this.planInnovation.assignInitialPlan(a));

		for (int iteration = 0; iteration < this.maxIterations; iteration++) {
			if (this.verbose) {
				System.out.println("Iteration " + iteration + " of " + this.maxIterations);
			}

			T networkConditions = this.networkLoading.compute(this.agents);

			for (A agent : this.agents) {

				P currentPlan = agent.getCurrentPlan();
				currentPlan.setUtility(this.utilityFunction.compute(currentPlan, agent, networkConditions));

				this.planInnovation.assignCandidatePlan(agent, networkConditions);
				P candidatePlan = agent.getCandidatePlan();
				if (candidatePlan.getUtility() == null) {
					candidatePlan.setUtility(this.utilityFunction.compute(candidatePlan, agent, networkConditions));
				}

				if (candidatePlan.getUtility() < currentPlan.getUtility()) {
					agent.setCandidatePlan(currentPlan);
				}
			}

			this.logger.log(this.agents, networkConditions, iteration);

			if (iteration < this.maxIterations - 1) {
				this.planSelection.assignSelectedPlans(this.agents, networkConditions, iteration);
			}
		}
	}
}
