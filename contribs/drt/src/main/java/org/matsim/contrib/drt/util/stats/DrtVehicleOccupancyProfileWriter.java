/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.util.stats;

import java.util.stream.IntStream;

import javax.inject.Inject;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.contrib.util.chart.ChartSaveUtils;
import org.matsim.contrib.util.timeprofile.TimeProfileCharts;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfileWriter implements MobsimBeforeCleanupListener {
	private static final String OUTPUT_FILE = "drt_occupancy_time_profiles";

	private final Fleet fleet;
	private final MatsimServices matsimServices;
	private final DrtConfigGroup drtCfg;

	@Inject
	public DrtVehicleOccupancyProfileWriter(@Drt Fleet fleet, MatsimServices matsimServices, DrtConfigGroup drtCfg) {
		this.fleet = fleet;
		this.matsimServices = matsimServices;
		this.drtCfg = drtCfg;
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		DrtVehicleOccupancyProfileCalculator calculator = new DrtVehicleOccupancyProfileCalculator(fleet, 300);

		TimeDiscretizer timeDiscretizer = calculator.getTimeDiscretizer();
		calculator.calculate();

		String file = filename(OUTPUT_FILE);
		String timeFormat = timeDiscretizer.getTimeInterval() % 60 == 0 ? Time.TIMEFORMAT_HHMM : Time.TIMEFORMAT_HHMMSS;

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".txt"))) {
			String[] paxHeader = IntStream.rangeClosed(0, calculator.getMaxCapacity())
					.mapToObj(i -> i + " pax")
					.toArray(String[]::new);
			writer.writeNext("time", "stay", paxHeader);

			for (int i = 0; i < timeDiscretizer.getIntervalCount(); i++) {
				int time = i * timeDiscretizer.getTimeInterval();
				String idleVehicles = calculator.getIdleVehicleProfile()[i] + "";
				writer.writeNext(Time.writeTime(time, timeFormat), idleVehicles,
						getOccupancyValues(calculator.getVehicleOccupancyProfiles(), i));
			}
		}

		DefaultTableXYDataset createXYDataset = createXYDataset(calculator);
		generateImage(createXYDataset, TimeProfileCharts.ChartType.Line);
		generateImage(createXYDataset, TimeProfileCharts.ChartType.StackedArea);
	}

	private String[] getOccupancyValues(double[][] vehicleOccupancyProfiles, int idx) {
		String[] values = new String[vehicleOccupancyProfiles.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = vehicleOccupancyProfiles[i][idx] + "";
		}
		return values;
	}

	private DefaultTableXYDataset createXYDataset(DrtVehicleOccupancyProfileCalculator calculator) {
		TimeDiscretizer timeDiscretizer = calculator.getTimeDiscretizer();
		double[] idleVehicleProfile = calculator.getIdleVehicleProfile();
		double[][] vehicleOccupancyProfiles = calculator.getVehicleOccupancyProfiles();

		XYSeries[] seriesArray = new XYSeries[1 + vehicleOccupancyProfiles.length];
		seriesArray[0] = new XYSeries("stay", false, false);
		for (int s = 0; s < vehicleOccupancyProfiles.length; s++) {
			seriesArray[1 + s] = new XYSeries(s + " pax", false, false);
		}

		for (int i = 0; i < timeDiscretizer.getIntervalCount(); i++) {
			double hour = ((double)(i * timeDiscretizer.getTimeInterval())) / 3600;
			seriesArray[0].add(hour, idleVehicleProfile[i]);
			for (int s = 0; s < vehicleOccupancyProfiles.length; s++) {
				seriesArray[1 + s].add(hour, vehicleOccupancyProfiles[s][i]);
			}
		}

		DefaultTableXYDataset dataset = new DefaultTableXYDataset();
		for (int s = seriesArray.length - 1; s >= 0; s--) {
			dataset.addSeries(seriesArray[s]);
		}
		return dataset;
	}

	private void generateImage(DefaultTableXYDataset createXYDataset, TimeProfileCharts.ChartType chartType) {
		JFreeChart chart = TimeProfileCharts.chartProfile(createXYDataset, chartType);
		String imageFile = filename(OUTPUT_FILE + "_" + chartType.name());
		ChartSaveUtils.saveAsPNG(chart, imageFile, 1500, 1000);
	}

	private String filename(String prefix) {
		return matsimServices.getControlerIO()
				.getIterationFilename(matsimServices.getIterationNumber(), prefix + "_" + drtCfg.getMode());
	}
}
