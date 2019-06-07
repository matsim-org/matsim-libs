/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.timeprofile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jfree.chart.JFreeChart;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.contrib.util.chart.ChartSaveUtils;
import org.matsim.contrib.util.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

public class TimeProfileCollector implements MobsimBeforeSimStepListener, MobsimBeforeCleanupListener {
	public interface ProfileCalculator {
		String[] getHeader();

		Object[] calcValues();
	}

	private final ProfileCalculator calculator;
	private final List<Double> times = new ArrayList<>();
	private final List<Object[]> timeProfile = new ArrayList<>();
	private final int interval;
	private final String outputFile;
	private final MatsimServices matsimServices;

	private Function<Object[], String[]> valuesToStringsConverter = TimeProfiles::combineValuesIntoStrings;
	private BiConsumer<JFreeChart, ChartType> chartCustomizer;
	private ChartType[] chartTypes = { ChartType.Line };

	public TimeProfileCollector(ProfileCalculator calculator, int interval, String outputFile,
			MatsimServices matsimServices) {
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

	public void setValuesToStringsConverter(Function<Object[], String[]> valuesToStringsConverter) {
		this.valuesToStringsConverter = valuesToStringsConverter;
	}

	public void setChartCustomizer(BiConsumer<JFreeChart, ChartType> chartCustomizer) {
		this.chartCustomizer = chartCustomizer;
	}

	public void setChartTypes(ChartType... chartTypes) {
		this.chartTypes = chartTypes;
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		String file = matsimServices.getControlerIO().getIterationFilename(matsimServices.getIterationNumber(),
				outputFile);
		String timeFormat = interval % 60 == 0 ? Time.TIMEFORMAT_HHMM : Time.TIMEFORMAT_HHMMSS;

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".txt"))) {
			writer.writeNext("time", calculator.getHeader());
			for (int i = 0; i < timeProfile.size(); i++) {
				writer.writeNext(Time.writeTime(times.get(i), timeFormat), //
						valuesToStringsConverter.apply(timeProfile.get(i)));
			}
		}

		for (ChartType t : chartTypes) {
			generateImage(t);
		}
	}

	private void generateImage(ChartType chartType) {
		JFreeChart chart = TimeProfileCharts.chartProfile(calculator.getHeader(), times, timeProfile, chartType);
		if (chartCustomizer != null) {
			chartCustomizer.accept(chart, chartType);
		}

		String imageFile = matsimServices.getControlerIO().getIterationFilename(matsimServices.getIterationNumber(),
				outputFile + "_" + chartType.name());
		ChartSaveUtils.saveAsPNG(chart, imageFile, 1500, 1000);
	}
}
