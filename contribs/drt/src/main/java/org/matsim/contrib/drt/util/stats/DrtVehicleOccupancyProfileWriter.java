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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.util.CSVLineBuilder;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.contrib.util.chart.ChartSaveUtils;
import org.matsim.contrib.util.timeprofile.TimeProfileCharts;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import one.util.streamex.EntryStream;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfileWriter implements IterationEndsListener {
	private static final String OUTPUT_FILE = "drt_occupancy_time_profiles";

	private final MatsimServices matsimServices;
	private final DrtConfigGroup drtCfg;
	private final DrtVehicleOccupancyProfileCalculator calculator;

	public DrtVehicleOccupancyProfileWriter(MatsimServices matsimServices, DrtConfigGroup drtCfg,
			DrtVehicleOccupancyProfileCalculator calculator) {
		this.matsimServices = matsimServices;
		this.drtCfg = drtCfg;
		this.calculator = calculator;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		TimeDiscretizer timeDiscretizer = calculator.getTimeDiscretizer();
		calculator.consolidate();

		ImmutableMap<String, double[]> profiles = Stream.concat(
				calculator.getNonOperatingVehicleProfiles().entrySet().stream(),
				EntryStream.of(calculator.getVehicleOccupancyProfiles())
						.map(e -> Pair.of(e.getKey() + " pax", e.getValue())))
				.collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

		String file = filename(OUTPUT_FILE);
		String timeFormat = timeDiscretizer.getTimeInterval() % 60 == 0 ? Time.TIMEFORMAT_HHMM : Time.TIMEFORMAT_HHMMSS;

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".txt"))) {
			String[] profileHeader = profiles.keySet().toArray(new String[0]);
			writer.writeNext(new CSVLineBuilder().add("time").addAll(profileHeader));
			timeDiscretizer.forEach((bin, time) -> writer.writeNext(
					new CSVLineBuilder().add(Time.writeTime(time, timeFormat)).addAll(cells(profiles, bin))));
		}

		if (this.matsimServices.getConfig().controler().isCreateGraphs()) {
			DefaultTableXYDataset xyDataset = createXYDataset(timeDiscretizer, profiles);
			generateImage(xyDataset, TimeProfileCharts.ChartType.Line);
			generateImage(xyDataset, TimeProfileCharts.ChartType.StackedArea);
		}
	}

	private Stream<String> cells(Map<String, double[]> profiles, int idx) {
		return profiles.values().stream().map(values -> values[idx] + "");
	}

	private DefaultTableXYDataset createXYDataset(TimeDiscretizer timeDiscretizer, Map<String, double[]> profiles) {
		List<XYSeries> seriesList = new ArrayList<>(profiles.size());
		profiles.forEach((name, profile) -> {
			XYSeries series = new XYSeries(name, true, false);
			timeDiscretizer.forEach((bin, time) -> series.add(((double)time) / 3600, profile[bin]));
			seriesList.add(series);
		});

		DefaultTableXYDataset dataset = new DefaultTableXYDataset();
		Lists.reverse(seriesList).forEach(dataset::addSeries);
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
