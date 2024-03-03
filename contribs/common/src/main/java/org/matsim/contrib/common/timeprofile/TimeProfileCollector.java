/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.common.timeprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.jfree.chart.JFreeChart;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.common.util.ChartSaveUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TimeProfileCollector implements MobsimBeforeSimStepListener, MobsimBeforeCleanupListener {
	public interface ProfileCalculator {
		ImmutableMap<String, Double> calcValues();
	}

	private final ImmutableList<String> header;
	private final ProfileCalculator calculator;

	private final List<Double> times = new ArrayList<>();
	private final List<ImmutableMap<String, Double>> timeProfile = new ArrayList<>();
	private final int interval;
	private final String outputFile;
	private final MatsimServices matsimServices;

	private BiConsumer<JFreeChart, ChartType> chartCustomizer;
	private ChartType[] chartTypes = { ChartType.Line };

	public TimeProfileCollector(ImmutableList<String> header, ProfileCalculator calculator, int interval, String outputFile,
			MatsimServices matsimServices) {
		this.header = header;
		this.calculator = calculator;
		this.interval = interval;
		this.outputFile = outputFile;
		this.matsimServices = matsimServices;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (e.getSimulationTime() % interval == 0) {
			times.add(e.getSimulationTime());
			timeProfile.add(calculator.calcValues());
		}
	}

	public void setChartCustomizer(BiConsumer<JFreeChart, ChartType> chartCustomizer) {
		this.chartCustomizer = chartCustomizer;
	}

	public void setChartTypes(ChartType... chartTypes) {
		this.chartTypes = chartTypes;
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		String file = matsimServices.getControlerIO().getIterationFilename(matsimServices.getIterationNumber(), outputFile);
		String timeFormat = interval % 60 == 0 ? Time.TIMEFORMAT_HHMM : Time.TIMEFORMAT_HHMMSS;

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".txt"))) {
			writer.writeNext(new CSVLineBuilder().add("time").addAll(header));

			for (int i = 0; i < timeProfile.size(); i++) {
				CSVLineBuilder builder = new CSVLineBuilder().add(Time.writeTime(times.get(i), timeFormat));
				for (String column : header) {
					builder.add(timeProfile.get(i).getOrDefault(column, 0.) + "");
				}
				writer.writeNext(builder);
			}
		}

		int createGraphsInterval = matsimServices.getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && matsimServices.getIterationNumber() % createGraphsInterval == 0;

		if(createGraphs){
			for (ChartType t : chartTypes) {
				generateImage(header, t);
			}
		}
	}

	private void generateImage(ImmutableList<String> extendedHeader, ChartType chartType) {
		JFreeChart chart = TimeProfileCharts.chartProfile(extendedHeader, times, timeProfile, chartType);
		if (chartCustomizer != null) {
			chartCustomizer.accept(chart, chartType);
		}

		String imageFile = matsimServices.getControlerIO()
				.getIterationFilename(matsimServices.getIterationNumber(), outputFile + "_" + chartType.name());
		ChartSaveUtils.saveAsPNG(chart, imageFile, 1500, 1000);
	}
}
