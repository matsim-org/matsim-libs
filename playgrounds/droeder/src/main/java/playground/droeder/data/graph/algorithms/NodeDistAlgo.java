/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.data.graph.algorithms;

import java.util.List;

import org.jfree.util.Log;

import playground.droeder.data.graph.GraphElement;
import playground.droeder.data.graph.MatchingGraph;
import playground.droeder.data.graph.MatchingNode;


/**
 * @author droeder
 *
 */
public class NodeDistAlgo implements MatchingAlgorithm{
	
	private Class<MatchingNode> clazz;
	private Double deltaDist;
	private MatchingGraph candGraph;

	public NodeDistAlgo(Double deltaDist, MatchingGraph candGraph){
		this.clazz= MatchingNode.class;
		this.deltaDist = deltaDist;
		this.candGraph = candGraph;
	}
	

	@Override
	public List<? extends GraphElement> run(GraphElement ref, List<? extends GraphElement> candidates) {
		if(!(ref instanceof MatchingNode)){
			Log.error("wrong GraphElementType");
			return null;
		}
		return this.candGraph.getNearestNodes(((MatchingNode) ref).getCoord().getX(), ((MatchingNode) ref).getCoord().getY(), deltaDist);
	}

	@Override
	public Class<? extends GraphElement> getProcessingClass() {
		return this.clazz;
	}

}
