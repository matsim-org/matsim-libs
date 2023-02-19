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
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

/**
 *
 * @author jbischoff
 */
public class DrtAnalysisControlerListener implements IterationEndsListener, ShutdownListener {

	private final DrtVehicleDistanceStats drtVehicleStats;
	private final MatsimServices matsimServices;
	private final Network network;
	private final DrtEventSequenceCollector drtEventSequenceCollector;
	private final VehicleOccupancyProfileCalculator vehicleOccupancyProfileCalculator;
	private final DrtConfigGroup drtCfg;
	private final QSimConfigGroup qSimCfg;
	private boolean headerWritten = false;
	private boolean vheaderWritten = false;
	private final String runId;
	private final DecimalFormat format = new DecimalFormat();
	private final int maxcap;
	private static final String notAvailableString = "NA";

	private final String delimiter;

	public DrtAnalysisControlerListener(Config config, DrtConfigGroup drtCfg, FleetSpecification fleet,
			DrtVehicleDistanceStats drtVehicleStats, MatsimServices matsimServices, Network network,
			DrtEventSequenceCollector drtEventSequenceCollector,
			VehicleOccupancyProfileCalculator vehicleOccupancyProfileCalculator) {
		this.drtVehicleStats = drtVehicleStats;
		this.matsimServices = matsimServices;
		this.network = network;
		this.drtEventSequenceCollector = drtEventSequenceCollector;
		this.vehicleOccupancyProfileCalculator = vehicleOccupancyProfileCalculator;
		this.drtCfg = drtCfg;
		this.qSimCfg = config.qsim();
		runId = Optional.ofNullable(config.controler().getRunId()).orElse(notAvailableString);
		maxcap = DrtLegsAnalyser.findMaxVehicleCapacity(fleet);

		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		this.delimiter = config.global().getDefaultDelimiter();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		boolean createGraphs = event.getServices().getConfig().controler().isCreateGraphs();

		writeAndPlotWaitTimeEstimateComparison(drtEventSequenceCollector.getPerformedRequestSequences().values(),
				filename(event, "waitTimeComparison", ".png"), filename(event, "waitTimeComparison", ".csv"),
				createGraphs);

		List<DrtLeg> legs = drtEventSequenceCollector.getPerformedRequestSequences()
				.values()
				.stream()
				.filter(EventSequence::isCompleted)
				.map(sequence -> new DrtLeg(sequence, network.getLinks()::get))
				.sorted(Comparator.comparing(leg -> leg.departureTime))
				.collect(toList());

		DrtLegsAnalyser.collection2Text(drtEventSequenceCollector.getRejectedRequestSequences().values(),
				filename(event, "drt_rejections", ".csv"),
				String.join(delimiter, "time", "personId", "fromLinkId", "toLinkId", "fromX", "fromY", "toX", "toY"), seq -> {
					DrtRequestSubmittedEvent submission = seq.getSubmitted();
					Coord fromCoord = network.getLinks().get(submission.getFromLinkId()).getToNode().getCoord();
					Coord toCoord = network.getLinks().get(submission.getToLinkId()).getToNode().getCoord();
					return String.join(delimiter, submission.getTime() + "",//
							submission.getPersonId() + "",//
							submission.getFromLinkId() + "",//
							submission.getToLinkId() + "",//
							fromCoord.getX() + "",//
							fromCoord.getY() + "",//
							toCoord.getX() + "",//
							toCoord.getY() + "");
				});

		double rejectionRate = (double)drtEventSequenceCollector.getRejectedRequestSequences().size()
				/ drtEventSequenceCollector.getRequestSubmissions().size();
		String legsSummarize = DrtLegsAnalyser.summarizeLegs(legs, drtVehicleStats.getTravelDistances(),
				drtEventSequenceCollector.getDrtFarePersonMoneyEvents(), delimiter);
		double directDistanceMean = DrtLegsAnalyser.getDirectDistanceMean(legs);
		writeIterationPassengerStats(legsSummarize
				+ delimiter
				+ drtEventSequenceCollector.getRejectedRequestSequences().size()
				+ delimiter
				+ format.format(rejectionRate), event.getIteration());
		double l_d = DrtLegsAnalyser.getTotalDistance(drtVehicleStats.getVehicleStates()) / (legs.size()
				* directDistanceMean);

		MinCountAndShareIdleVehiclesOverDay minCountAndShareIdleVehiclesOverDay = getMinCountAndShareIdleVehiclesOverDay();
		String vehStats = DrtLegsAnalyser.summarizeVehicles(drtVehicleStats.getVehicleStates(), delimiter)
				+ delimiter
				+ format.format(l_d)
				+ delimiter
				+ format.format(minCountAndShareIdleVehiclesOverDay.minShareIdleVehiclesOverDay)
				+ delimiter
				+ format.format(minCountAndShareIdleVehiclesOverDay.minCountIdleVehiclesOverDay);
		String occStats = DrtLegsAnalyser.summarizeDetailedOccupancyStats(drtVehicleStats.getVehicleStates(), delimiter,
				maxcap);
		writeIterationVehicleStats(vehStats, occStats, event.getIteration());
		if (drtCfg.plotDetailedCustomerStats) {
			String header = String.join(delimiter, "departureTime",//
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
					"inVehicleTravelTime",//
					"travelDistance_m",//
					"directTravelDistance_m",//
					"fareForLeg", //
					"latestDepartureTime", //
					"latestArrivalTime");

			DrtLegsAnalyser.collection2Text(legs, filename(event, "drt_legs", ".csv"), header, leg -> String.join(delimiter,//
					(Double)leg.departureTime + "",//
					leg.person + "",//
					leg.vehicle + "",//
					leg.fromLinkId + "",//
					format.format(leg.fromCoord.getX()),//
					format.format(leg.fromCoord.getY()),//
					leg.toLinkId + "",//
					format.format(leg.toCoord.getX()),//
					format.format(leg.toCoord.getY()),//
					leg.waitTime + "",//
					leg.arrivalTime + "",//
					(leg.arrivalTime - leg.departureTime - leg.waitTime) + "",//
					format.format(drtVehicleStats.getTravelDistances().get(leg.request)),//
					format.format(leg.unsharedDistanceEstimate_m),//
					format.format(leg.fare), //
					format.format(leg.latestDepartureTime), //
					format.format(leg.latestArrivalTime)));
		}
		DrtLegsAnalyser.writeVehicleDistances(drtVehicleStats.getVehicleStates(),
				filename(event, "vehicleDistanceStats", ".csv"), delimiter);
		DrtLegsAnalyser.analyseDetours(network, legs, drtVehicleStats.getTravelDistances(), drtCfg,
				filename(event, "drt_detours"), createGraphs, delimiter);
		DrtLegsAnalyser.analyseWaitTimes(filename(event, "waitStats"), legs, 1800, createGraphs, delimiter);
		DrtLegsAnalyser.analyseConstraints(filename(event, "constraints"), legs, createGraphs);

		double endTime = qSimCfg.getEndTime()
				.orElseGet(() -> legs.isEmpty() ?
						qSimCfg.getStartTime().orElse(0) :
						legs.get(legs.size() - 1).departureTime);

		DrtLegsAnalyser.analyzeBoardingsAndDeboardings(legs, delimiter, qSimCfg.getStartTime().orElse(0), endTime, 3600,
				filename(event, "drt_boardings", ".csv"), filename(event, "drt_alightments", ".csv"), network);
	}

	private record MinCountAndShareIdleVehiclesOverDay(double minCountIdleVehiclesOverDay,
													   double minShareIdleVehiclesOverDay) {
	}

	private MinCountAndShareIdleVehiclesOverDay getMinCountAndShareIdleVehiclesOverDay() {
		double minCountIdleVehiclesOverDay = Double.MAX_VALUE;
		double minShareIdleVehiclesOverDay = Double.MAX_VALUE;
		double[] stayTaskProfile = vehicleOccupancyProfileCalculator.getNonPassengerServingTaskProfiles().get(DrtStayTask.TYPE);
		double[] numberOfVehiclesInServiceProfile = vehicleOccupancyProfileCalculator.getNumberOfVehiclesInServiceProfile();
		if (stayTaskProfile != null) {
			for (int i = 0; i < numberOfVehiclesInServiceProfile.length; i++) {
				if (numberOfVehiclesInServiceProfile[i] > 0) {
					// only consider time intervals in which vehicles were in operation. Otherwise, any time period without a vehicle in operation will make for count 0.
					minCountIdleVehiclesOverDay = Math.min(minCountIdleVehiclesOverDay, stayTaskProfile[i]);
					minShareIdleVehiclesOverDay = Math.min(minShareIdleVehiclesOverDay, stayTaskProfile[i] / numberOfVehiclesInServiceProfile[i]);
				}
			}
		}

		return new MinCountAndShareIdleVehiclesOverDay(
				minCountIdleVehiclesOverDay < Double.MAX_VALUE ? minCountIdleVehiclesOverDay : Double.NaN,
				minShareIdleVehiclesOverDay < Double.MAX_VALUE ? minShareIdleVehiclesOverDay : Double.NaN);
	}

	private String filename(IterationEndsEvent event, String prefix) {
		return filename(event, prefix, "");
	}

	private String filename(IterationEndsEvent event, String prefix, String extension) {
		return filename(event.getIteration(), prefix, extension);
	}

	private String filename(int iteration, String prefix, String extension) {
		return matsimServices.getControlerIO()
				.getIterationFilename(iteration, prefix + "_" + drtCfg.getMode() + extension);
	}

	private String outputFilename(String prefix, String extension) {
		return matsimServices.getControlerIO()
				.getOutputFilenameWithOutputPrefix(prefix + "_" + drtCfg.getMode() + extension);
	}

	/**
	 * @param summarizeLegs
	 * @param it            iteration
	 */
	private void writeIterationPassengerStats(String summarizeLegs, int it) {
		try (var bw = getAppendingBufferedWriter("drt_customer_stats", ".csv")) {
			if (!headerWritten) {
				headerWritten = true;
				bw.write(line("runId", "iteration", "rides", "wait_average", "wait_max", "wait_p95", "wait_p75",
						"wait_median", "percentage_WT_below_10", "percentage_WT_below_15", "inVehicleTravelTime_mean",
						"distance_m_mean", "directDistance_m_mean", "totalTravelTime_mean", "fareAllReferences_mean",
						"rejections", "rejectionRate"));
			}
			bw.write(runId + delimiter + it + delimiter + summarizeLegs);
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
						"averagePassengerDistanceTraveled", "d_p/d_t", "l_det", "minShareIdleVehicles", "minCountIdleVehicles"));
			}
			bw.write(line(runId, it, summarizeVehicles));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (var bw = getAppendingBufferedWriter("drt_detailed_distanceStats", ".csv")) {
			if (!vheaderWritten) {
				vheaderWritten = true;
				bw.write("runId;iteration");
				for (int i = 0; i <= maxcap; i++) {
					bw.write(delimiter + i + " pax distance_m");
				}
				bw.newLine();
			}
			bw.write(runId + delimiter + it + vehOcc);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAndPlotWaitTimeEstimateComparison(
			Collection<EventSequence> performedRequestEventSequences, String plotFileName,
			String textFileName, boolean createChart) {
		try (var bw = IOUtils.getBufferedWriter(textFileName)) {
			XYSeries times = new XYSeries("waittimes", false, true);

			bw.append(line("RequestId", "actualWaitTime", "estimatedWaitTime", "deviate"));
			for (EventSequence seq : performedRequestEventSequences) {
				if (seq.getPickedUp().isPresent()) {
					double actualWaitTime = seq.getPickedUp().get().getTime() - seq.getDeparture().get().getTime();
					double estimatedWaitTime = seq.getScheduled().get().getPickupTime() - seq.getDeparture().get().getTime();
					bw.append(line(seq.getSubmitted().getRequestId(), actualWaitTime, estimatedWaitTime,
							actualWaitTime - estimatedWaitTime));
					times.add(actualWaitTime, estimatedWaitTime);
				}
			}

			if (createChart) {
				final JFreeChart chart2 = DensityScatterPlots.createPlot("Wait times", "Actual wait time [s]",
						"Initially planned wait time [s]", times, Pair.of(0., drtCfg.maxWaitTime));
				//			xAxis.setLowerBound(0);
				//			yAxis.setLowerBound(0);
				ChartUtils.writeChartAsPNG(new FileOutputStream(plotFileName), chart2, 1500, 1500);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String line(Object... cells) {
		return Arrays.stream(cells).map(Object::toString).collect(Collectors.joining(delimiter, "", "\n"));
	}

	private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
		return IOUtils.getAppendingBufferedWriter(
				matsimServices.getControlerIO().getOutputFilename(prefix + "_" + drtCfg.getMode() + extension));
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// copy analysis output from last iteration to output directory
		dumpOutput(event.getIteration(), "waitTimeComparison", ".png");
		dumpOutput(event.getIteration(), "waitTimeComparison", ".csv");
		dumpOutput(event.getIteration(), "drt_rejections", ".csv");
		dumpOutput(event.getIteration(), "drt_legs", ".csv");
		dumpOutput(event.getIteration(), "vehicleDistanceStats", ".csv");
		dumpOutput(event.getIteration(), "drt_detours", ".csv");
		dumpOutput(event.getIteration(), "drt_detours", "_distancePlot.png");
		dumpOutput(event.getIteration(), "drt_detours", "_travelTimePlot.png");
		dumpOutput(event.getIteration(), "drt_detours", "_rideTimePlot.png");
		dumpOutput(event.getIteration(), "waitStats", ".csv");
		dumpOutput(event.getIteration(), "waitStats", ".png");
		dumpOutput(event.getIteration(), "waitStats", "_requests.png");
		dumpOutput(event.getIteration(), "constraints", "_waiting_time.png");
		dumpOutput(event.getIteration(), "constraints", "_travel_time.png");
		dumpOutput(event.getIteration(), "drt_boardings", ".csv");
		dumpOutput(event.getIteration(), "drt_alightments", ".csv");
	}

	private void dumpOutput(int iteration, String prefix, String extension) {
		try {
			IOUtils.copyFile(filename(iteration, prefix, extension), outputFilename(prefix, extension));
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass()).error("writing output " + outputFilename(prefix, extension) +
					" did not work; probably parameters were such that no such output was generated in the final iteration");
		}
	}

}
