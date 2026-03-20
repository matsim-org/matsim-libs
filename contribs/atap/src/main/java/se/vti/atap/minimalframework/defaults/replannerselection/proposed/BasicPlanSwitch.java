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
package se.vti.atap.minimalframework.defaults.replannerselection.proposed;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.Plan;

/**
 * 
 * @author GunnarF
 *
 */
public class BasicPlanSwitch<P extends Plan, A extends Agent<P>> {

	private final P oldPlan;
	private final P newPlan;
	private final A agent;

	public BasicPlanSwitch(P oldPlan, P newPlan, A agent) {
		this.oldPlan = oldPlan;
		this.newPlan = newPlan;
		this.agent = agent;
	}

	public P getOldPlan() {
		return this.oldPlan;
	}

	public P getNewPlan() {
		return this.newPlan;
	}

	public A getAgent() {
		return this.agent;
	}
}
