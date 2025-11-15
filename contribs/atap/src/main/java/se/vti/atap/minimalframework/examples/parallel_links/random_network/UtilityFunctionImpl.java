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

import java.util.Arrays;

import se.vti.atap.minimalframework.UtilityFunction;
import se.vti.atap.minimalframework.examples.parallel_links.AgentImpl;
import se.vti.atap.minimalframework.examples.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.examples.parallel_links.PathFlows;

/**
 * 
 * @author GunnarF
 *
 */
public class UtilityFunctionImpl implements UtilityFunction<PathFlows, AgentImpl, NetworkConditionsImpl> {

	public UtilityFunctionImpl() {
	}

	@Override
	public double compute(PathFlows paths, AgentImpl odPair, NetworkConditionsImpl networkConditions) {
		double[] pathFlows_veh = paths.computePathFlows_veh();

		int shortestPath = odPair.computeBestPath(networkConditions);
		double shortestTravelTime_s = networkConditions.linkTravelTimes_s[odPair.availableLinks[shortestPath]];
		double shortestTravelTimeSum_s = Arrays.stream(pathFlows_veh).sum() * shortestTravelTime_s;

		double realizedTravelTimeSum_s = 0.0;
		for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
			int link = odPair.availableLinks[path];
			realizedTravelTimeSum_s += pathFlows_veh[path] * networkConditions.linkTravelTimes_s[link];
		}
		double gap_s = realizedTravelTimeSum_s - shortestTravelTimeSum_s;
		return (-1.0) * gap_s;
	}
}
