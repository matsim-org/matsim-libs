/* *********************************************************************** *
 * project: org.matsim.*
 * UnivariatePiStatistic.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.math;

import org.apache.commons.math.stat.descriptive.UnivariateStatistic;

/**
 * @author illenberger
 *
 */
public interface UnivariatePiStatistic extends UnivariateStatistic {

	public void setPiValues(double[] piValues);
	
}
