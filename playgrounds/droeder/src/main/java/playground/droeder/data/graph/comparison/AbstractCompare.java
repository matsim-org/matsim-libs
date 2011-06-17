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

import org.matsim.api.core.v01.Id;

import playground.droeder.data.graph.GraphElement;

/**
 * @author droeder
 *
 */
public abstract class AbstractCompare implements Comparable<AbstractCompare>{
	
	private Id refId;
	private Id compId;
	private Double score;

	/**
	 * calls computeScore() and stores the elementIds
	 * @param refElement
	 * @param compareElement
	 */
	public AbstractCompare(GraphElement refElement, GraphElement compareElement){
		this.refId = refElement.getId();
		this.compId = compareElement.getId();
		this.score = Double.MAX_VALUE;
	}
	
	public void setScore(Double score){
		this.score = score;
	}
	
	public Double getScore(){
		return this.score;
	}
	
	/**
	 * @return the refId
	 */
	public Id getRefId() {
		return refId;
	}

	/**
	 * @return the compId
	 */
	public Id getCompId() {
		return compId;
	}
	
	@Override
	public int compareTo(AbstractCompare c){
		return Double.compare(this.getScore(), c.getScore());
	}
}
