/*******************************************************************************
 * Copyright (C) 2011 Stefan Schršder.
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
package vrp.basics;

public class EuclideanDistanceCalculator {
	
	public static double calculateDistance(Coordinate coord1, Coordinate coord2){
		double xDiff = coord1.getX()-coord2.getX();
		double yDiff = coord1.getY()-coord2.getY();
		return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
	}

}
