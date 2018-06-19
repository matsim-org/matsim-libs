/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util.stats;

import java.awt.Color;
import java.awt.Paint;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.util.timeprofile.TimeProfileCharts;
import org.matsim.contrib.util.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class TaxiStatusTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final Fleet fleet;
	private final SubmittedTaxiRequestsCollector requestCollector;
	private final MatsimServices matsimServices;

	@Inject
	public TaxiStatusTimeProfileCollectorProvider(Fleet fleet, MatsimServices matsimServices,
			SubmittedTaxiRequestsCollector requestCollector) {
		this.fleet = fleet;
		this.requestCollector = requestCollector;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = TimeProfiles.combineProfileCalculators(
				TaxiTimeProfiles.createCurrentTaxiTaskTypeCounter(fleet), //
				TaxiTimeProfiles.createRequestsWithStatusCounter(requestCollector.getRequests().values(),
						TaxiRequestStatus.UNPLANNED));

		TimeProfileCollector collector = new TimeProfileCollector(calc, 300, "taxi_status_time_profiles",
				matsimServices);

		collector.setChartCustomizer((chart, chartType) -> {
			TimeProfileCharts.changeSeriesColors(chart, new Paint[] { //
					new Color(91, 155, 213), // EMPTY_DRIVE
					new Color(237, 125, 49), // PICKUP
					new Color(165, 165, 165), // OCCUPIED_DRIVE
					new Color(255, 192, 0), // DROPOFF
					new Color(112, 173, 71), // STAY
					new Color(37, 94, 145) }); // UNPLANNED (requests)
			if (chartType == ChartType.StackedArea) {
				((DefaultTableXYDataset)chart.getXYPlot().getDataset()).removeSeries(5);
			}
		});

		collector.setChartTypes(ChartType.Line, ChartType.StackedArea);
		return collector;
	}
}
