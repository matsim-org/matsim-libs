/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/**
 * 
 */
package vrp.basics;

import vrp.api.Costs;
import vrp.api.Node;


/**
 * @author stefan schroeder
 *
 */
public class CrowFlyCosts implements Costs{
	
	public int speed = 1;
	
	public double detourFactor = 1.0;
	
	@Override
	public Double getCost(Node from, Node to) {
		return getDistance(from, to);
	}

	@Override
	public Double getDistance(Node from, Node to) {
		return EuclideanDistanceCalculator.calculateDistance(from.getCoord(), to.getCoord())*detourFactor;
	}


	@Override
	public synchronized Double getTime(Node from, Node to) {
		return getDistance(from, to)/speed;
	}

}
