/* *********************************************************************** *
 * project: org.matsim.*
 * ScoresAggregator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.scoring;

/**
 * Interface for classes meant to compute the joint score from the
 * individual ones.
 *
 * @author thibautd
 */
public interface ScoresAggregator {
	/**
	 * Computes the joint score based on the information passed at construction.
	 * This information should mainly consist of the individual plans, plus some
	 * parameters.
	 *
	 * @return the joint score.
	 */
	public Double getJointScore();
}

