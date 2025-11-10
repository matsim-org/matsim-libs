/**
 * se.vti.atap.examples.minimalframework.parallel_links.ods
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
package se.vti.atap.minimalframework.examples.parallel_links;

import java.util.Set;

import se.vti.atap.minimalframework.defaults.replannerselection.proposed.AbstractApproximateNetworkConditions;
import se.vti.atap.minimalframework.defaults.replannerselection.proposed.PlanSwitch;

/**
 * 
 * @author GunnarF
 *
 */
public class ApproximateNetworkConditionsImpl
		extends AbstractApproximateNetworkConditions<PathFlows, AgentImpl, ApproximateNetworkConditionsImpl> {

	private double[] linkFlows_veh;
	private double[] linkConditions;

//	private final boolean useCostsForDistance;
	private final double costWeight;
	private final double distanceFactor;
	private final boolean capacityScale;
	private final boolean squareFlow;

	public ApproximateNetworkConditionsImpl(Set<AgentImpl> agentsUsingCurrentPlan,
			Set<AgentImpl> agentsUsingCandidatePlan, Network network, double costWeight, double distanceFactor,
			boolean capacityScale, boolean squareFlow) {
		super(agentsUsingCurrentPlan, agentsUsingCandidatePlan, network);
//		this.useCostsForDistance = useCostsForDistances;
		this.costWeight = costWeight;
		this.distanceFactor = distanceFactor;
		this.capacityScale = capacityScale;
		this.squareFlow = squareFlow;

		this.linkFlows_veh = new double[super.network.getNumberOfLinks()];
		this.linkConditions = new double[super.network.getNumberOfLinks()];

		for (int linkIndex = 0; linkIndex < super.network.getNumberOfLinks(); linkIndex++) {
			this.linkConditions[linkIndex] = (1.0 - this.costWeight) * this.flow(linkIndex)
					+ this.costWeight * this.cost(linkIndex);
//					* this.costScale
//					* (super.network.computeLinkTravelTime_s(linkIndex, this.linkFlows_veh[linkIndex])
//							+ this.linkFlows_veh[linkIndex] * super.network
//									.compute_dLinkTravelTime_dLinkFlow_s_veh(linkIndex, this.linkFlows_veh[linkIndex]));
		}
		super.switchAgentsIntoEmptyState(agentsUsingCurrentPlan, agentsUsingCandidatePlan);
	}

	private double flow(int linkIndex) {
		double flow = this.linkFlows_veh[linkIndex];
		if (this.squareFlow) {
			flow = flow * flow;
		}
		if (this.capacityScale) {
			flow *= this.network.t0_s[linkIndex] * this.network.alpha[linkIndex] * this.network.beta[linkIndex]
					/ this.network.cap_veh[linkIndex];
		}
		return flow;
	}

	private double cost(int linkIndex) {
//		double factor = this.network.t0_s[linkIndex] * this.network.alpha[linkIndex] * this.network.beta[linkIndex]
//				/ Math.pow(this.network.cap_veh[linkIndex], this.network.beta[linkIndex]);
//		return factor * this.linkFlows_veh[linkIndex];

		return this.linkFlows_veh[linkIndex]
				* this.network.computeLinkTravelTime_s(linkIndex, this.linkFlows_veh[linkIndex]);
//		return this.network.computeLinkTravelTime_s(linkIndex, this.linkFlows_veh[linkIndex]);

	}

//	@Override
//	private void ensureInitializedInternalState() {
//		if (this.linkFlows_veh == null) {
//			this.linkFlows_veh = new double[super.network.getNumberOfLinks()];
//			this.linkConditions = new double[super.network.getNumberOfLinks()];
//
//			if (this.useCostsForDistance) {
//				for (int linkIndex = 0; linkIndex < super.network.getNumberOfLinks(); linkIndex++) {
//					this.linkConditions[linkIndex] = this.conditionScale
//							* super.network.computeLinkTravelTime_s(linkIndex, 0.0);
//				}
//			}
//		}
//	}

	@Override
	public double computeDistance(ApproximateNetworkConditionsImpl other) {

//		this.ensureInitializedInternalState();
//		other.ensureInitializedInternalState();

		double sumOfSquares = 0.0;
		for (int link = 0; link < this.linkConditions.length; link++) {
			double diff = this.linkConditions[link] - other.linkConditions[link];
			diff *= this.distanceFactor;
			sumOfSquares += diff * diff;
		}
		double result = Math.sqrt(sumOfSquares);
//		System.out.println("  " + result);
		return result;
	}

	@Override
	public PlanSwitch<PathFlows, AgentImpl> switchToPlan(PathFlows newPlan, AgentImpl agent) {

		var planSwitch = new PlanSwitch<>(this.agent2plan.get(agent), newPlan, agent);

		planSwitch.oldLinkFlowsOnPaths_veh = new double[agent.getNumberOfPaths()];
		planSwitch.oldLinkConditionsOnPaths = new double[agent.getNumberOfPaths()];
		for (int path = 0; path < agent.getNumberOfPaths(); path++) {
			int link = agent.availableLinks[path];
			planSwitch.oldLinkFlowsOnPaths_veh[path] = this.linkFlows_veh[link];
			planSwitch.oldLinkConditionsOnPaths[path] = this.linkConditions[link];
		}

		if (newPlan != null) {
			double[] pathFlows_veh = newPlan.computePathFlows_veh();
			this.agent2plan.put(agent, newPlan);
			for (int path = 0; path < agent.getNumberOfPaths(); path++) {
				this.linkFlows_veh[agent.availableLinks[path]] += pathFlows_veh[path];
			}
		} else {
			this.agent2plan.remove(agent);
		}

		if (planSwitch.getOldPlan() != null) {
			double[] pathFlows_veh = planSwitch.getOldPlan().computePathFlows_veh();
			for (int path = 0; path < agent.getNumberOfPaths(); path++) {
				this.linkFlows_veh[agent.availableLinks[path]] -= pathFlows_veh[path];
			}
		}

//		for (int linkIndex = 0; linkIndex < super.network.getNumberOfLinks(); linkIndex++) {
//			this.linkConditions[linkIndex] = (1.0 - this.costWeight) * this.flow(linkIndex)
//					+ this.costWeight * this.cost(linkIndex);
//		}
		for (int path = 0; path < agent.getNumberOfPaths(); path++) {
			int link = agent.availableLinks[path];
			this.linkConditions[link] = (1.0 - this.costWeight) * this.flow(link)
					+ this.costWeight * this.cost(link);
		}

		return planSwitch;
	}

	@Override
	public void undoSwitch(PlanSwitch<PathFlows, AgentImpl> undoSwitch) {

		AgentImpl agent = undoSwitch.getAgent();

		PathFlows oldPlan = undoSwitch.getOldPlan();
//		if (oldPlan != null) {
//			for (int path = 0; path < agent.getNumberOfPaths(); path++) {
//				this.linkFlows_veh[agent.availableLinks[path]] += oldPlan.pathFlows_veh[path];
//			}
//		}
//		PathFlows newPlan = undoSwitch.getNewPlan();
//		if (newPlan != null) {
//			for (int path = 0; path < agent.getNumberOfPaths(); path++) {
//				this.linkFlows_veh[agent.availableLinks[path]] -= newPlan.pathFlows_veh[path];
//			}
//		}
//		this.linkFlows_veh = Arrays.copyOf(undoSwitch.oldLinkFlows_veh, undoSwitch.oldLinkFlows_veh.length);

		for (int path = 0; path < agent.getNumberOfPaths(); path++) {
			this.linkFlows_veh[agent.availableLinks[path]] = undoSwitch.oldLinkFlowsOnPaths_veh[path];
			this.linkConditions[agent.availableLinks[path]] = undoSwitch.oldLinkConditionsOnPaths[path];
		}

//		for (int linkIndex = 0; linkIndex < super.network.getNumberOfLinks(); linkIndex++) {
//			this.linkConditions[linkIndex] = (1.0 - this.costWeight) * this.flow(linkIndex)
//					+ this.costWeight * this.cost(linkIndex);
//		}
//		for (int path = 0; path < agent.getNumberOfPaths(); path++) {
//			int link = agent.availableLinks[path];
//			this.linkConditions[link] = (1.0 - this.costWeight) * this.flow(link)
//					+ this.costWeight * this.cost(link);
//		}

		this.agent2plan.put(agent, oldPlan);
	}
}
