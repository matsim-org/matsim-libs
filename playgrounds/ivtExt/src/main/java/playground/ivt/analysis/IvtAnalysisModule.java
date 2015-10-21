/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.analysis;

import org.matsim.core.controler.AbstractModule;
import playground.ivt.analysis.activityhistogram.ActivityHistogramModule;
import playground.ivt.analysis.tripstats.TripStatisticsModule;

/**
 * @author thibautd
 */
public class IvtAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		install( new TripModeSharesModule() );
		install( new ActivityHistogramModule() );
		install( new TripStatisticsModule() );
	}

}
