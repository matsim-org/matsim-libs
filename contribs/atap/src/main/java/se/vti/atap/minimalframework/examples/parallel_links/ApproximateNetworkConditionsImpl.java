/**
 * se.vti.atap
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
import se.vti.atap.minimalframework.defaults.replannerselection.proposed.BasicPlanSwitch;

/**
 * 
 * @author GunnarF
 *
 */
public class ApproximateNetworkConditionsImpl
		extends AbstractApproximateNetworkConditions<PathFlows, AgentImpl, ApproximateNetworkConditionsImpl> {

	private final ApproximateNetworkLoadingImpl.DistanceType distanceType;

	private double distanceFactor = 1.0;

	private double[] linkFlows_veh = null;

	private double[] linkConditions = null;

	public ApproximateNetworkConditionsImpl(Set<AgentImpl> agentsUsingCurrentPlan,
			Set<AgentImpl> agentsUsingCandidatePlan, Network network,
			ApproximateNetworkLoadingImpl.DistanceType distanceType) {
		super(agentsUsingCurrentPlan, agentsUsingCandidatePlan, network);
		this.distanceType = distanceType;

		this.linkFlows_veh = new double[super.network.getNumberOfLinks()];
		this.linkConditions = new double[super.network.getNumberOfLinks()];

		for (int linkIndex = 0; linkIndex < super.network.getNumberOfLinks(); linkIndex++) {
			this.linkConditions[linkIndex] = this.computeLinkCondition(linkIndex);
		}

		super.switchAgentsIntoEmptyState(agentsUsingCurrentPlan, agentsUsingCandidatePlan);
	}

	private double computeLinkCondition(int linkIndex) {
		double flow_veh = this.linkFlows_veh[linkIndex];
		if (ApproximateNetworkLoadingImpl.DistanceType.FLOWS == this.distanceType) {
			return flow_veh;
		} else if (ApproximateNetworkLoadingImpl.DistanceType.CAPACITY_SCALED_FLOWS == this.distanceType) {
			return flow_veh
					* this.network.compute_dLinkTravelTime_dLinkFlow_s_veh(linkIndex, this.network.cap_veh[linkIndex]);
		} else if (ApproximateNetworkLoadingImpl.DistanceType.FLOW_SQUARED == this.distanceType) {
			return flow_veh * flow_veh;
		} else if (ApproximateNetworkLoadingImpl.DistanceType.TRAVELTIMES == this.distanceType) {
			return this.network.computeLinkTravelTime_s(linkIndex, flow_veh);
		} else {
			throw new UnsupportedOperationException("Unknown distance type: " + this.distanceType);
		}
	}

	@Override
	public double computeDistance(ApproximateNetworkConditionsImpl other) {
		double sumOfSquares = 0.0;
		for (int link = 0; link < this.linkConditions.length; link++) {
			double diff = this.linkConditions[link] - other.linkConditions[link];
			diff *= this.distanceFactor;
			sumOfSquares += diff * diff;
		}
		double result = Math.sqrt(sumOfSquares);
		return result;
	}

	@Override
	public BasicPlanSwitch<PathFlows, AgentImpl> switchToPlan(PathFlows newPlan, AgentImpl agent) {

		var planSwitch = new PlanSwitch(this.agent2plan.get(agent), newPlan, agent);

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

		for (int path = 0; path < agent.getNumberOfPaths(); path++) {
			int link = agent.availableLinks[path];
			this.linkConditions[link] = this.computeLinkCondition(link);
		}

		return planSwitch;
	}

	@Override
	public void undoPlanSwitch(BasicPlanSwitch<PathFlows, AgentImpl> undoSwitch) {

		AgentImpl agent = undoSwitch.getAgent();

		PathFlows oldPlan = undoSwitch.getOldPlan();

		var parallelLinksUndoSwitch = (PlanSwitch) undoSwitch;
		for (int path = 0; path < agent.getNumberOfPaths(); path++) {
			this.linkFlows_veh[agent.availableLinks[path]] = parallelLinksUndoSwitch.oldLinkFlowsOnPaths_veh[path];
			this.linkConditions[agent.availableLinks[path]] = parallelLinksUndoSwitch.oldLinkConditionsOnPaths[path];
		}

		this.agent2plan.put(agent, oldPlan);
	}
}
