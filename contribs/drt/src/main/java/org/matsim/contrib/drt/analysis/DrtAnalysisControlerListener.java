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

package org.matsim.contrib.drt.analysis;

import static java.util.stream.Collectors.toList;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer.PerformedRequestEventSequence;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author jbischoff
 */
public class DrtAnalysisControlerListener implements IterationEndsListener {

	private final DrtVehicleDistanceStats drtVehicleStats;
	private final MatsimServices matsimServices;
	private final Network network;
	private final DrtRequestAnalyzer drtRequestAnalyzer;
	private final DrtConfigGroup drtCfg;
	private final QSimConfigGroup qSimCfg;
	private boolean headerWritten = false;
	private boolean vheaderWritten = false;
	private final String runId;
	private final DecimalFormat format = new DecimalFormat();
	private final int maxcap;

	public DrtAnalysisControlerListener(Config config, DrtConfigGroup drtCfg, FleetSpecification fleet,
			DrtVehicleDistanceStats drtVehicleStats, MatsimServices matsimServices, Network network,
			DrtRequestAnalyzer drtRequestAnalyzer) {
		this.drtVehicleStats = drtVehicleStats;
		this.matsimServices = matsimServices;
		this.network = network;
		this.drtRequestAnalyzer = drtRequestAnalyzer;
		this.drtCfg = drtCfg;
		this.qSimCfg = config.qsim();
		runId = Optional.ofNullable(config.controler().getRunId()).orElse("N/A");
		maxcap = DrtTripsAnalyser.findMaxVehicleCapacity(fleet);

		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		boolean createGraphs = event.getServices().getConfig().controler().isCreateGraphs();

		writeAndPlotWaitTimeEstimateComparison(drtRequestAnalyzer.getPerformedRequestSequences().values(),
				filename(event, "waitTimeComparison", ".png"), filename(event, "waitTimeComparison", ".csv"),
				createGraphs);

		List<DrtTrip> trips = drtRequestAnalyzer.getPerformedRequestSequences()
				.values()
				.stream()
				.filter(PerformedRequestEventSequence::isCompleted)
				.map(sequence -> new DrtTrip(sequence, network.getLinks()::get))
				.sorted(Comparator.comparing(trip -> trip.departureTime))
				.collect(toList());

		DrtTripsAnalyser.collection2Text(drtRequestAnalyzer.getRejectedRequestSequences().values(),
				filename(event, "drt_rejections", ".csv"),
				String.join(";", "time", "personId", "fromLinkId", "toLinkId", "fromX", "fromY", "toX", "toY"), seq -> {
					DrtRequestSubmittedEvent submission = seq.getSubmitted();
					Coord fromCoord = network.getLinks().get(submission.getFromLinkId()).getToNode().getCoord();
					Coord toCoord = network.getLinks().get(submission.getToLinkId()).getToNode().getCoord();
					return String.join(";", submission.getTime() + "",//
							submission.getPersonId() + "",//
							submission.getFromLinkId() + "",//
							submission.getToLinkId() + "",//
							fromCoord.getX() + "",//
							fromCoord.getY() + "",//
							toCoord.getX() + "",//
							toCoord.getY() + "");
				});

		double rejectionRate = (double)drtRequestAnalyzer.getRejectedRequestSequences().size()
				/ drtRequestAnalyzer.getRequestSubmissions().size();
		String tripsSummarize = DrtTripsAnalyser.summarizeTrips(trips, drtVehicleStats.getTravelDistances(), ";");
		double directDistanceMean = DrtTripsAnalyser.getDirectDistanceMean(trips);
		writeIterationPassengerStats(
				tripsSummarize + ";" + drtRequestAnalyzer.getRejectedRequestSequences().size() + ";" + format.format(
						rejectionRate), event.getIteration());
		double l_d = DrtTripsAnalyser.getTotalDistance(drtVehicleStats.getVehicleStates()) / (trips.size()
				* directDistanceMean);
		String vehStats = DrtTripsAnalyser.summarizeVehicles(drtVehicleStats.getVehicleStates(), ";")
				+ ";"
				+ format.format(l_d);
		String occStats = DrtTripsAnalyser.summarizeDetailedOccupancyStats(drtVehicleStats.getVehicleStates(), ";",
				maxcap);
		writeIterationVehicleStats(vehStats, occStats, event.getIteration());
		if (drtCfg.isPlotDetailedCustomerStats()) {
			String header = String.join(";", "departureTime",//
					"personId",//
					"vehicleId",//
					"fromLinkId",//
					"fromX",//
					"fromY",//
					"toLinkId",//
					"toX",//
					"toY",//
					"waitTime",//
					"arrivalTime",//
					"travelTime",//
					"travelDistance_m",//
					"direcTravelDistance_m");

			DrtTripsAnalyser.collection2Text(trips, filename(event, "drt_trips", ".csv"), header,
					trip -> String.join(";",//
							(Double)trip.departureTime + "",//
							trip.person + "",//
							trip.vehicle + "",//
							trip.fromLinkId + "",//
							format.format(trip.fromCoord.getX()),//
							format.format(trip.fromCoord.getY()),//
							trip.toLink + "",//
							format.format(trip.toCoord.getX()),//
							format.format(trip.toCoord.getY()),//
							trip.waitTime + "",//
							trip.arrivalTime + "",//
							(trip.arrivalTime - trip.departureTime - trip.waitTime) + "",//
							format.format(drtVehicleStats.getTravelDistances().get(trip.request)),//
							format.format(trip.unsharedDistanceEstimate_m)));
		}
		DrtTripsAnalyser.writeVehicleDistances(drtVehicleStats.getVehicleStates(),
				filename(event, "vehicleDistanceStats", ".csv"));
		DrtTripsAnalyser.analyseDetours(network, trips, drtVehicleStats.getTravelDistances(), drtCfg,
				filename(event, "drt_detours"), createGraphs);
		DrtTripsAnalyser.analyseWaitTimes(filename(event, "waitStats"), trips, 1800, createGraphs);

		double endTime = qSimCfg.getEndTime()
				.orElseGet(() -> trips.isEmpty() ?
						qSimCfg.getStartTime().orElse(0) :
						trips.get(trips.size() - 1).departureTime);

		DrtTripsAnalyser.analyzeBoardingsAndDeboardings(trips, ";", qSimCfg.getStartTime().orElse(0), endTime, 3600,
				filename(event, "drt_boardings", ".csv"), filename(event, "drt_alightments", ".csv"), network);
	}

	private String filename(IterationEndsEvent event, String prefix) {
		return filename(event, prefix, "");
	}

	private String filename(IterationEndsEvent event, String prefix, String extension) {
		return matsimServices.getControlerIO()
				.getIterationFilename(event.getIteration(), prefix + "_" + drtCfg.getMode() + extension);
	}

	/**
	 * @param summarizeTrips
	 * @param it             iteration
	 */
	private void writeIterationPassengerStats(String summarizeTrips, int it) {
		try (var bw = getAppendingBufferedWriter("drt_customer_stats", ".csv")) {
			if (!headerWritten) {
				headerWritten = true;
				bw.write(line("runId", "iteration", "rides", "wait_average", "wait_max", "wait_p95", "wait_p75",
						"wait_median", "percentage_WT_below_10", "percentage_WT_below_15", "inVehicleTravelTime_mean",
						"distance_m_mean", "directDistance_m_mean", "totalTravelTime_mean", "rejections", "rejectionRate"));
			}
			bw.write(runId + ";" + it + ";" + summarizeTrips);
			bw.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param summarizeVehicles
	 * @param it                iteration
	 */
	private void writeIterationVehicleStats(String summarizeVehicles, String vehOcc, int it) {
		try (var bw = getAppendingBufferedWriter("drt_vehicle_stats", ".csv")) {
			if (!vheaderWritten) {
				bw.write(line("runId", "iteration", "vehicles", "totalDistance", "totalEmptyDistance", "emptyRatio",
						"totalPassengerDistanceTraveled", "averageDrivenDistance", "averageEmptyDistance",
						"averagePassengerDistanceTraveled", "d_p/d_t", "l_det"));
			}
			bw.write(line(runId, it, summarizeVehicles));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (var bw = getAppendingBufferedWriter("drt_detailed_distanceStats", ".csv")) {
			if (!vheaderWritten) {
				vheaderWritten = true;
				bw.write("runId;iteration;");
				for (int i = 0; i <= maxcap; i++) {
					bw.write(";" + i + " pax distance_m");
				}
				bw.newLine();
			}
			bw.write(runId + ";" + it + vehOcc);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAndPlotWaitTimeEstimateComparison(
			Collection<PerformedRequestEventSequence> performedRequestEventSequences, String plotFileName,
			String textFileName, boolean createChart) {
		try (var bw = IOUtils.getBufferedWriter(textFileName)) {
			XYSeries times = new XYSeries("waittimes", true, true);

			bw.append(line("RequestId", "actualWaitTime", "estimatedWaitTime", "deviate"));
			for (PerformedRequestEventSequence seq : performedRequestEventSequences) {
				if (seq.getPickedUp().isPresent()) {
					double actualWaitTime = seq.getPickedUp().get().getTime() - seq.getSubmitted().getTime();
					double estimatedWaitTime = seq.getScheduled().getPickupTime() - seq.getSubmitted().getTime();
					bw.append(line(seq.getSubmitted().getRequestId(), actualWaitTime, estimatedWaitTime,
							actualWaitTime - estimatedWaitTime));
					times.add(actualWaitTime, estimatedWaitTime);
				}
			}

			if (createChart) {
				final JFreeChart chart2 = DensityScatterPlots.createPlot("Wait times", "Actual wait time [s]",
						"Initially planned wait time [s]", times, Pair.of(0., drtCfg.getMaxWaitTime()));
				//			xAxis.setLowerBound(0);
				//			yAxis.setLowerBound(0);
				ChartUtils.writeChartAsPNG(new FileOutputStream(plotFileName), chart2, 1500, 1500);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String line(Object... cells) {
		return Arrays.stream(cells).map(Object::toString).collect(Collectors.joining(";", "", "\n"));
	}

	private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
		return IOUtils.getAppendingBufferedWriter(
				matsimServices.getControlerIO().getOutputFilename(prefix + "_" + drtCfg.getMode() + extension));
	}
}
