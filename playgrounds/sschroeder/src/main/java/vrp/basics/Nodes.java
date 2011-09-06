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

import java.util.HashMap;
import java.util.Map;

import vrp.api.Node;


/**
 * 
 * @author stefan schroeder
 *
 */

public class Nodes {

	private Map<Integer,Node> matrixId2NodeMap = new HashMap<Integer, Node>(); 
	
	private Map<String,Node> nodes = new HashMap<String,Node>();
	
	public Map<String, Node> getNodes() {
		return nodes;
	}

	public Map<Integer, Node> getMatrixIdNodeMap() {
		return matrixId2NodeMap;
	}
}
