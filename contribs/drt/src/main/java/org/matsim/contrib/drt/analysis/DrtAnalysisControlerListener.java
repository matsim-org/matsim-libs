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

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
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
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts;
import org.matsim.contrib.common.util.ChartSaveUtils;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.analysis.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
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

	DrtAnalysisControlerListener(Config config, DrtConfigGroup drtCfg, FleetSpecification fleet, DrtVehicleDistanceStats drtVehicleStats,
			MatsimServices matsimServices, Network network, DrtEventSequenceCollector drtEventSequenceCollector,
			VehicleOccupancyProfileCalculator vehicleOccupancyProfileCalculator) {
		this.drtVehicleStats = drtVehicleStats;
		this.matsimServices = matsimServices;
		this.network = network;
		this.drtEventSequenceCollector = drtEventSequenceCollector;
		this.vehicleOccupancyProfileCalculator = vehicleOccupancyProfileCalculator;
		this.drtCfg = drtCfg;
		this.qSimCfg = config.qsim();
		runId = Optional.ofNullable(config.controller().getRunId()).orElse(notAvailableString);
		maxcap = findMaxVehicleCapacity(fleet);

		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		this.delimiter = config.global().getDefaultDelimiter();
	}

	private record DrtLeg(Id<Request> request, double submissionTime, double departureTime, Id<Person> person, Id<DvrpVehicle> vehicle, Id<Link> fromLinkId, Coord fromCoord,
						  Id<Link> toLinkId, Coord toCoord, double waitTime, double unsharedDistanceEstimate_m, double unsharedTimeEstimate_m,
						  double arrivalTime, double fare, double earliestDepartureTime, double latestDepartureTime, double latestArrivalTime) {
	}

	private static List<DrtLeg> newDrtLegs(EventSequence sequence, Function<Id<Link>, ? extends Link> linkProvider) {
		Preconditions.checkArgument(sequence.isCompleted());
		List<DrtLeg> legs = new ArrayList<>();
		DrtRequestSubmittedEvent submittedEvent = sequence.getSubmitted();

		Map<Id<Person>, EventSequence.PersonEvents> personEvents = sequence.getPersonEvents();

		var request = submittedEvent.getRequestId();
		var submissionTime = submittedEvent.getTime();
		var fromLinkId = submittedEvent.getFromLinkId();
		var fromCoord = linkProvider.apply(fromLinkId).getToNode().getCoord();
		var toLinkId = submittedEvent.getToLinkId();
		var toCoord = linkProvider.apply(toLinkId).getToNode().getCoord();
		var unsharedDistanceEstimate_m = submittedEvent.getUnsharedRideDistance();
		var unsharedTimeEstimate_m = submittedEvent.getUnsharedRideTime();
		// PersonMoneyEvent has negative amount because the agent's money is reduced -> for the operator that is a positive amount
		var fare = sequence.getDrtFares().stream().mapToDouble(PersonMoneyEvent::getAmount).sum();
		var earliestDepartureTime = sequence.getSubmitted().getEarliestDepartureTime();
		var latestDepartureTime = sequence.getSubmitted().getLatestPickupTime();
		var latestArrivalTime = sequence.getSubmitted().getLatestDropoffTime();

		for (Id<Person> person : submittedEvent.getPersonIds()) {
			var departureTime = personEvents.get(person).getDeparture().get().getTime();
			PassengerPickedUpEvent pickedUp = personEvents.get(person).getPickedUp().get();
			var vehicle = pickedUp.getVehicleId();
			var waitTime = pickedUp.getTime() -  personEvents.get(person).getDeparture().get().getTime();
			var arrivalTime = personEvents.get(person).getDroppedOff().get().getTime();
			legs.add(new DrtLeg(request, submissionTime, departureTime, person, vehicle, fromLinkId, fromCoord, toLinkId, toCoord, waitTime, unsharedDistanceEstimate_m,
					unsharedTimeEstimate_m, arrivalTime, fare, earliestDepartureTime, latestDepartureTime, latestArrivalTime));
		}
		return legs;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && event.getIteration() % createGraphsInterval == 0;

		writeAndPlotWaitTimeEstimateComparison(drtEventSequenceCollector.getPerformedRequestSequences().values(),
				filename(event, "waitTimeComparison", ".png"), filename(event, "waitTimeComparison", ".csv"), createGraphs);

		List<DrtLeg> legs = drtEventSequenceCollector.getPerformedRequestSequences()
				.values()
				.stream()
				.filter(EventSequence::isCompleted)
				.map(sequence -> newDrtLegs(sequence, network.getLinks()::get))
				.flatMap(Collection::stream)
				.sorted(Comparator.comparing(leg -> leg.departureTime))
				.collect(toList());

		List<PassengerRequestRejectedEvent> rejectionEvents = drtEventSequenceCollector.getRejectedRequestSequences()
				.values()
				.stream()
				.map(eventSequence -> eventSequence.getRejected().get())
				.sorted(Comparator.comparing(rejectionEvent -> rejectionEvent.getTime()))
				.collect(toList());

		collection2Text(drtEventSequenceCollector.getRejectedRequestSequences().values(), filename(event, "drt_rejections", ".csv"),
				String.join(delimiter, "time", "personIds", "requestId", "fromLinkId", "toLinkId", "fromX", "fromY", "toX", "toY", "cause"), seq -> {
					DrtRequestSubmittedEvent submission = seq.getSubmitted();
					Coord fromCoord = network.getLinks().get(submission.getFromLinkId()).getToNode().getCoord();
					Coord toCoord = network.getLinks().get(submission.getToLinkId()).getToNode().getCoord();
					PassengerRequestRejectedEvent rejection = seq.getRejected().get();
					return String.join(delimiter, submission.getTime() + "",//
							submission.getPersonIds().stream().map(Object::toString).collect(Collectors.joining("-")) + "",//
							submission.getRequestId() + "",//
							submission.getFromLinkId() + "",//
							submission.getToLinkId() + "",//
							fromCoord.getX() + "",//
							fromCoord.getY() + "",//
							toCoord.getX() + "",//
							toCoord.getY() + "",//
							rejection.getCause());
				});

		double rejectionRate = (double) drtEventSequenceCollector.getRejectedRequestSequences().size()
				/ drtEventSequenceCollector.getRequestSubmissions().size();
		String legsSummarize = summarizeLegs(legs, drtVehicleStats.getTravelDistances(), drtEventSequenceCollector.getDrtFarePersonMoneyEvents(),
				delimiter);
		double directDistanceMean = getDirectDistanceMean(legs);
		writeIterationPassengerStats(
				legsSummarize + delimiter + drtEventSequenceCollector.getRejectedRequestSequences().size() + delimiter + format.format(rejectionRate),
				event.getIteration());
		double l_d = getTotalDistance(drtVehicleStats.getVehicleStates()) / (legs.size() * directDistanceMean);

		MinCountAndShareIdleVehiclesOverDay minCountAndShareIdleVehiclesOverDay = getMinCountAndShareIdleVehiclesOverDay();
		String vehStats = summarizeVehicles(drtVehicleStats.getVehicleStates(), delimiter)
				+ delimiter
				+ format.format(l_d)
				+ delimiter
				+ format.format(minCountAndShareIdleVehiclesOverDay.minShareIdleVehiclesOverDay)
				+ delimiter
				+ format.format(minCountAndShareIdleVehiclesOverDay.minCountIdleVehiclesOverDay);
		String occStats = summarizeDetailedOccupancyStats(drtVehicleStats.getVehicleStates(), delimiter, maxcap);
		writeIterationVehicleStats(vehStats, occStats, event.getIteration());
		if (drtCfg.plotDetailedCustomerStats) {
			String header = String.join(delimiter, //
					"submissionTime", //
					"departureTime",//
					"personId",//
					"requestId",//
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
					"earliestDepartureTime",
					"latestDepartureTime", //
					"latestArrivalTime");

			collection2Text(legs, filename(event, "drt_legs", ".csv"), header, leg -> String.join(delimiter,//
					(Double)leg.submissionTime + "",//
					(Double)leg.departureTime + "",//
					leg.person + "",//
					leg.request + "",//
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
					format.format(leg.earliestDepartureTime), //
					format.format(leg.latestDepartureTime), //
					format.format(leg.latestArrivalTime)));
		}
		writeVehicleDistances(drtVehicleStats.getVehicleStates(), filename(event, "vehicleDistanceStats", ".csv"), delimiter);
		analyseDetours(network, legs, drtVehicleStats.getTravelDistances(), drtCfg, filename(event, "drt_detours"), createGraphs, delimiter);
		analyseWaitTimes(filename(event, "waitStats"), legs, 1800, createGraphs, delimiter);
		analyseRejections(filename(event, "drt_rejections_perTimeBin"), rejectionEvents, 1800, createGraphs, delimiter);
		analyseConstraints(filename(event, "constraints"), legs, createGraphs);

		double endTime = qSimCfg.getEndTime()
				.orElseGet(() -> legs.isEmpty() ? qSimCfg.getStartTime().orElse(0) : legs.get(legs.size() - 1).departureTime);

		analyzeBoardingsAndDeboardings(legs, delimiter, qSimCfg.getStartTime().orElse(0), endTime, 3600, filename(event, "drt_boardings", ".csv"),
				filename(event, "drt_alightments", ".csv"), network);
	}

	private static double getTotalDistance(Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances) {
		DescriptiveStatistics driven = new DescriptiveStatistics();
		for (DrtVehicleDistanceStats.VehicleState state : vehicleDistances.values()) {
			driven.addValue(state.totalDistance);
		}
		return driven.getSum();
	}

	private record MinCountAndShareIdleVehiclesOverDay(double minCountIdleVehiclesOverDay, double minShareIdleVehiclesOverDay) {
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

		return new MinCountAndShareIdleVehiclesOverDay(minCountIdleVehiclesOverDay < Double.MAX_VALUE ? minCountIdleVehiclesOverDay : Double.NaN,
				minShareIdleVehiclesOverDay < Double.MAX_VALUE ? minShareIdleVehiclesOverDay : Double.NaN);
	}

	private String filename(IterationEndsEvent event, String prefix) {
		return filename(event, prefix, "");
	}

	private String filename(IterationEndsEvent event, String prefix, String extension) {
		return filename(event.getIteration(), prefix, extension);
	}

	private String filename(int iteration, String prefix, String extension) {
		return matsimServices.getControlerIO().getIterationFilename(iteration, prefix + "_" + drtCfg.getMode() + extension);
	}

	private String outputFilename(String prefix, String extension) {
		return matsimServices.getControlerIO().getOutputFilenameWithOutputPrefix(prefix + "_" + drtCfg.getMode() + extension);
	}

	/**
	 * @param summarizeLegs
	 * @param it            iteration
	 */
	private void writeIterationPassengerStats(String summarizeLegs, int it) {
		try (var bw = getAppendingBufferedWriter("drt_customer_stats", ".csv")) {
			if (!headerWritten) {
				headerWritten = true;
				bw.write(line("runId", "iteration", "rides", "wait_average", "wait_max", "wait_p95", "wait_p75", "wait_median",
						"percentage_WT_below_10", "percentage_WT_below_15", "inVehicleTravelTime_mean", "distance_m_mean", "directDistance_m_mean",
						"totalTravelTime_mean", "fareAllReferences_mean", "rejections", "rejectionRate"));
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
				bw.write(line("runId", "iteration", "vehicles", "totalServiceDuration", "totalDistance", "totalEmptyDistance", "emptyRatio", "totalPassengerDistanceTraveled",
						"averageDrivenDistance", "averageEmptyDistance", "averagePassengerDistanceTraveled", "d_p/d_t", "l_det",
						"minShareIdleVehicles", "minCountIdleVehicles"));
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

	private void writeAndPlotWaitTimeEstimateComparison(Collection<EventSequence> performedRequestEventSequences, String plotFileName,
			String textFileName, boolean createChart) {
		try (var bw = IOUtils.getBufferedWriter(textFileName)) {
			XYSeries times = new XYSeries("waittimes", false, true);

			bw.append(line("RequestId", "actualWaitTime", "estimatedWaitTime", "deviate"));
			for (EventSequence seq : performedRequestEventSequences) {
				List<Id<Person>> personIds = seq.getSubmitted().getPersonIds();
				for (Id<Person> person : personIds) {
					if(seq.getPersonEvents().containsKey(person)) {
						EventSequence.PersonEvents personEvents = seq.getPersonEvents().get(person);
						if(personEvents.getPickedUp().isPresent() && personEvents.getDeparture().isPresent()) {
							double actualWaitTime = personEvents.getPickedUp().get().getTime() - personEvents.getDeparture().get().getTime();
							double estimatedWaitTime = seq.getScheduled().get().getPickupTime() - seq.getSubmitted().getEarliestDepartureTime();
							bw.append(line(seq.getSubmitted().getRequestId(), actualWaitTime, estimatedWaitTime, actualWaitTime - estimatedWaitTime));
							times.add(actualWaitTime, estimatedWaitTime);
						}
					}
				}
			}

			if (createChart) {
				final JFreeChart chart2 = DensityScatterPlots.createPlot("Wait times", "Actual wait time [s]", "Initially planned wait time [s]",
						times, Pair.of(0., drtCfg.maxWaitTime));
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
		return IOUtils.getAppendingBufferedWriter(matsimServices.getControlerIO().getOutputFilename(prefix + "_" + drtCfg.getMode() + extension));
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// copy analysis output from last iteration to output directory
		dumpOutput(event.getIteration(), "waitTimeComparison", ".png");
		dumpOutput(event.getIteration(), "waitTimeComparison", ".csv");
		dumpOutput(event.getIteration(), "drt_rejections", ".csv");
		dumpOutput(event.getIteration(), "drt_rejections_perTimeBin", ".csv");
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
		dumpOutput(event.getIteration(), "occupancy_time_profiles", ".txt");
		dumpOutput(event.getIteration(), "occupancy_time_profiles" + "_" + TimeProfileCharts.ChartType.Line, ".png");
		dumpOutput(event.getIteration(), "occupancy_time_profiles" + "_" + TimeProfileCharts.ChartType.StackedArea, ".png");
		dumpOutput(event.getIteration(), "task_time_profiles", ".txt");
		dumpOutput(event.getIteration(), "task_time_profiles" + "_" + TimeProfileCharts.ChartType.Line, ".png");
		dumpOutput(event.getIteration(), "task_time_profiles" + "_" + TimeProfileCharts.ChartType.StackedArea, ".png");
	}

	private void dumpOutput(int iteration, String prefix, String extension) {
		try {
			IOUtils.copyFile(filename(iteration, prefix, extension), outputFilename(prefix, extension));
		} catch (Exception ee) {
			LogManager.getLogger(this.getClass())
					.error("writing output "
							+ outputFilename(prefix, extension)
							+ " did not work; probably parameters were such that no such output was generated in the final iteration");
		}
	}

	private static Map<Double, List<DrtLeg>> splitLegsIntoBins(Collection<DrtLeg> legs, int startTime, int endTime, int binSize_s) {
		LinkedList<DrtLeg> allLegs = new LinkedList<>(legs);
		DrtLeg currentLeg = allLegs.pollFirst();
		if (currentLeg.departureTime > endTime) {
			LogManager.getLogger(DrtAnalysisControlerListener.class).error("wrong end / start Times for analysis");
		}
		Map<Double, List<DrtLeg>> splitLegs = new TreeMap<>();
		for (int time = startTime; time < endTime; time = time + binSize_s) {
			List<DrtLeg> currentList = new ArrayList<>();
			splitLegs.put((double)time, currentList);
			while (currentLeg.departureTime < time + binSize_s) {
				currentList.add(currentLeg);
				currentLeg = allLegs.pollFirst();
				if (currentLeg == null) {
					return splitLegs;
				}
			}
		}

		return splitLegs;
	}

	private static Map<Double, List<PassengerRequestRejectedEvent>> splitEventsIntoBins(List<PassengerRequestRejectedEvent> rejectionEvents, int binSize_s) {
		Map<Double, List<PassengerRequestRejectedEvent>> rejections = new TreeMap<>();

		int startTime = ((int)(rejectionEvents.get(0).getTime() / binSize_s)) * binSize_s;
		int endTime = ((int)(rejectionEvents.get(rejectionEvents.size() - 1).getTime() / binSize_s) + 1) * binSize_s;

		for (int time = startTime; time < endTime; time = time + binSize_s) {

			// rejection list in this timebin
			List<PassengerRequestRejectedEvent> rejectionList = new ArrayList<>();

			//Iterate through each rejection
			for (PassengerRequestRejectedEvent rejectedEvent : rejectionEvents){
				double rejectionTime = rejectedEvent.getTime();
				if (rejectionTime > endTime || rejectionTime < startTime) {
					LogManager.getLogger(DrtAnalysisControlerListener.class).error("wrong end / start Times for analysis");
				}

				if (rejectionTime > time && rejectionTime < time + binSize_s) {
					rejectionList.add(rejectedEvent);
				}
			}
			rejections.put((double)time, rejectionList);
		}
		return rejections;
	}

	private static void analyzeBoardingsAndDeboardings(List<DrtLeg> legs, String delimiter, double startTime, double endTime, double timeBinSize,
			String boardingsFile, String deboardingsFile, Network network) {
		if (endTime < startTime) {
			throw new IllegalArgumentException("endTime < startTime");
		}
		Map<Id<Link>, int[]> boardings = new HashMap<>();
		Map<Id<Link>, int[]> deboardings = new HashMap<>();
		int bins = (int)Math.ceil((endTime - startTime) / timeBinSize);

		for (DrtLeg leg : legs) {
			int[] board = boardings.getOrDefault(leg.fromLinkId, new int[bins]);
			int startTimeBin = (int)((leg.departureTime + leg.waitTime - startTime) / timeBinSize);
			if (startTimeBin < bins) {
				board[startTimeBin]++;
				boardings.put(leg.fromLinkId, board);
			}
			int[] deboard = deboardings.getOrDefault(leg.toLinkId, new int[bins]);
			int arrivalTimeBin = (int)((leg.arrivalTime - startTime) / timeBinSize);
			if (arrivalTimeBin < bins) {
				deboard[arrivalTimeBin]++;
				deboardings.put(leg.toLinkId, deboard);
			}
		}
		writeBoardings(boardingsFile, network, boardings, startTime, timeBinSize, bins, delimiter);
		writeBoardings(deboardingsFile, network, deboardings, startTime, timeBinSize, bins, delimiter);
	}

	private static void writeBoardings(String filename, Network network, Map<Id<Link>, int[]> boardings, double startTime, double timeBinSize,
			int bins, String delimiter) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("Link" + delimiter + "x" + delimiter + "y");
			for (int i = 0; i < bins; i++) {
				bw.write(delimiter + Time.writeTime(startTime + i * timeBinSize));
			}
			for (Map.Entry<Id<Link>, int[]> e : boardings.entrySet()) {
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

	private static String summarizeLegs(List<DrtLeg> legs, Map<Id<Request>, Double> travelDistances, List<PersonMoneyEvent> drtFarePersonMoneyEvents,
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

		for (DrtLeg leg : legs) {
			if (leg.toLinkId == null) {
				continue;
			}
			waitStats.addValue(leg.waitTime);
			rideStats.addValue(leg.arrivalTime - leg.departureTime - leg.waitTime);
			distanceStats.addValue(travelDistances.get(leg.request));
			directDistanceStats.addValue(leg.unsharedDistanceEstimate_m);
			traveltimes.addValue(leg.arrivalTime - leg.departureTime);
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
				format.format(traveltimes.getMean()) + "",//
				// all fares referencing this drt operator. Including daily fares independent from the legs.
				// PersonMoneyEvent has negative amount because the agent's money is reduced -> for the operator that is a positive amount
				format.format(-drtFarePersonMoneyEvents.stream().mapToDouble(PersonMoneyEvent::getAmount).sum() / (waitStats.getValues().length == 0 ?
						1 :
						waitStats.getValues().length)));
	}

	private static double getDirectDistanceMean(List<DrtLeg> legs) {

		DescriptiveStatistics directDistanceStats = new DescriptiveStatistics();

		for (DrtLeg leg : legs) {
			if (leg.toLinkId == null) {
				continue;
			}

			directDistanceStats.addValue(leg.unsharedDistanceEstimate_m);
		}
		return directDistanceStats.getMean();
	}

	private static void analyseDetours(Network network, List<DrtLeg> legs, Map<Id<Request>, Double> travelDistances, DrtConfigGroup drtCfg,
			String fileName, boolean createGraphs, String delimiter) {
		if (legs == null)
			return;

		List<String> detours = new ArrayList<>();
		XYSeries distances = new XYSeries("distances", false, true);
		XYSeries travelTimes = new XYSeries("travel times", false, true);
		XYSeries rideTimes = new XYSeries("ride times", false, true);

		for (DrtLeg leg : legs) {
			if (leg.toLinkId == null) {
				continue; // unfinished leg (simulation stopped before arrival)
			}

			double travelDistance = travelDistances.get(leg.request);
			double travelTime = leg.arrivalTime - leg.departureTime;
			if (createGraphs) {
				distances.add(travelDistance, leg.unsharedDistanceEstimate_m);
				travelTimes.add(travelTime, leg.unsharedTimeEstimate_m);
				rideTimes.add(leg.arrivalTime - leg.departureTime - leg.waitTime, leg.unsharedTimeEstimate_m);
			}

			double distanceDetour = travelDistance / leg.unsharedDistanceEstimate_m;
			double timeDetour = travelTime / leg.unsharedTimeEstimate_m;
			detours.add(String.join(delimiter, leg.person + "",//
					travelDistance + "",//
					leg.unsharedDistanceEstimate_m + "",//
					distanceDetour + "",//
					travelTime + "",//
					leg.unsharedTimeEstimate_m + "",//
					timeDetour + ""));
		}

		collection2Text(detours, fileName + ".csv",
				String.join(delimiter, "person", "distance", "unsharedDistance", "distanceDetour", "time", "unsharedTime", "timeDetour"),
				String::toString);

		if (createGraphs) {
			final JFreeChart chart = DensityScatterPlots.createPlot("Travelled Distances", "travelled distance [m]", "unshared ride distance [m]",
					distances);
			ChartSaveUtils.saveAsPNG(chart, fileName + "_distancePlot", 1500, 1500);

			final JFreeChart chart2 = DensityScatterPlots.createPlot("Travel Times", "travel time [s]", "unshared ride time [s]", travelTimes,
					Pair.of(drtCfg.maxTravelTimeAlpha, drtCfg.maxTravelTimeBeta));
			ChartSaveUtils.saveAsPNG(chart2, fileName + "_travelTimePlot", 1500, 1500);

			final JFreeChart chart3 = DensityScatterPlots.createPlot("Ride Times", "ride time [s]", "unshared ride time [s]", rideTimes,
					Pair.of(drtCfg.maxTravelTimeAlpha, drtCfg.maxTravelTimeBeta));
			ChartSaveUtils.saveAsPNG(chart3, fileName + "_rideTimePlot", 1500, 1500);
		}
	}

	private static void analyseWaitTimes(String fileName, List<DrtLeg> legs, int binsize_s, boolean createGraphs, String delimiter) {
		if (legs.size() == 0)
			return;
		int startTime = ((int)(legs.get(0).departureTime / binsize_s)) * binsize_s;
		int endTime = ((int)(legs.get(legs.size() - 1).departureTime / binsize_s) + binsize_s) * binsize_s;
		Map<Double, List<DrtLeg>> splitLegs = splitLegsIntoBins(legs, startTime, endTime, binsize_s);

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
			bw.write(String.join(delimiter, "timebin", "legs", "average_wait", "min", "p_5", "p_25", "median", "p_75", "p_95", "max"));
			for (Map.Entry<Double, List<DrtLeg>> e : splitLegs.entrySet()) {
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
					for (DrtLeg t : e.getValue()) {
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
				bw.write(String.join(delimiter, Time.writeTime(e.getKey()) + "",//
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
				JFreeChart chart = chartProfile(splitLegs.size(), dataset, "Waiting times", "Wait time (s)");
				JFreeChart chart2 = chartProfile(splitLegs.size(), datasetrequ, "Ride requests per hour", "Requests per hour (req/h)");
				ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
				ChartSaveUtils.saveAsPNG(chart2, fileName + "_requests", 1500, 1000);
			}

		} catch (IOException | ParseException e) {

			e.printStackTrace();
		}

	}

	private static void analyseRejections(String fileName, List<PassengerRequestRejectedEvent> rejectionEvents, int binsize_s, boolean createGraphs, String delimiter) {
		if (rejectionEvents.size() == 0)
			return;

		Map<Double, List<PassengerRequestRejectedEvent>> splitEvents = splitEventsIntoBins(rejectionEvents, binsize_s);

		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

		BufferedWriter bw = IOUtils.getBufferedWriter(fileName + ".csv");
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		TimeSeries rejections = new TimeSeries("number of rejections");

		try {
			bw.write(String.join(delimiter, "timebin", "rejections"));

			for(Map.Entry<Double, List<PassengerRequestRejectedEvent>> e : splitEvents.entrySet()){
				int drt_numOfRejection = 0;
				if (!e.getValue().isEmpty()) {
					drt_numOfRejection = e.getValue().size();
				}

				Minute h = new Minute(sdf2.parse(Time.writeTime(e.getKey())));

				rejections.addOrUpdate(h, Double.valueOf(drt_numOfRejection));
				bw.newLine();
				bw.write(String.join(delimiter, Time.writeTime(e.getKey()) + "",//
					format.format(drt_numOfRejection) +""));
			}

			bw.flush();
			bw.close();
			if (createGraphs) {
				dataset.addSeries(rejections);
				JFreeChart chart = chartProfile(splitEvents.size(), dataset, "Number of rejections", "Number");
				ChartSaveUtils.saveAsPNG(chart, fileName, 1500, 1000);
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

	private static <T> void collection2Text(Collection<T> c, String filename, String header, Function<T, String> toStringFunction) {
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
	private static void writeVehicleDistances(Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances, String iterationFilename,
			String delimiter) {
		String header = String.join(delimiter, "vehicleId", "drivenDistance_m", "occupiedDistance_m", "emptyDistance_m",
				"passengerDistanceTraveled_pm");
		BufferedWriter bw = IOUtils.getBufferedWriter(iterationFilename);
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
		try {
			bw.write(header);
			bw.newLine();
			for (Map.Entry<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> e : vehicleDistances.entrySet()) {
				var vehicleId = e.getKey();
				var vehicleState = e.getValue();
				bw.write(vehicleId + delimiter//
						+ format.format(vehicleState.totalDistance) + delimiter//
						+ format.format(vehicleState.totalOccupiedDistance) + delimiter//
						+ format.format(vehicleState.totalDistanceByOccupancy[0]) + delimiter//
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
	private static String summarizeVehicles(Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances, String del) {
		DecimalFormat format = new DecimalFormat();
		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		DescriptiveStatistics totalServiceDuration = new DescriptiveStatistics();
		DescriptiveStatistics driven = new DescriptiveStatistics();
		DescriptiveStatistics passengerTraveledDistance = new DescriptiveStatistics();
		DescriptiveStatistics occupied = new DescriptiveStatistics();
		DescriptiveStatistics empty = new DescriptiveStatistics();


		for (DrtVehicleDistanceStats.VehicleState state : vehicleDistances.values()) {
			totalServiceDuration.addValue(state.serviceDuration);
			driven.addValue(state.totalDistance);
			passengerTraveledDistance.addValue(state.totalPassengerTraveledDistance);
			occupied.addValue(state.totalOccupiedDistance);
			empty.addValue(state.totalDistanceByOccupancy[0]);
		}
		double d_p_d_t = passengerTraveledDistance.getSum() / driven.getSum();
		return String.join(del, vehicleDistances.size() + "",//
				format.format(totalServiceDuration.getSum()) + "",//
				format.format(driven.getSum()) + "",//
				format.format(empty.getSum()) + "",//
				format.format(empty.getSum() / driven.getSum()) + "",//
				format.format(passengerTraveledDistance.getSum()) + "",//
				format.format(driven.getMean()) + "",//
				format.format(empty.getMean()) + "",//
				format.format(passengerTraveledDistance.getMean()) + "",//
				format.format(d_p_d_t) + "");
	}

	/**
	 * @param fleet
	 * @return
	 */
	static int findMaxVehicleCapacity(FleetSpecification fleet) {
		return fleet.getVehicleSpecifications().values().stream().mapToInt(DvrpVehicleSpecification::getCapacity).max().getAsInt();
	}

	private static String summarizeDetailedOccupancyStats(Map<Id<Vehicle>, DrtVehicleDistanceStats.VehicleState> vehicleDistances, String del,
			int maxcap) {
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
			result.append(del).append(format.format(sum[i]));
		}

		return result.toString();
	}

	private static double getPercentageWaitTimeBelow(int timeCriteria, DescriptiveStatistics stats) {
		double[] waitingTimes = stats.getValues();

		if (waitingTimes.length == 0) {
			return Double.NaN; // to be consistent with DescriptiveStatistics
		}

		double count = (double)Arrays.stream(waitingTimes).filter(t -> t < timeCriteria).count();
		return count * 100 / waitingTimes.length;
	}

	private static void analyseConstraints(String fileName, List<DrtLeg> legs, boolean createGraphs) {
		if (legs == null)
			return;

		if (!createGraphs)
			return;

		XYSeries waitingTimes = new XYSeries("max_wait_times", false, true);
		XYSeries travelTimes = new XYSeries("max_travel_times", false, true);

		for (DrtLeg leg : legs) {
			double waitingTime = leg.waitTime;
			double maximumWaitingTime = leg.latestDepartureTime - leg.departureTime;
			waitingTimes.add(maximumWaitingTime, waitingTime);

			if (leg.toLinkId != null) {
				double travelTime = leg.arrivalTime - leg.departureTime;
				double maximumTravelTime = leg.latestArrivalTime - leg.departureTime;
				travelTimes.add(maximumTravelTime, travelTime);
			}
		}

		final JFreeChart chart = DensityScatterPlots.createPlot("Maximum wait time", "Maximum wait time [s]", "Actual wait time [s]", waitingTimes);
		ChartSaveUtils.saveAsPNG(chart, fileName + "_waiting_time", 1500, 1500);

		final JFreeChart chart2 = DensityScatterPlots.createPlot("Maximum travel time", "Maximum travel time [s]", "Actual travel time [s]",
				travelTimes);
		ChartSaveUtils.saveAsPNG(chart2, fileName + "_travel_time", 1500, 1500);
	}
}
