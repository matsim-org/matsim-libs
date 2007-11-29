/* *********************************************************************** *
 * project: org.matsim.*
 * GraphUtils.java
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
/**
 * 
 */
package playground.yu.graphUtils;

import org.matsim.counts.CountSimComparison;

/**
 * @author ychen
 * 
 */
public class CountsBarChartUtils extends BarChartUtil {
	public CountsBarChartUtils() {
		super();
	}

	public void addData(final CountSimComparison cc) {
		String matsim_series = "Sim Volumes"; 
		String real_series = "Count Volumes";
		String h=Integer.toString(cc.getHour());
		dataset0.addValue(cc.getSimulationValue(),matsim_series, h);
		dataset0.addValue(cc.getCountValue(),real_series,h);
		 //relative error
		 dataset1.addValue(cc.calculateRelativeError(),"Signed Rel.	* Error",h); 
	}

	@Override
	public void addData(Object[] values) {
//		TODO...
	}


}
