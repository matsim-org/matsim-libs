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

import java.util.Arrays;

import se.vti.atap.minimalframework.defaults.BasicPlan;

/**
 * 
 * @author GunnarF
 *
 */
public class PathFlows extends BasicPlan {

	public final double[] pathFlows_veh;

	public PathFlows(double[] pathFlows_veh) {
		this.pathFlows_veh = pathFlows_veh;
	}

	public PathFlows(int chosenPath, int numberOfPaths) {
		this.pathFlows_veh = new double[numberOfPaths];
		this.pathFlows_veh[chosenPath] = 1.0;
	}

	public double[] computePathFlows_veh() {
		return Arrays.copyOf(this.pathFlows_veh, this.pathFlows_veh.length);
	}
	
	public PathFlows deepClone() {
		return new PathFlows(this.computePathFlows_veh());
	}

}
