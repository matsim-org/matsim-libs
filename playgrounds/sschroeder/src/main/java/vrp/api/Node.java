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
package vrp.api;

import vrp.basics.Coordinate;

/**
 * 
 * @author stefan schroeder
 *
 */

public interface Node {

	public abstract Coordinate getCoord();

	public abstract void setCoord(Coordinate coord);
	
	public abstract String getId();

	public abstract void setMatrixId(int matrixId);
	
	public abstract int getMatrixId();

}
