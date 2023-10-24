/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

import java.awt.Paint;
import java.util.*;
import java.util.stream.Stream;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.common.util.ChartSaveUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * @author michalm (Michal Maciejewski)
 */
public class ProfileWriter implements IterationEndsListener {

	public interface ProfileView {
		// times at which profile samples were collected
		double[] times();

		// map of sampled time profiles
		ImmutableMap<String, double[]> profiles();

		// custom series paint (if not provided, the default paint is used instead)
		Map<String, Paint> seriesPaints();
	}

	private final MatsimServices matsimServices;
	private final String mode;
	private final ProfileView view;
	private final String outputFile;

	public ProfileWriter(MatsimServices matsimServices, String mode, ProfileView profileView, String fileName) {
		this.matsimServices = matsimServices;
		this.mode = mode;
		this.view = profileView;
		this.outputFile = fileName;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		var times = view.times();
		ImmutableMap<String, double[]> profiles = view.profiles();

		String file = filename(outputFile);
		String timeFormat = Time.TIMEFORMAT_HHMMSS;

		int startTime = 0;
		int endTime = 0;

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".txt"),
			matsimServices.getConfig().global().getDefaultDelimiter().charAt(0))) {
			String[] profileHeader = profiles.keySet().toArray(new String[0]);
			writer.writeNext(new CSVLineBuilder().add("time").addAll(profileHeader));
			double[] stay = profiles.get("STAY");

			//filter the time before operation and set that time as start time
			for (int i = 0; i < times.length; i++){
				if (stay != null && stay.length > 0){
					double numOfStay = stay [i];
					if (numOfStay != 0){
						startTime = i;
						break;
					}
				}
			}
			//filter the time after operation and set that time as end time
			for (int i = times.length - 1; i > 0; i--){
				if (stay != null && stay.length > 0){
					double numOfStay = stay [i];
					if (numOfStay != 0){
						endTime = i;
						break;
					}
				}
			}

			for (int i = startTime; i <= endTime; i++) {
				var line = new CSVLineBuilder().add(Time.writeTime(times[i], timeFormat)).addAll(cells(profiles, i));
				writer.writeNext(line);
			}
		}

		//choose profiles which only happened during operation time
		Map<String, double[]> profilesInOperationTime = new LinkedHashMap<>();
		Set<String> profilesKeys = profiles.keySet();
		for (String key : profilesKeys){
			List<Double> arrayList = new ArrayList<>();
			for(double value : profiles.get(key)) {
				arrayList.add(value);
			}
			List<Double> subList = arrayList.subList(startTime,endTime + 1);
			double[] newArray = new double[subList.size()];
			for (int i = 0; i < subList.size(); i++){
				newArray[i] = subList.get(i);
			}
			profilesInOperationTime.put(key,newArray);
		}

		//choose operation time
		List<Double> timesArrayList = new ArrayList<>();
		for (double value : times){
			timesArrayList.add(value);
		}
		List<Double> operationTimesArrayList = timesArrayList.subList(startTime,endTime + 1);
		double[] operationsTimes = new double[operationTimesArrayList.size()];
		for (int i = 0; i < operationTimesArrayList.size(); i++){
			operationsTimes[i] = operationTimesArrayList.get(i);
		}


		if (this.matsimServices.getConfig().controller().isCreateGraphs()) {
			DefaultTableXYDataset xyDataset = createXYDataset(operationsTimes, profilesInOperationTime);
			generateImage(xyDataset, TimeProfileCharts.ChartType.Line);
			generateImage(xyDataset, TimeProfileCharts.ChartType.StackedArea);
		}
	}

	private Stream<String> cells(Map<String, double[]> profiles, int idx) {
		return profiles.values().stream().map(profile -> profile[idx] + "");
	}

	private DefaultTableXYDataset createXYDataset(double[] times, Map<String, double[]> profiles) {
		List<XYSeries> seriesList = new ArrayList<>(profiles.size());
		profiles.forEach((name, profile) -> {
			XYSeries series = new XYSeries(name, true, false);
			for (int i = 0; i < times.length; i++) {
				series.add((double)times[i] / 3600, profile[i]);
			}
			seriesList.add(series);
		});

		DefaultTableXYDataset dataset = new DefaultTableXYDataset();
		Lists.reverse(seriesList).forEach(dataset::addSeries);
		return dataset;
	}

	private void generateImage(DefaultTableXYDataset xyDataset, TimeProfileCharts.ChartType chartType) {
		JFreeChart chart = TimeProfileCharts.chartProfile(xyDataset, chartType);
		String runID = matsimServices.getConfig().controller().getRunId();
		if (runID != null) {
			chart.setTitle(runID + " " + chart.getTitle().getText());
		}
		setSeriesPaints(chart.getXYPlot());
		String imageFile = filename(outputFile + "_" + chartType.name());
		ChartSaveUtils.saveAsPNG(chart, imageFile, 1500, 1000);
	}

	private void setSeriesPaints(XYPlot plot) {
		var dataset = plot.getDataset(0);
		var seriesPaints = view.seriesPaints();
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			var paint = seriesPaints.get((String)dataset.getSeriesKey(i));
			if (paint != null) {
				plot.getRenderer().setSeriesPaint(i, paint);
			}
		}
	}

	private String filename(String prefix) {
		return matsimServices.getControlerIO().getIterationFilename(matsimServices.getIterationNumber(), prefix + "_" + mode);
	}
}
