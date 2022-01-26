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
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * TODO: replace hard coded ; with delimiter variable
 *
 * @author jbischoff
 */
public class DrtAnalysisControlerListener implements IterationEndsListener {

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

		double rejectionRate = (double)drtEventSequenceCollector.getRejectedRequestSequences().size()
				/ drtEventSequenceCollector.getRequestSubmissions().size();
		String legsSummarize = DrtLegsAnalyser.summarizeLegs(legs, drtVehicleStats.getTravelDistances(),
				drtEventSequenceCollector.getDrtFarePersonMoneyEvents(), ";");
		double directDistanceMean = DrtLegsAnalyser.getDirectDistanceMean(legs);
		writeIterationPassengerStats(legsSummarize
				+ ";"
				+ drtEventSequenceCollector.getRejectedRequestSequences().size()
				+ ";"
				+ format.format(rejectionRate), event.getIteration());
		double l_d = DrtLegsAnalyser.getTotalDistance(drtVehicleStats.getVehicleStates()) / (legs.size()
				* directDistanceMean);

		var stayTaskProfile = vehicleOccupancyProfileCalculator.getNonPassengerServingTaskProfiles()
				.getOrDefault(new DrtTaskType(DrtTaskBaseType.STAY), new double[0]);
		var minStayTaskVehicleCountOverDay = Arrays.stream(stayTaskProfile).min();

		String vehStats = DrtLegsAnalyser.summarizeVehicles(drtVehicleStats.getVehicleStates(), ";")
				+ ";"
				+ format.format(l_d)
				+ ";"
				+ (minStayTaskVehicleCountOverDay.isPresent() ?
				format.format(minStayTaskVehicleCountOverDay.getAsDouble()) :
				notAvailableString);
		String occStats = DrtLegsAnalyser.summarizeDetailedOccupancyStats(drtVehicleStats.getVehicleStates(), ";",
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
					"directTravelDistance_m",//
					"fareForLeg", //
					"latestDepartureTime", //
					"latestArrivalTime");

			DrtLegsAnalyser.collection2Text(legs, filename(event, "drt_legs", ".csv"), header, leg -> String.join(";",//
					(Double)leg.departureTime + "",//
					leg.person + "",//
					leg.vehicle + "",//
					leg.fromLinkId + "",//
					format.format(leg.fromCoord.getX()),//
					format.format(leg.fromCoord.getY()),//
					leg.toLink + "",//
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
				filename(event, "vehicleDistanceStats", ".csv"));
		DrtLegsAnalyser.analyseDetours(network, legs, drtVehicleStats.getTravelDistances(), drtCfg,
				filename(event, "drt_detours"), createGraphs);
		DrtLegsAnalyser.analyseWaitTimes(filename(event, "waitStats"), legs, 1800, createGraphs);
		DrtLegsAnalyser.analyseConstraints(filename(event, "constraints"), legs, createGraphs);
		
		double endTime = qSimCfg.getEndTime()
				.orElseGet(() -> legs.isEmpty() ?
						qSimCfg.getStartTime().orElse(0) :
						legs.get(legs.size() - 1).departureTime);

		DrtLegsAnalyser.analyzeBoardingsAndDeboardings(legs, ";", qSimCfg.getStartTime().orElse(0), endTime, 3600,
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
			bw.write(runId + ";" + it + ";" + summarizeLegs);
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
						"averagePassengerDistanceTraveled", "d_p/d_t", "l_det", "minShareIdleVehicles"));
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
			Collection<EventSequence> performedRequestEventSequences, String plotFileName,
			String textFileName, boolean createChart) {
		try (var bw = IOUtils.getBufferedWriter(textFileName)) {
			XYSeries times = new XYSeries("waittimes", true, true);

			bw.append(line("RequestId", "actualWaitTime", "estimatedWaitTime", "deviate"));
			for (EventSequence seq : performedRequestEventSequences) {
				if (seq.getPickedUp().isPresent()) {
					double actualWaitTime = seq.getPickedUp().get().getTime() - seq.getSubmitted().getTime();
					double estimatedWaitTime = seq.getScheduled().get().getPickupTime() - seq.getSubmitted().getTime();
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
