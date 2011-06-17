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
package playground.droeder.data.graph.comparison;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

import playground.droeder.GeoCalculator;
import playground.droeder.data.graph.MatchingEdge;
import playground.droeder.data.graph.MatchingNode;

/**
 * @author droeder
 *
 */
public class NodeCompare extends AbstractCompare{
	private Double dist, phi;

	public NodeCompare(MatchingNode ref, MatchingNode compare) {
		super(ref, compare);
		this.dist = GeoCalculator.distanceBetween2Points(ref.getCoord(), compare.getCoord());
		this.phi = getMinAngle(ref, compare);
	}

	@Override
	public int compareTo(AbstractCompare o) {
		return super.compareTo(o);
	}

	private Double getMinAngle(MatchingNode ref, MatchingNode compare){
		double temp, 
				phi = Double.MAX_VALUE;
		for(MatchingEdge refEdge : ref.getOutEdges()){
			//iterate over all edges outgoing from the candidateNode
			for(MatchingEdge candEdge : compare.getOutEdges()){
				temp = GeoCalculator.angleBeetween2Straights(new Tuple<Coord, Coord>(refEdge.getFromNode().getCoord(), refEdge.getToNode().getCoord()), 
						new Tuple<Coord, Coord>(candEdge.getFromNode().getCoord(), candEdge.getToNode().getCoord()));
				if(temp < phi){
					phi = temp;
				}
			}
		}
		return phi;
	}

	/**
	 * @return the dist
	 */
	public Double getDist() {
		return dist;
	}

	/**
	 * @return the phi
	 */
	public Double getPhi() {
		return phi;
	}
}
