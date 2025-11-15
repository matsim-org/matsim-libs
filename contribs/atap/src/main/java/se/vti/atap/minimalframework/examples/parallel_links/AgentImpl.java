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

import java.util.Random;

import se.vti.atap.minimalframework.defaults.BasicAgent;

/**
 * 
 * @author GunnarF
 *
 */
public class AgentImpl extends BasicAgent<PathFlows> {

	public final double size_veh;

	public final int[] availableLinks;

	public AgentImpl(String id, double size_veh, int... availableLinks) {
		super(id);
		this.size_veh = size_veh;
		this.availableLinks = availableLinks;
	}

	public int getNumberOfPaths() {
		return this.availableLinks.length;
	}

	public AgentImpl(String id, int... availableLinks) {
		this(id, 1.0, availableLinks);
	}

	public int computeBestPath(NetworkConditionsImpl networkConditions) {
		int bestPath = 0;
		double shortestTT_s = Double.POSITIVE_INFINITY;
		for (int path = 0; path < this.getNumberOfPaths(); path++) {
			int link = this.availableLinks[path];
			if (networkConditions.linkTravelTimes_s[link] < shortestTT_s) {
				bestPath = path;
				shortestTT_s = networkConditions.linkTravelTimes_s[link];
			}
		}
		return bestPath;
	}
	
	public int computeRandomPath(Random rnd) {
		return rnd.nextInt(this.getNumberOfPaths());
	}
}
