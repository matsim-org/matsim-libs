/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.michalm.drt.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.contrib.taxi.util.stats.TimeProfileCharts.ChartType;
import org.matsim.contrib.util.chart.ChartSaveUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author jbischoff
 *
 */

public class DrtTripsAnalyser {

	public static Map<Double, List<DrtTrip>> splitTripsIntoBins(Collection<DrtTrip> trips, int startTime, int endTime,
			int binSize_s) {
		LinkedList<DrtTrip> alltrips = new LinkedList<>();
		alltrips.addAll(trips);
		Collections.sort(alltrips);
		DrtTrip currentTrip = alltrips.pollFirst();
		if (currentTrip.getDepartureTime() > endTime) {
			Logger.getLogger(DrtTripsAnalyser.class).error("wrong end / start Times for analysis");
		}
		Map<Double, List<DrtTrip>> splitTrips = new TreeMap<>();
		for (int time = startTime; time < endTime; time = time + binSize_s) {
			List<DrtTrip> currentList = new ArrayList<>();
			splitTrips.put(Double.valueOf(time), currentList);
			while (currentTrip.getDepartureTime() < time + binSize_s) {
				currentList.add(currentTrip);
				currentTrip = alltrips.pollFirst();
				if (currentTrip == null) {
					return splitTrips;
				}
			}

		}

		return splitTrips;

	}

	public static void analyseWaitTimes(String fileName, List<DrtTrip> trips, int binsize_s) {
		Collections.sort(trips);
		int startTime =( (int) (trips.get(0).getDepartureTime() / binsize_s))*binsize_s;
		int endTime = ((int) (trips.get(trips.size() - 1).getDepartureTime() / binsize_s) + binsize_s)*binsize_s;
		Map<Double, List<DrtTrip>> splitTrips = splitTripsIntoBins(trips, startTime, endTime, binsize_s);

		DecimalFormat format = new DecimalFormat();
	    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	    format.setMinimumIntegerDigits(1);
	    format.setMaximumFractionDigits(2);
	    format.setGroupingUsed(false);
	    
	    SimpleDateFormat sdf2 = new SimpleDateFormat("hh:mm:ss");
	    
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName + ".csv");
	    TimeSeriesCollection dataset = new TimeSeriesCollection();
	    TimeSeriesCollection datasetrequ = new TimeSeriesCollection();
		TimeSeries averageWaitC = new TimeSeries("average");
		TimeSeries medianWait = new TimeSeries("median");
		TimeSeries p_5Wait = new TimeSeries("5th percentile");
		TimeSeries p_95Wait = new TimeSeries("95th percentile");
		TimeSeries requests = new TimeSeries("Ride requests");

		try {
			bw.write("timebin;trips;average_wait;min;p_5;p_25;median;p_75;p_95;max");
			for (Entry<Double, List<DrtTrip>> e : splitTrips.entrySet()) {
				long rides = 0;
				double averageWait = 0;
				double min = 0;
				double p_5 = 0;
				double p_25 = 0;
				double median = 0;
				double p_75 = 0;
				double p_95 = 0;
				double max = 0;
				if (!e.getValue().isEmpty()) {
					DescriptiveStatistics stats = new DescriptiveStatistics();
					for (DrtTrip t : e.getValue()) {
						stats.addValue(t.getWaitTime());
					}
					rides = stats.getN();
					averageWait = stats.getMean();
					min = stats.getMin();
					p_5 = stats.getPercentile(5);
					p_25 = stats.getPercentile(25);
					median = stats.getPercentile(50);
					p_75 = stats.getPercentile(75);
					p_95 = stats.getPercentile(95);
					max = stats.getMax();
					
				}
				Minute h = new Minute(sdf2.parse(Time.writeTime(e.getKey())));
				
				medianWait.addOrUpdate(h, Double.valueOf(median));
				averageWaitC.addOrUpdate(h, Double.valueOf(averageWait));
				p_5Wait.addOrUpdate(h, Double.valueOf(p_5));
				p_95Wait.addOrUpdate(h,Double.valueOf(p_95));
				requests.addOrUpdate(h, rides);
				bw.newLine();
				bw.write(Time.writeTime(e.getKey())+";"+rides+";"+format.format(averageWait)+";"+format.format(min)+";"+format.format(p_5)+";"+format.format(p_25)+";"+format.format(median)+";"+format.format(p_75)+";"+format.format(p_95)+";"+format.format(max));
				
			}
			bw.flush();
			bw.close();
			dataset.addSeries(averageWaitC);
			dataset.addSeries(medianWait);
			dataset.addSeries(p_5Wait);
			dataset.addSeries(p_95Wait);
			datasetrequ.addSeries(requests);
			JFreeChart chart = chartProfile(splitTrips.size(), dataset,"Taxi waiting times","Wait time (s)");
			JFreeChart chart2 = chartProfile(splitTrips.size(), datasetrequ,"Ride requests","Requests");
			ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
			ChartSaveUtils.saveAsPNG(chart2, fileName+"_requests", 1500, 1000);

		} catch (IOException | ParseException e) {

			e.printStackTrace();
		}

	}
	
	private static JFreeChart chartProfile(int length, TimeSeriesCollection dataset, String descriptor, String yax) {
		   JFreeChart chart = ChartFactory.createTimeSeriesChart(descriptor,"Time",yax,dataset);
 
				
//				ChartFactory.createXYLineChart("TimeProfile", "Time", "Wait Time [s]", dataset,
//						PlotOrientation.VERTICAL, true, false, false);
	
		XYPlot plot = chart.getXYPlot();
		plot.setRangeGridlinesVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setBackgroundPaint(Color.white);

		NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
		yAxis.setAutoRange(true);

		XYItemRenderer renderer = plot.getRenderer();
		for (int s = 0; s < length; s++) {
			renderer.setSeriesStroke(s, new BasicStroke(2));
		}

		return chart;
	}
}
