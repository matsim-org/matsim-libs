/**
 * se.vti.atap.examples.minimalframework.parallel_links
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
package se.vti.atap.minimalframework.defaults;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.Plan;

/**
 * 
 * @author GunnarF
 *
 */
public class BasicAgent<P extends Plan> implements Agent<P> {

	private final String id;

	private P currentPlan = null;

	private P candidatePlan = null;

	public BasicAgent(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public P getCurrentPlan() {
		return this.currentPlan;
	}

	@Override
	public P getCandidatePlan() {
		return this.candidatePlan;
	}

	@Override
	public void setCurrentPlan(P plan) {
		this.currentPlan = plan;
	}

	@Override
	public void setCandidatePlan(P plan) {
		this.candidatePlan = plan;
	}

	@Override
	public void setCurrentPlanToCandidatePlan() {
		this.setCurrentPlan(this.candidatePlan);
	}
	
	@Override
	public double computeGap() {
		return this.getCandidatePlan().getUtility() - this.getCurrentPlan().getUtility();
	}
}
