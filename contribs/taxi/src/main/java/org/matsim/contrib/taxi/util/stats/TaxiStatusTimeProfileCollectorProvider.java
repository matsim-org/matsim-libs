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

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.passenger.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.common.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.Provider;

public class TaxiStatusTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final Fleet fleet;
	private final SubmittedTaxiRequestsCollector requestCollector;
	private final MatsimServices matsimServices;
	private final String mode;

	public TaxiStatusTimeProfileCollectorProvider(Fleet fleet, MatsimServices matsimServices,
			SubmittedTaxiRequestsCollector requestCollector, TaxiConfigGroup taxiCfg) {
		this.fleet = fleet;
		this.requestCollector = requestCollector;
		this.matsimServices = matsimServices;
		mode = taxiCfg.getMode();
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = TimeProfiles.combineProfileCalculators(
				TaxiTimeProfiles.createCurrentTaxiTaskTypeCounter(fleet),
				TaxiTimeProfiles.createRequestsWithStatusCounter(requestCollector.getRequests().values(),
						TaxiRequestStatus.UNPLANNED));

		TimeProfileCollector collector = new TimeProfileCollector(calc, 300, "taxi_status_time_profiles_" + mode,
				matsimServices);

		collector.setChartCustomizer((chart, chartType) -> {
			TimeProfileCharts.changeSeriesColors(chart, //
					new Color(91, 155, 213), // EMPTY_DRIVE
					new Color(237, 125, 49), // PICKUP
					new Color(165, 165, 165), // OCCUPIED_DRIVE
					new Color(255, 192, 0), // DROPOFF
					new Color(112, 173, 71)); // STAY
			DefaultTableXYDataset dataset = ((DefaultTableXYDataset)chart.getXYPlot().getDataset());

			if (chartType == ChartType.StackedArea) {
				// remove UNPLANNED
				dataset.removeSeries(5);
			} else {
				// move UNPLANNED to the end
				XYSeries unplannedSeries = dataset.getSeries(5);
				dataset.removeSeries(5);
				dataset.addSeries(unplannedSeries);
			}
		});

		if (matsimServices.getConfig().controler().isCreateGraphs()) {
			collector.setChartTypes(ChartType.Line, ChartType.StackedArea);
		} else {
			collector.setChartTypes();
		}
		return collector;
	}
}
