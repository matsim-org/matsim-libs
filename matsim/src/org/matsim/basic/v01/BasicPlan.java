/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
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
package org.matsim.basic.v01;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.ActLegIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
/**
*
* @author dgrether
*
*/

public interface BasicPlan {

	/**
	 * Constant describing the score of an unscored plan. <b>Do not use this constant in
	 * comparisons</b>, but use {@link #hasUndefinedScore()} or {@link #isUndefinedScore(double)}
	 * instead to test if a plan has an undefined score.
	 */
	public static final double UNDEF_SCORE = Double.NaN;

	public double getScore();

	public void setScore(final double score);

	/** @return true if the score of this plan is not defined */
	public boolean hasUndefinedScore();

	/**
	 * Getter for the Iterator class defined above
	 * @return A special iterator over acts and legs.
	 */
	public ActLegIterator getIterator();

	public LegIterator getIteratorLeg();

	public ActIterator getIteratorAct();

	public void addLeg(final BasicLeg leg);

	public void addAct(final BasicAct act);

}