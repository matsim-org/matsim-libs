/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.stats.abtractPAnalysisModules;

import org.apache.log4j.Logger;


/**
 * Simply divides {@link CountPassengerMeterPerMode} by {@link CountCapacityMeterPerMode}.
 * 
 * @author aneumann
 *
 */
final class AverageLoadPerDistancePerMode extends AbstractPAnalyisModule {
	
	private final static Logger log = Logger.getLogger(AverageLoadPerDistancePerMode.class);
	
	private final CountPassengerMeterPerMode countPassengerMeterPerMode;
	private final CountCapacityMeterPerMode countCapacityMeterPerMode;
	
	public AverageLoadPerDistancePerMode(CountPassengerMeterPerMode countPassengerMeterPerMode,	CountCapacityMeterPerMode countCapacityMeterPerMode) {
		super(AverageLoadPerDistancePerMode.class.getSimpleName());
		this.countPassengerMeterPerMode = countPassengerMeterPerMode;
		this.countCapacityMeterPerMode = countCapacityMeterPerMode;
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			double averageLoadPerDistance = this.countPassengerMeterPerMode.getResults().get(ptMode) / this.countCapacityMeterPerMode.getResults().get(ptMode);
			strB.append(", " + averageLoadPerDistance);
		}
		return strB.toString();
	}
	
}
