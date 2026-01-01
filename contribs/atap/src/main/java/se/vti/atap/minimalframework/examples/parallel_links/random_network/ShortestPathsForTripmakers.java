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
package se.vti.atap.minimalframework.examples.parallel_links.random_network;

import java.util.Random;

import se.vti.atap.minimalframework.PlanInnovation;
import se.vti.atap.minimalframework.examples.parallel_links.AgentImpl;
import se.vti.atap.minimalframework.examples.parallel_links.Network;
import se.vti.atap.minimalframework.examples.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.examples.parallel_links.PathFlows;

/**
 * 
 * @author GunnarF
 *
 */
public class ShortestPathsForTripmakers implements PlanInnovation<AgentImpl, NetworkConditionsImpl> {

	private final NetworkConditionsImpl initialNetworkConditions;

	private Random rnd = null;

	public ShortestPathsForTripmakers(Network network) {
		this.initialNetworkConditions = NetworkConditionsImpl.createEmptyNetworkConditions(network);
	}

	public ShortestPathsForTripmakers setRandomizing(Random rnd) {
		this.rnd = rnd;
		return this;
	}

	@Override
	public void assignInitialPlan(AgentImpl tripMaker) {
		if (this.rnd != null) {
			tripMaker
					.setCurrentPlan(new PathFlows(tripMaker.computeRandomPath(this.rnd), tripMaker.getNumberOfPaths()));
		} else {
			tripMaker.setCurrentPlan(new PathFlows(tripMaker.computeBestPath(this.initialNetworkConditions),
					tripMaker.getNumberOfPaths()));
		}
	}

	@Override
	public void assignCandidatePlan(AgentImpl tripMaker, NetworkConditionsImpl networkConditions) {
		if (this.rnd != null) {
			// attention, may be worse than the current plan
			tripMaker
					.setCandidatePlan(new PathFlows(tripMaker.computeRandomPath(this.rnd), tripMaker.getNumberOfPaths()));
		} else {
			tripMaker.setCandidatePlan(
					new PathFlows(tripMaker.computeBestPath(networkConditions), tripMaker.getNumberOfPaths()));
		}
	}
}
