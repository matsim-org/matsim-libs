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
/**
 * 
 */
package vrp.basics;

import vrp.api.Node;


/**
 * @author stefan schroeder
 *
 */
public class NodeImpl implements Comparable<NodeImpl>, Node {
	
	private String id;
	
	private int matrixId;
	
	private Coordinate coord;
	
	/* (non-Javadoc)
	 * @see core.basic.Node#getCoord()
	 */
	@Override
	public Coordinate getCoord() {
		return coord;
	}

	
	@Override
	public void setCoord(Coordinate coord) {
		this.coord = coord;
	}

	public NodeImpl(String id) {
		super();
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see core.basic.Node#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getMatrixId() {
		return matrixId;
	}

	@Override
	public void setMatrixId(int matrixId) {
		this.matrixId = matrixId;
	}

	/* (non-Javadoc)
	 * @see core.basic.Node#getDemand()
	 */
	
	@Override
	public String toString(){
		return "[id="+id+"]";
	}


	@Override
	public int compareTo(NodeImpl o) {
		return 0;
	}
	
}
