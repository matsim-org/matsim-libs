/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreWriterHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.nmviljoen.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import playground.nmviljoen.gridExperiments.NmvLink;
import playground.nmviljoen.gridExperiments.NmvNode;

public interface MultilayerInstanceWriterHandler {
	
	/* <instance> ... </instance> */
	public void startInstance(final BufferedWriter out) throws IOException;
	public void endInstance(final BufferedWriter out) throws IOException;
	
	/* <physicalNetwork> ... </physicalNetwork> */
	public void startPhysicalNetwork(final BufferedWriter out) throws IOException;
	public void endPhysicalNetwork(final BufferedWriter out) throws IOException;

	/* <physicalNodes> ... </physicalNodes> */
	public void startPhysicalNodes(final BufferedWriter out) throws IOException;
	public void endPhysicalNodes(final BufferedWriter out) throws IOException;
	
	/* <physicalNode ... /> */
	public void startPhysicalNode(final BufferedWriter out, NmvNode node) throws IOException;
	public void endPhysicalNode(final BufferedWriter out) throws IOException;
	
	/* <physicalEdges> ... </physicalEdges> */
	public void startPhysicalEdges(final BufferedWriter out) throws IOException;
	public void endPhysicalEdges(final BufferedWriter out) throws IOException;
	
	/* <physicalEdge ... /> */
	public void startPhysicalEdge(final BufferedWriter out, NmvLink link) throws IOException;
	public void endPhysicalEdge(final BufferedWriter out) throws IOException;
	
	
	
	/* <logicalNetwork> ... </logicalNetwork> */
	public void startLogicalNetwork(final BufferedWriter out) throws IOException;
	public void endLogicalNetwork(final BufferedWriter out) throws IOException;

	/* <logicalNodes> ... </logicalNodes> */
	public void startLogicalNodes(final BufferedWriter out) throws IOException;
	public void endLogicalNodes(final BufferedWriter out) throws IOException;
	
	/* <logicalNode ... /> */
	public void startLogicalNode(final BufferedWriter out, NmvNode node) throws IOException;
	public void endLogicalNode(final BufferedWriter out) throws IOException;
	
	/* <logicalEdges> ... </logicalEdges> */
	public void startLogicalEdges(final BufferedWriter out) throws IOException;
	public void endLogicalEdges(final BufferedWriter out) throws IOException;
	
	/* <logicalEdge ... /> */
	public void startLogicalEdge(final BufferedWriter out, NmvLink link) throws IOException;
	public void endLogicalEdge(final BufferedWriter out) throws IOException;

	
	/* <associations> ... </associations> */
	public void startAssociations(final BufferedWriter out) throws IOException;
	public void endAssociations(final BufferedWriter out) throws IOException;
	
	/* <association ... /> */
	public void startAssociation(final BufferedWriter out, String logicalId, String physicalId) throws IOException;
	public void endAssociation(final BufferedWriter out) throws IOException;
	
	
	/* <shortestPathSets> ... </shortestPathSets> */
	public void startSets(final BufferedWriter out) throws IOException;
	public void endSets(final BufferedWriter out) throws IOException;
	
	/* <set> ... </set> */
	public void startSet(final BufferedWriter out, String fromId, String toId) throws IOException;
	public void endSet(final BufferedWriter out) throws IOException;
	
	/* <path> ... </path> */
	public void startPath(final BufferedWriter out, List<String> path) throws IOException;
	public void endPath(final BufferedWriter out) throws IOException;
	
	
	public void writeSeparator(final BufferedWriter out) throws IOException;
	
}

