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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

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
		int endTime = times.length;

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".txt"),
			matsimServices.getConfig().global().getDefaultDelimiter().charAt(0))) {
			String[] profileHeader = profiles.keySet().toArray(new String[0]);
			writer.writeNext(new CSVLineBuilder().add("time").addAll(profileHeader));
			{
				//filter time windows: start with the first time we have 1+ vehicles in operation and end at with last such time window.
				//if there is a 'lunch' break in the fleet operation, this will remain included
				for (int i = 0; i < times.length; i++){
					int timeWindowIdx = i;
					//get profileSum = total nr of vehicles operating in time window
					double profileSum = profiles.values().stream()
						.mapToDouble(profile -> profile[timeWindowIdx])
						.sum();
					if (profileSum > 0){
						startTime = i;
						break;
					}
				}
				for (int i = times.length - 1; i > 0; i--){
					int timeWindowIdx = i;
					//get profileSum = total nr of vehicles operating in time window
					double profileSum = profiles.values().stream()
						.mapToDouble(profile -> profile[timeWindowIdx])
						.sum();
					if (profileSum > 0){
						endTime = i;
						break;
					}
				}
			}
			for (int i = startTime; i < endTime; i++) {
				var line = new CSVLineBuilder().add(Time.writeTime(times[i], timeFormat)).addAll(cells(profiles, i));
				writer.writeNext(line);
			}
		}

		if (this.matsimServices.getConfig().controller().isCreateGraphs()) {
			int finalStartTime = startTime;
			int finalEndTime = endTime;
			//add reduced double array one after each other and into a LinkedHashMap because otherwise the order of profiles gets messed up later
			Map<String, double[]> filteredProfiles = new LinkedHashMap<>();
			profiles.entrySet().forEach( e -> filteredProfiles.put(e.getKey(), Arrays.copyOfRange(e.getValue(), finalStartTime, finalEndTime)));
			//create dataset only for time windows where we have 1+ vehicles in operation
			DefaultTableXYDataset xyDataset = createXYDataset(Arrays.copyOfRange(times, startTime, endTime), filteredProfiles);
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
