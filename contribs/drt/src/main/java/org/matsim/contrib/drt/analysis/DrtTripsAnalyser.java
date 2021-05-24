package org.matsim.contrib.drt.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;

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
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.util.chart.ChartSaveUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

public class DrtTripsAnalyser {

	public static Map<Double, List<DrtTrip>> splitTripsIntoBins(Collection<DrtTrip> trips, int startTime, int endTime,
			int binSize_s) {
		LinkedList<DrtTrip> alltrips = new LinkedList<>(trips);
		DrtTrip currentTrip = alltrips.pollFirst();
		if (currentTrip.departureTime > endTime) {
			Logger.getLogger(DrtTripsAnalyser.class).error("wrong end / start Times for analysis");
		}
		Map<Double, List<DrtTrip>> splitTrips = new TreeMap<>();
		for (int time = startTime; time < endTime; time = time + binSize_s) {
			List<DrtTrip> currentList = new ArrayList<>();
			splitTrips.put((double)time, currentList);
			while (currentTrip.departureTime < time + binSize_s) {
				currentList.add(currentTrip);
				currentTrip = alltrips.pollFirst();
				if (currentTrip == null) {
					return splitTrips;
				}
			}
		}

		return splitTrips;
	}

	public static void analyzeBoardingsAndDeboardings(List<DrtTrip> trips, String delimiter, double startTime,
			double endTime, double timeBinSize, String boardingsFile, String deboardingsFile, Network network) {
		if (endTime < startTime) {
			throw new IllegalArgumentException("endTime < startTime");
		}
		Map<Id<Link>, int[]> boardings = new HashMap<>();
		Map<Id<Link>, int[]> deboardings = new HashMap<>();
		int bins = (int)((endTime - startTime) / timeBinSize);

		for (DrtTrip trip : trips) {
			int[] board = boardings.getOrDefault(trip.fromLinkId, new int[bins]);
			int startTimeBin = (int)((trip.departureTime + trip.waitTime - startTime) / timeBinSize);
			if (startTimeBin < bins) {
				board[startTimeBin]++;
				boardings.put(trip.fromLinkId, board);
			}
			int[] deboard = deboardings.getOrDefault(trip.toLink, new int[bins]);
			int arrivalTimeBin = (int)((trip.arrivalTime - startTime) / timeBinSize);
			if (arrivalTimeBin < bins) {
				deboard[arrivalTimeBin]++;
				deboardings.put(trip.fromLinkId, deboard);
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
				Coord coord = network.getLinks().get(e.getKey()).getToNode().getCoord();
				bw.write(e.getKey().toString() + delimiter + coord.getX() + delimiter + coord.getY());
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

	public static String summarizeTrips(List<DrtTrip> trips, Map<Id<Request>, Double> travelDistances,
			String delimiter) {
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

		for (DrtTrip trip : trips) {
			if (trip.toLink == null) {
				continue;
			}
			waitStats.addValue(trip.waitTime);
			rideStats.addValue(trip.arrivalTime - trip.departureTime - trip.waitTime);
			distanceStats.addValue(travelDistances.get(trip.request));
			directDistanceStats.addValue(trip.unsharedDistanceEstimate_m);
			traveltimes.addValue(trip.arrivalTime - trip.departureTime);
		}

		return String.join(delimiter, format.format(waitStats.getValues().length) + "",//
				format.format(waitStats.getMean()) + "",//
				format.format(waitStats.getMax()) + "",//
				format.format(waitStats.getPercentile(95)) + "",//
				format.format(waitStats.getPercentile(75)) + "",//
				format.format(waitStats.getPercentile(50)) + "",//
				format.format(getPercentageWaitTimeBelow(600, waitStats)) + "",//
				format.format(getPercentageWaitTimeBelow(900, waitStats)) + "",//
				format.format(rideStats.getMean()) + "",//
				format.format(distanceStats.getMean()) + "",//
				format.format(directDistanceStats.getMean()) + "",//
				format.format(traveltimes.getMean()));
	}

	public static double getDirectDistanceMean(List<DrtTrip> trips) {

		DescriptiveStatistics directDistanceStats = new DescriptiveStatistics();

		for (DrtTrip trip : trips) {
			if (trip.toLink == null) {
				continue;
			}

			directDistanceStats.addValue(trip.unsharedDistanceEstimate_m);
		}
		return directDistanceStats.getMean();
	}

	public static void analyseDetours(Network network, List<DrtTrip> trips, Map<Id<Request>, Double> travelDistances,
			DrtConfigGroup drtCfg, String fileName, boolean createGraphs) {
		if (trips == null)
			return;

		List<String> detours = new ArrayList<>();
		XYSeries distances = new XYSeries("distances");
		XYSeries travelTimes = new XYSeries("travel times");
		XYSeries rideTimes = new XYSeries("ride times");

		for (DrtTrip trip : trips) {
			if (trip.toLink == null) {
				continue; // unfinished trip (simulation stopped before arrival)
			}

			double travelDistance = travelDistances.get(trip.request);
			double travelTime = trip.arrivalTime - trip.departureTime;
			distances.add(travelDistance, trip.unsharedDistanceEstimate_m);
			travelTimes.add(travelTime, trip.unsharedTimeEstimate_m);
			rideTimes.add(trip.arrivalTime - trip.departureTime - trip.waitTime, trip.unsharedTimeEstimate_m);

			double distanceDetour = travelDistance / trip.unsharedDistanceEstimate_m;
			double timeDetour = travelTime / trip.unsharedTimeEstimate_m;
			detours.add(String.join(";", trip.person + "",//
					travelDistance + "",//
					trip.unsharedDistanceEstimate_m + "",//
					distanceDetour + "",//
					travelTime + "",//
					trip.unsharedTimeEstimate_m + "",//
					timeDetour + ""));
		}

		collection2Text(detours, fileName + ".csv",
				"person;distance;unsharedDistance;distanceDetour;time;unsharedTime;timeDetour", String::toString);

		if (createGraphs) {
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
	}

	public static void analyseWaitTimes(String fileName, List<DrtTrip> trips, int binsize_s, boolean createGraphs) {
		if (trips.size() == 0)
			return;
		int startTime = ((int)(trips.get(0).departureTime / binsize_s)) * binsize_s;
		int endTime = ((int)(trips.get(trips.size() - 1).departureTime / binsize_s) + binsize_s) * binsize_s;
		Map<Double, List<DrtTrip>> splitTrips = splitTripsIntoBins(trips, startTime, endTime, binsize_s);

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
						stats.addValue(t.waitTime);
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
				bw.write(String.join(";", Time.writeTime(e.getKey()) + "",//
						rides + "",//
						format.format(averageWait) + "",//
						format.format(min) + "",//
						format.format(p_5) + "",//
						format.format(p_25) + "",//
						format.format(median) + "",//
						format.format(p_75) + "",//
						format.format(p_95) + "",//
						format.format(max) + ""));

			}
			bw.flush();
			bw.close();
			if (createGraphs) {
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
			}

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

	public static <T> void collection2Text(Collection<T> c, String filename, String header,
			Function<T, String> toStringFunction) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			if (header != null) {
				bw.write(header);
				bw.newLine();
			}
			for (T t : c) {
				bw.write(toStringFunction.apply(t));
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
	public static void writeVehicleDistances(Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances,
			String iterationFilename) {
		String header = "vehicleId;drivenDistance_m;occupiedDistance_m;emptyDistance_m;passengerDistanceTraveled_pm";
		BufferedWriter bw = IOUtils.getBufferedWriter(iterationFilename);
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
		try {
			bw.write(header);
			bw.newLine();
			for (Entry<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> e : vehicleDistances.entrySet()) {
				var vehicleId = e.getKey();
				var vehicleState = e.getValue();
				bw.write(vehicleId + ";"//
						+ format.format(vehicleState.totalDistance) + ";"//
						+ format.format(vehicleState.totalOccupiedDistance) + ";"//
						+ format.format(vehicleState.totalDistanceByOccupancy[0]) + ";"//
						+ format.format(vehicleState.totalPassengerTraveledDistance));
				bw.newLine();
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
	public static String summarizeVehicles(Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances,
			String del) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		DescriptiveStatistics driven = new DescriptiveStatistics();
		DescriptiveStatistics passengerTraveledDistance = new DescriptiveStatistics();
		DescriptiveStatistics occupied = new DescriptiveStatistics();
		DescriptiveStatistics empty = new DescriptiveStatistics();

		for (DrtVehicleDistanceStats.VehicleState state : vehicleDistances.values()) {
			driven.addValue(state.totalDistance);
			passengerTraveledDistance.addValue(state.totalPassengerTraveledDistance);
			occupied.addValue(state.totalOccupiedDistance);
			empty.addValue(state.totalDistanceByOccupancy[0]);
		}
		double d_p_d_t = passengerTraveledDistance.getSum() / driven.getSum();
		return String.join(del, vehicleDistances.size() + "",//
				format.format(driven.getSum()) + "",//
				format.format(empty.getSum()) + "",//
				format.format(empty.getSum() / driven.getSum()) + "",//
				format.format(passengerTraveledDistance.getSum()) + "",//
				format.format(driven.getMean()) + "",//
				format.format(empty.getMean()) + "",//
				format.format(passengerTraveledDistance.getMean()) + "",//
				format.format(d_p_d_t) + "");
	}

	public static double getTotalDistance(Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances) {
		DescriptiveStatistics driven = new DescriptiveStatistics();
		for (DrtVehicleDistanceStats.VehicleState state : vehicleDistances.values()) {
			driven.addValue(state.totalDistance);
		}
		return driven.getSum();
	}

	/**
	 * @param fleet
	 * @return
	 */
	public static int findMaxVehicleCapacity(FleetSpecification fleet) {
		return fleet.getVehicleSpecifications()
				.values()
				.stream()
				.mapToInt(DvrpVehicleSpecification::getCapacity)
				.max()
				.getAsInt();
	}

	public static String summarizeDetailedOccupancyStats(
			Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances, String del, int maxcap) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		double[] sum = new double[maxcap + 1];

		for (DrtVehicleDistanceStats.VehicleState state : vehicleDistances.values()) {
			for (int i = 0; i <= maxcap; i++) {
				sum[i] += state.totalDistanceByOccupancy[i];
			}
		}
		StringBuilder result = new StringBuilder();
		for (int i = 0; i <= maxcap; i++) {
			result.append(";").append(format.format(sum[i]));
		}

		return result.toString();
	}

	public static double getPercentageWaitTimeBelow(int timeCriteria, DescriptiveStatistics stats) {
		double[] waitingTimes = stats.getValues();

		if (waitingTimes.length == 0) {
			return Double.NaN; // to be consistent with DescriptiveStatistics
		}

		double count = (double)Arrays.stream(waitingTimes).filter(t -> t < timeCriteria).count();
		return count * 100 / waitingTimes.length;
	}
}
