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

import java.util.Set;

import se.vti.atap.minimalframework.defaults.BasicLoggerImpl;
import se.vti.atap.minimalframework.examples.parallel_links.AgentImpl;
import se.vti.atap.minimalframework.examples.parallel_links.NetworkConditionsImpl;

/**
 * 
 * @author GunnarF
 *
 */
public class LoggerImpl extends BasicLoggerImpl<AgentImpl, NetworkConditionsImpl> {

	public double computeGap(Set<AgentImpl> agents, NetworkConditionsImpl networkConditions, int iteration) {
		double numerator = (-1.0) * agents.stream().mapToDouble(a -> a.getCurrentPlan().getUtility()).sum();
		double denominator = 0.0;
		for (int link = 0; link < networkConditions.linkFlows_veh.length; link++) {
			denominator += networkConditions.linkFlows_veh[link] * networkConditions.linkTravelTimes_s[link];
		}
		double relativeEquilibriumGap = numerator / denominator;
		return relativeEquilibriumGap;
	}

}
