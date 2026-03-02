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
package se.vti.atap.minimalframework.defaults.replannerselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.NetworkConditions;
import se.vti.atap.minimalframework.PlanSelection;

/**
 * 
 * @author GunnarF
 *
 */
public class OneAtATimePlanSelection<A extends Agent<?>, T extends NetworkConditions> implements PlanSelection<A, T> {

	private final Random rnd;

	private List<A> agents = null;

	private int lastAgentIndex = -1;

	public OneAtATimePlanSelection(Random rnd) {
		this.rnd = rnd;
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {

		if (this.agents == null) {
			this.agents = Collections.unmodifiableList(new ArrayList<>(agents));
		}
		assert (this.agents.containsAll(agents) && agents.containsAll(this.agents));

		if (this.rnd != null) {
			this.lastAgentIndex = this.rnd.nextInt(0, this.agents.size());
		} else {
			this.lastAgentIndex++;
			if (this.lastAgentIndex == this.agents.size()) {
				this.lastAgentIndex = 0;
			}
		}

		this.agents.get(this.lastAgentIndex).setCurrentPlanToCandidatePlan();
	}
}
