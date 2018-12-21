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
package org.matsim.contrib.drt.analysis;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.util.chart.ChartSaveUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author jbischoff
 */

public class DynModeTripsAnalyser {

	public static Map<Double, List<DynModeTrip>> splitTripsIntoBins(Collection<DynModeTrip> trips, int startTime,
			int endTime, int binSize_s) {
		LinkedList<DynModeTrip> alltrips = new LinkedList<>();
		alltrips.addAll(trips);
		Collections.sort(alltrips);
		DynModeTrip currentTrip = alltrips.pollFirst();
		if (currentTrip.getDepartureTime() > endTime) {
			Logger.getLogger(DynModeTripsAnalyser.class).error("wrong end / start Times for analysis");
		}
		Map<Double, List<DynModeTrip>> splitTrips = new TreeMap<>();
		for (int time = startTime; time < endTime; time = time + binSize_s) {
			List<DynModeTrip> currentList = new ArrayList<>();
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

	public static void analyzeBoardingsAndDeboardings(List<DynModeTrip> trips, String delimiter, double startTime,
			double endTime, double timeBinSize, String boardingsFile, String deboardingsFile, Network network) {
		Map<Id<Link>, int[]> boardings = new HashMap<>();
		Map<Id<Link>, int[]> deboardings = new HashMap<>();
		double actualstartTime = Math.max(startTime, 0.0);
		int bins = (int)((endTime - actualstartTime) / timeBinSize);

		for (DynModeTrip trip : trips) {
			int[] board = boardings.getOrDefault(trip.getFromLinkId(), new int[bins]);
			int startTimeBin = (int)((trip.getDepartureTime() - startTime) / timeBinSize);
			if (startTimeBin < bins) {
				board[startTimeBin]++;
				boardings.put(trip.getFromLinkId(), board);
			}
			int[] deboard = deboardings.getOrDefault(trip.getToLinkId(), new int[bins]);
			int arrivalTimeBin = (int)((trip.getArrivalTime() - startTime) / timeBinSize);
			if (arrivalTimeBin < bins) {
				deboard[arrivalTimeBin]++;
				deboardings.put(trip.getFromLinkId(), deboard);
			}
		}
		writeBoardings(boardingsFile, network, boardings, startTime, timeBinSize, bins, delimiter);
		writeBoardings(deboardingsFile, network, deboardings, startTime, timeBinSize, bins, delimiter);
	}

	private static void writeBoardings(String filename, Network network, Map<Id<Link>, int[]> boardings,
			double startTime, double timeBinSize, int bins, String delimiter) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("Link" + delimiter + "x" + delimiter + "y");
			for (int i = 0; i < bins; i++) {
				bw.write(delimiter + Time.writeTime(startTime + i * timeBinSize));
			}
			for (Entry<Id<Link>, int[]> e : boardings.entrySet()) {
				bw.newLine();
				Coord coord = network.getLinks().get(e.getKey()).getCoord();
				bw.write(e.getKey().toString() + delimiter + coord.getY() + delimiter + coord.getY());
				for (int i = 0; i < e.getValue().length; i++) {
					bw.write(delimiter + e.getValue()[i]);
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String summarizeTrips(List<DynModeTrip> trips, String delimiter) {
		DescriptiveStatistics waitStats = new DescriptiveStatistics();
		DescriptiveStatistics rideStats = new DescriptiveStatistics();
		DescriptiveStatistics distanceStats = new DescriptiveStatistics();
		DescriptiveStatistics directDistanceStats = new DescriptiveStatistics();

		DescriptiveStatistics traveltimes = new DescriptiveStatistics();

		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		for (DynModeTrip trip : trips) {
			if (trip.getToLinkId() == null) {
				continue;
			}
			waitStats.addValue(trip.getWaitTime());
			rideStats.addValue(trip.getInVehicleTravelTime());
			distanceStats.addValue(trip.getTravelDistance());
			directDistanceStats.addValue(trip.getUnsharedDistanceEstimate_m());
			traveltimes.addValue(trip.getInVehicleTravelTime() + trip.getWaitTime());
		}
		String value = format.format(waitStats.getValues().length)
				+ delimiter
				+ format.format(waitStats.getMean())
				+ delimiter
				+ format.format(waitStats.getMax())
				+ delimiter
				+ format.format(waitStats.getPercentile(95))
				+ delimiter
				+ format.format(waitStats.getPercentile(75))
				+ delimiter
				+ format.format(waitStats.getPercentile(50))
				+ delimiter
				+ format.format(rideStats.getMean())
				+ delimiter
				+ format.format(distanceStats.getMean())
				+ delimiter
				+ format.format(directDistanceStats.getMean())
				+ delimiter
				+ format.format(traveltimes.getMean());
		return value;
	}

	public static double getDirectDistanceMean(List<DynModeTrip> trips) {

		DescriptiveStatistics directDistanceStats = new DescriptiveStatistics();

		for (DynModeTrip trip : trips) {
			if (trip.getToLinkId() == null) {
				continue;
			}

			directDistanceStats.addValue(trip.getUnsharedDistanceEstimate_m());
		}
		return directDistanceStats.getMean();
	}

	public static void analyseDetours(Network network, List<DynModeTrip> trips, DrtConfigGroup drtCfg,
			String fileName) {
		if (trips == null)
			return;

		List<String> detours = new ArrayList<String>();
		XYSeries distances = new XYSeries("distances");
		XYSeries travelTimes = new XYSeries("travel times");
		XYSeries rideTimes = new XYSeries("ride times");

		for (DynModeTrip trip : trips) {
			if (trip.getToLinkId() == null) {
				continue; // unfinished trip (simulation stopped before arrival)
			}

			double travelTime = trip.getInVehicleTravelTime() + trip.getWaitTime();
			distances.add(trip.getTravelDistance(), trip.getUnsharedDistanceEstimate_m());
			travelTimes.add(travelTime, trip.getUnsharedTimeEstimate_m());
			rideTimes.add(trip.getInVehicleTravelTime(), trip.getUnsharedTimeEstimate_m());

			double distanceDetour = trip.getTravelDistance() / trip.getUnsharedDistanceEstimate_m();
			double timeDetour = travelTime / trip.getUnsharedTimeEstimate_m();
			detours.add(trip.getPerson()
					+ ";"
					+ trip.getTravelDistance()
					+ ";"
					+ trip.getUnsharedDistanceEstimate_m()
					+ ";"
					+ distanceDetour
					+ ";"
					+ travelTime
					+ ";"
					+ trip.getUnsharedTimeEstimate_m()
					+ ";"
					+ timeDetour);
		}

		collection2Text(detours, fileName + ".csv",
				"person;distance;unsharedDistance;distanceDetour;time;unsharedTime;timeDetour");

		final JFreeChart chart = DensityScatterPlots.createPlot("Travelled Distances", "travelled distance [m]",
				"unshared ride distance [m]", distances);
		ChartSaveUtils.saveAsPNG(chart, fileName + "_distancePlot", 1500, 1500);

		final JFreeChart chart2 = DensityScatterPlots.createPlot("Travel Times", "travel time [s]",
				"unshared ride time [s]", travelTimes,
				Pair.of(drtCfg.getMaxTravelTimeAlpha(), drtCfg.getMaxTravelTimeBeta()));
		ChartSaveUtils.saveAsPNG(chart2, fileName + "_travelTimePlot", 1500, 1500);

		final JFreeChart chart3 = DensityScatterPlots.createPlot("Ride Times", "ride time [s]",
				"unshared ride time [s]", rideTimes,
				Pair.of(drtCfg.getMaxTravelTimeAlpha(), drtCfg.getMaxTravelTimeBeta()));
		ChartSaveUtils.saveAsPNG(chart3, fileName + "_rideTimePlot", 1500, 1500);
	}

	public static void analyseWaitTimes(String fileName, List<DynModeTrip> trips, int binsize_s) {
		Collections.sort(trips);
		if (trips.size() == 0)
			return;
		int startTime = ((int)(trips.get(0).getDepartureTime() / binsize_s)) * binsize_s;
		int endTime = ((int)(trips.get(trips.size() - 1).getDepartureTime() / binsize_s) + binsize_s) * binsize_s;
		Map<Double, List<DynModeTrip>> splitTrips = splitTripsIntoBins(trips, startTime, endTime, binsize_s);

		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

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
			for (Entry<Double, List<DynModeTrip>> e : splitTrips.entrySet()) {
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
					for (DynModeTrip t : e.getValue()) {
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
				p_95Wait.addOrUpdate(h, Double.valueOf(p_95));
				requests.addOrUpdate(h, rides * 3600. / binsize_s);// normalised [req/h]
				bw.newLine();
				bw.write(Time.writeTime(e.getKey())
						+ ";"
						+ rides
						+ ";"
						+ format.format(averageWait)
						+ ";"
						+ format.format(min)
						+ ";"
						+ format.format(p_5)
						+ ";"
						+ format.format(p_25)
						+ ";"
						+ format.format(median)
						+ ";"
						+ format.format(p_75)
						+ ";"
						+ format.format(p_95)
						+ ";"
						+ format.format(max));

			}
			bw.flush();
			bw.close();
			dataset.addSeries(averageWaitC);
			dataset.addSeries(medianWait);
			dataset.addSeries(p_5Wait);
			dataset.addSeries(p_95Wait);
			datasetrequ.addSeries(requests);
			JFreeChart chart = chartProfile(splitTrips.size(), dataset, "Waiting times", "Wait time (s)");
			JFreeChart chart2 = chartProfile(splitTrips.size(), datasetrequ, "Ride requests per hour",
					"Requests per hour (req/h)");
			ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
			ChartSaveUtils.saveAsPNG(chart2, fileName + "_requests", 1500, 1000);

		} catch (IOException | ParseException e) {

			e.printStackTrace();
		}

	}

	private static JFreeChart chartProfile(int length, TimeSeriesCollection dataset, String descriptor, String yax) {
		JFreeChart chart = ChartFactory.createTimeSeriesChart(descriptor, "Time", yax, dataset);

		// ChartFactory.createXYLineChart("TimeProfile", "Time", "Wait Time
		// [s]", dataset,
		// PlotOrientation.VERTICAL, true, false, false);

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

	public static <T> void collection2Text(Collection<T> c, String filename, String header) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			if (header != null) {
				bw.write(header);
				bw.newLine();
			}
			for (Iterator<T> iterator = c.iterator(); iterator.hasNext(); ) {

				bw.write(iterator.next().toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param vehicleDistances
	 * @param iterationFilename
	 */
	public static void writeVehicleDistances(Map<Id<Vehicle>, double[]> vehicleDistances, String iterationFilename) {
		String header = "vehicleId;drivenDistance_m;occupiedDistance_m;emptyDistance_m;revenueDistance_pm";
		BufferedWriter bw = IOUtils.getBufferedWriter(iterationFilename);
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
		try {
			bw.write(header);
			for (Entry<Id<Vehicle>, double[]> e : vehicleDistances.entrySet()) {
				double drivenDistance = e.getValue()[0];
				double revenueDistance = e.getValue()[1];
				double occDistance = e.getValue()[2];
				double emptyDistance = drivenDistance - occDistance;
				bw.newLine();
				bw.write(e.getKey().toString()
						+ ";"
						+ format.format(drivenDistance)
						+ ";"
						+ format.format(occDistance)
						+ ";"
						+ format.format(emptyDistance)
						+ ";"
						+ format.format(revenueDistance));
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param vehicleDistances Map of vehicle distances
	 * @param del              Delimiter tag
	 * @return
	 */
	public static String summarizeVehicles(Map<Id<Vehicle>, double[]> vehicleDistances, String del) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		DescriptiveStatistics driven = new DescriptiveStatistics();
		DescriptiveStatistics revenue = new DescriptiveStatistics();
		DescriptiveStatistics occupied = new DescriptiveStatistics();
		DescriptiveStatistics empty = new DescriptiveStatistics();

		for (double[] dist : vehicleDistances.values()) {
			driven.addValue(dist[0]);
			revenue.addValue(dist[1]);
			occupied.addValue(dist[2]);
			double emptyD = dist[0] - dist[2];
			empty.addValue(emptyD);
		}
		double d_r_d_t = revenue.getSum() / driven.getSum();
		// bw.write("iteration;vehicles;totalDistance;totalEmptyDistance;emptyRatio;totalRevenueDistance;averageDrivenDistance;averageEmptyDistance;averageRevenueDistance");
		String result = vehicleDistances.size()
				+ del
				+ format.format(driven.getSum())
				+ del
				+ format.format(empty.getSum())
				+ del
				+ format.format(empty.getSum() / driven.getSum())
				+ del
				+ format.format(revenue.getSum())
				+ del
				+ format.format(driven.getMean())
				+ del
				+ format.format(empty.getMean())
				+ del
				+ format.format(revenue.getMean())
				+ del
				+ format.format(d_r_d_t);
		return result;
	}

	public static double getTotalDistance(Map<Id<Vehicle>, double[]> vehicleDistances) {
		DescriptiveStatistics driven = new DescriptiveStatistics();
		for (double[] dist : vehicleDistances.values()) {
			driven.addValue(dist[0]);
		}
		return driven.getSum();
	}

	/**
	 * @param fleet
	 * @return
	 */
	public static int findMaxCap(Fleet fleet) {
		return fleet.getVehicles().values().stream().mapToInt(v -> v.getCapacity()).max().getAsInt();
	}

	public static String summarizeDetailedOccupancyStats(Map<Id<Vehicle>, double[]> vehicleDistances, String del,
			int maxcap) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		double[] sum = new double[maxcap + 1];

		for (double[] dist : vehicleDistances.values()) {
			double emptyD = dist[0] - dist[2];
			sum[0] += emptyD;
			for (int i = 3; i < maxcap + 3; i++) {
				sum[i - 2] += dist[i];
			}
		}
		String result = "";
		for (int i = 0; i <= maxcap; i++) {
			result = result + ";" + format.format(sum[i]);
		}

		return result;
	}
}
