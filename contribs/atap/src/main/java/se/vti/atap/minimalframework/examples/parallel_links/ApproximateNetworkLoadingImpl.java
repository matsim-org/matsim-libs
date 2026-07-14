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

import se.vti.atap.minimalframework.defaults.replannerselection.proposed.ApproximateNetworkLoading;

/**
 * 
 * @author GunnarF
 *
 */
public class ApproximateNetworkLoadingImpl implements
		ApproximateNetworkLoading<PathFlows, AgentImpl, NetworkConditionsImpl, ApproximateNetworkConditionsImpl> {

	public static enum DistanceType {
		FLOWS, CAPACITY_SCALED_FLOWS, FLOW_SQUARED, TRAVELTIMES
	}

	private final Network network;

	private final DistanceType distanceType;

	public ApproximateNetworkLoadingImpl(Network network, DistanceType distanceType) {
		this.network = network;
		this.distanceType = distanceType;
	}

	@Override
	public ApproximateNetworkConditionsImpl compute(Set<AgentImpl> agentsUsingCurrentPlan,
			Set<AgentImpl> agentsUsingCandidatePlan, NetworkConditionsImpl networkConditions) {
		return new ApproximateNetworkConditionsImpl(agentsUsingCurrentPlan, agentsUsingCandidatePlan, this.network,
				this.distanceType);
	}

}
