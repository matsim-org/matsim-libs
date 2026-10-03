/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.extension.operations.remoteoperations.RemoteGuidanceOperators;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.ActivationChange;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.CoverageChange;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.IncidentRecord;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.OperatorChange;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.OperatorRecord;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent.DeactivationReason;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.io.IOUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Writes the remote guidance analysis outputs per iteration, from the raw data accumulated by
 * {@link RemoteGuidanceAnalysisTracker}. All file names are prefixed with {@code drt_remoteGuidance_} and suffixed with
 * the mode:
 * <ul>
 *     <li>{@code _incidents} (CSV) — one row per completed incident.</li>
 *     <li>{@code _incidentStats} (CSV) — per-severity and combined summary of counts, queueing and durations.</li>
 *     <li>{@code _operatorUtilisation} (CSV and PNG) — the concurrent-busy-operator step function against the operator
 *         pool. The denominator is the on-duty operator count reconstructed from the operator started and ended events,
 *         since an incident occupies one operator regardless of its capacity;
 *         {@link RemoteGuidanceOperators#plannedOnDutyCount(double)} is plotted alongside it as the planned count.</li>
 *     <li>{@code _operatorHours} (CSV) — planned against actual operator-hours, the gap being the retention overhead.</li>
 *     <li>{@code _activeVehicles} (CSV and PNG) — the supervised-active count step function against the activation
 *         capacity {@link RemoteGuidanceOperators#activationCapacityAt(double)}.</li>
 *     <li>{@code _deactivationReasons} (CSV) — the deactivation-reason breakdown and the activation-churn totals.</li>
 *     <li>{@code _production} (CSV) — actual operator-hours against passenger-kilometres served and the service-quality
 *         metrics (served requests, mean wait, rejections, rejection rate), joining this analyzer's operator-hours with
 *         the request outcomes from {@link DrtEventSequenceCollector}.</li>
 *     <li>{@code .gpkg} layer {@code incident_hotspots}, written at shutdown — per-link incident count, mean queue delay
 *         and mean hold duration as point features at the link's to-node.</li>
 * </ul>
 * {@code _incidentStats}, {@code _operatorHours} and {@code _production} additionally append one row per iteration to a
 * cross-iteration file, so a run can be tracked over its iterations without post-processing the per-iteration files.
 *
 * @author nkuehnel / MOIA
 */
public final class RemoteGuidanceAnalysisControlerListener implements IterationEndsListener, ShutdownListener {

	private static final Logger log = LogManager.getLogger(RemoteGuidanceAnalysisControlerListener.class);

	private final DrtConfigGroup drtConfigGroup;
	private final RemoteGuidanceAnalysisTracker tracker;
	private final RemoteGuidanceOperators operators;
	private final DrtEventSequenceCollector requestCollector;
	private final Network network;
	private final MatsimServices matsimServices;

	private final String delimiter;
	private final String runId;
	private boolean incidentStatsHeaderWritten = false;
	private boolean operatorHoursHeaderWritten = false;
	private boolean productionHeaderWritten = false;

	// last iteration's incident records, retained for the shutdown-time GeoPackage.
	private List<IncidentRecord> lastIterationIncidents = List.of();

	private static final String NA = "NA";
	private static final String COMBINED = "all";
	private static final String INCIDENT_HOTSPOT_LAYER = "incident_hotspots";

	public RemoteGuidanceAnalysisControlerListener(DrtConfigGroup drtConfigGroup,
												   RemoteGuidanceAnalysisTracker tracker,
												   RemoteGuidanceOperators operators,
												   DrtEventSequenceCollector requestCollector, Network network,
												   MatsimServices matsimServices) {
		this.drtConfigGroup = drtConfigGroup;
		this.tracker = tracker;
		this.operators = operators;
		this.requestCollector = requestCollector;
		this.network = network;
		this.matsimServices = matsimServices;
		this.delimiter = matsimServices.getConfig().global().getDefaultDelimiter();
		this.runId = Optional.ofNullable(matsimServices.getConfig().controller().getRunId()).orElse(NA);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int createGraphsInterval = matsimServices.getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval > 0 && event.getIteration() % createGraphsInterval == 0;

		List<IncidentRecord> incidents = tracker.getCompletedIncidents();
		// snapshot for the shutdown-time GeoPackage: the tracker is reset before the next iteration, so we copy.
		lastIterationIncidents = new ArrayList<>(incidents);

		writeIncidentLog(incidents, filename(event, "incidents", ".csv"));
		writeIncidentStats(incidents, event.getIteration(), filename(event, "incidentStats", ".csv"));
		writeOperatorUtilisation(incidents, tracker.getOperatorChanges(),
				filename(event, "operatorUtilisation", ".csv"),
				createGraphs ? filename(event, "operatorUtilisation", ".png") : null);
		writeActiveVehicles(tracker.getActivationChanges(), tracker.getCoverageChanges(),
				filename(event, "activeVehicles", ".csv"),
				createGraphs ? filename(event, "activeVehicles", ".png") : null);
		writeDeactivationReasons(filename(event, "deactivationReasons", ".csv"));
		writeOperatorHours(tracker.getOperatorRecords(), event.getIteration(),
				filename(event, "operatorHours", ".csv"));
		writeProduction(tracker.getOperatorRecords(), event.getIteration(),
				filename(event, "production", ".csv"));
	}

	// ---------------------------------------------------------------------------------------- incident log

	private void writeIncidentLog(List<IncidentRecord> incidents, String csvFile) {
		try (BufferedWriter bw = IOUtils.getBufferedWriter(csvFile)) {
			bw.append(line("vehicle", "operatorId", "severity", "link", "startTime", "assignTime", "resolveTime",
					"expectedDuration", "actualDuration", "queued", "queueDelay"));
			for (IncidentRecord i : incidents) {
				bw.append(line(i.vehicleId(), i.operatorId(), i.severity(), i.linkId(), i.startTime(), i.assignTime(),
						i.resolveTime(), i.expectedDuration(), i.actualDuration(), i.queued(), i.queueDelay()));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// ---------------------------------------------------------------------------------------- incident summary

	private void writeIncidentStats(List<IncidentRecord> incidents, int iteration, String perIterationCsv) {
		// severity classes present this iteration, in ascending order, plus the combined aggregate.
		List<Integer> severities = incidents.stream().map(IncidentRecord::severity).distinct().sorted().toList();

		try (BufferedWriter bw = IOUtils.getBufferedWriter(perIterationCsv)) {
			bw.append(line(statsHeaderCells().toArray()));
			bw.append(line(prepend(COMBINED, summarizeIncidents(incidents))));
			for (int severity : severities) {
				List<IncidentRecord> subset = incidents.stream().filter(i -> i.severity() == severity).toList();
				bw.append(line(prepend(Integer.toString(severity), summarizeIncidents(subset))));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// cross-iteration appended file: one combined row per iteration.
		try (BufferedWriter bw = getAppendingBufferedWriter("incidentStats", ".csv")) {
			if (!incidentStatsHeaderWritten) {
				incidentStatsHeaderWritten = true;
				List<Object> header = new ArrayList<>();
				header.add("runId");
				header.add("iteration");
				header.addAll(statsHeaderCells());
				bw.write(line(header.toArray()));
			}
			bw.write(line(prepend(runId, prepend(Integer.toString(iteration),
					prepend(COMBINED, summarizeIncidents(incidents))))));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private List<Object> statsHeaderCells() {
		return new ArrayList<>(List.of("severity", "count", "queuedCount", "queuedShare", "meanQueueDelay",
				"p50QueueDelay", "p90QueueDelay", "p95QueueDelay", "maxQueueDelay", "meanActualDuration",
				"p90ActualDuration", "meanActualToExpectedRatio", "meanInterArrival"));
	}

	private Object[] summarizeIncidents(List<IncidentRecord> incidents) {
		int count = incidents.size();
		long queuedCount = incidents.stream().filter(IncidentRecord::queued).count();
		double queuedShare = count == 0 ? Double.NaN : (double) queuedCount / count;

		double[] queueDelays = incidents.stream().mapToDouble(IncidentRecord::queueDelay).sorted().toArray();
		double[] durations = incidents.stream().mapToDouble(IncidentRecord::actualDuration).sorted().toArray();
		double meanRatio = incidents.stream()
				.filter(i -> i.expectedDuration() > 0)
				.mapToDouble(i -> i.actualDuration() / i.expectedDuration())
				.average().orElse(Double.NaN);

		// mean inter-arrival over incident start times (needs ≥ 2 to define a gap).
		double[] starts = incidents.stream().mapToDouble(IncidentRecord::startTime).sorted().toArray();
		double meanInterArrival = starts.length < 2 ? Double.NaN
				: (starts[starts.length - 1] - starts[0]) / (starts.length - 1);

		return new Object[] {
				count,
				queuedCount,
				queuedShare,
				mean(queueDelays),
				percentile(queueDelays, 50),
				percentile(queueDelays, 90),
				percentile(queueDelays, 95),
				max(queueDelays),
				mean(durations),
				percentile(durations, 90),
				meanRatio,
				meanInterArrival
		};
	}

	// ---------------------------------------------------------------------------------------- operator utilisation

	private void writeOperatorUtilisation(List<IncidentRecord> incidents, List<OperatorChange> operatorChanges,
										  String csvFile, String pngFile) {
		// one merged {time -> (busyDelta, onDutyDelta)} timeline from BOTH streams: operator-busy intervals
		// [assignTime, resolveTime] and the real on-duty operator started/ended changes. Deltas at the same timestamp
		// are summed so each emitted point is a settled state (no spurious intermediate values / vertical spikes).
		TreeMap<Double, int[]> byTime = new TreeMap<>(); // time -> {busyDelta, onDutyDelta}
		byTime.put(0.0, new int[2]); // anchor at t=0 (nothing busy / on duty before the first operator starts).
		for (IncidentRecord i : incidents) {
			if (!Double.isNaN(i.assignTime())) {
				byTime.computeIfAbsent(i.assignTime(), t -> new int[2])[0] += 1;
				byTime.computeIfAbsent(i.resolveTime(), t -> new int[2])[0] -= 1;
			}
		}
		for (OperatorChange c : operatorChanges) {
			byTime.computeIfAbsent(c.time(), t -> new int[2])[1] += c.delta();
		}

		XYSeries busySeries = new XYSeries("busy operators", false, true);
		XYSeries onDutySeries = new XYSeries("on-duty operators", false, true);
		XYSeries plannedSeries = new XYSeries("planned on-duty operators", false, true);

		try (BufferedWriter bw = IOUtils.getBufferedWriter(csvFile)) {
			bw.append(line("time", "busyOperators", "onDutyOperators", "plannedOnDutyOperators", "utilisation"));
			int busy = 0;
			int onDuty = 0;
			for (Map.Entry<Double, int[]> e : byTime.entrySet()) {
				double time = e.getKey();
				busy += e.getValue()[0];
				onDuty += e.getValue()[1];
				int planned = operators.plannedOnDutyCount(time); // schedule-based comparison line
				double utilisation = onDuty == 0 ? Double.NaN : (double) busy / onDuty;
				bw.append(line(time, busy, onDuty, planned, utilisation));
				busySeries.add(time / 3600.0, busy);
				onDutySeries.add(time / 3600.0, onDuty);
				plannedSeries.add(time / 3600.0, planned);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (pngFile != null) {
			writeStepChart(pngFile, "Remote guidance operator utilisation", "# operators", busySeries, onDutySeries,
					plannedSeries);
		}
	}

	// ---------------------------------------------------------------------------------------- active vehicles

	private void writeActiveVehicles(List<ActivationChange> changes, List<CoverageChange> coverageChanges,
									 String csvFile, String pngFile) {
		// merge the active-vehicle deltas and the κ-weighted coverage-capacity deltas onto one timeline; sum deltas at
		// the same timestamp so each emitted point is a settled state (no spurious vertical spikes / multiple y per x).
		TreeMap<Double, int[]> byTime = new TreeMap<>(); // time -> {activeDelta, coverageDelta}
		byTime.put(0.0, new int[2]); // anchor all series at t=0 with value 0 (nothing active / on duty before the first
		// operator starts), so the step lines begin at 0 and step up at the first operator start rather than appearing
		// to start mid-air at the first event's value.
		for (ActivationChange c : changes) {
			byTime.computeIfAbsent(c.time(), t -> new int[2])[0] += c.delta();
		}
		for (CoverageChange c : coverageChanges) {
			byTime.computeIfAbsent(c.time(), t -> new int[2])[1] += c.delta();
		}

		XYSeries activeSeries = new XYSeries("active vehicles", false, true);
		XYSeries coverageSeries = new XYSeries("supervision capacity", false, true);
		XYSeries ceilingSeries = new XYSeries("activation ceiling (planned)", false, true);

		try (BufferedWriter bw = IOUtils.getBufferedWriter(csvFile)) {
			// coverageCapacity = effective supervision capacity (operators on duty incl. retained past planned end) —
			// this is the bound the active fleet must always respect. activationCapacity = the planned-window ceiling
			// for activating NEW vehicles (drops at an operator's planned end, so it can sit below coverage during the
			// wind-down while retained operators still supervise the vehicles heading home).
			bw.append(line("time", "activeVehicles", "coverageCapacity", "activationCapacity"));
			int active = 0;
			int coverage = 0;
			for (Map.Entry<Double, int[]> e : byTime.entrySet()) {
				double time = e.getKey();
				active += e.getValue()[0];
				coverage += e.getValue()[1];
				int ceiling = operators.activationCapacityAt(time);
				bw.append(line(time, active, coverage, ceiling));
				activeSeries.add(time / 3600.0, active);
				coverageSeries.add(time / 3600.0, coverage);
				ceilingSeries.add(time / 3600.0, ceiling);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		if (pngFile != null) {
			// coverage is the prominent reference (active <= coverage always holds); the planned ceiling is the thin
			// dashed secondary line; it may dip below coverage during wind-down, which is expected rather than an error.
			writeStepChart(pngFile, "Remote guidance active vehicles", "# vehicles",
					List.of(2), // dashed series index: the planned ceiling (series 2)
					activeSeries, coverageSeries, ceilingSeries);
		}
	}

	// ---------------------------------------------------------------------------------------- deactivation reasons

	private void writeDeactivationReasons(String csvFile) {
		var counts = tracker.getDeactivationReasonCounts();
		int totalDeactivations = counts.values().stream().mapToInt(Integer::intValue).sum();
		try (BufferedWriter bw = IOUtils.getBufferedWriter(csvFile)) {
			bw.append(line("reason", "count", "share"));
			for (DeactivationReason reason : DeactivationReason.values()) {
				int count = counts.getOrDefault(reason, 0);
				double share = totalDeactivations == 0 ? Double.NaN : (double) count / totalDeactivations;
				bw.append(line(reason, count, share));
			}
			// activation-churn totals: each activation-deactivation pair is one supervision cycle.
			bw.append(line("totalActivations", tracker.getActivationCount(), NA));
			bw.append(line("totalDeactivations", totalDeactivations, NA));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// ---------------------------------------------------------------------------------------- operator hours

	private void writeOperatorHours(List<OperatorRecord> records, int iteration, String perIterationCsv) {
		double plannedHours = 0;
		double actualHours = 0;
		try (BufferedWriter bw = IOUtils.getBufferedWriter(perIterationCsv)) {
			bw.append(line("operatorId", "startTime", "plannedEndTime", "actualEndTime", "plannedHours", "actualHours",
					"retentionSeconds"));
			for (OperatorRecord r : records) {
				double planned = (r.plannedEndTime() - r.startTime()) / 3600.0;
				double actual = (r.actualEndTime() - r.startTime()) / 3600.0;
				double retention = r.actualEndTime() - r.plannedEndTime();
				plannedHours += planned;
				actualHours += actual;
				bw.append(line(r.operatorId(), r.startTime(), r.plannedEndTime(), r.actualEndTime(), planned, actual,
						retention));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// cross-iteration append: one summary row per iteration (the L_T real-vs-planned datapoint).
		try (BufferedWriter bw = getAppendingBufferedWriter("operatorHours", ".csv")) {
			if (!operatorHoursHeaderWritten) {
				operatorHoursHeaderWritten = true;
				bw.write(line("runId", "iteration", "endedOperators", "plannedHours", "actualHours",
						"retentionHours"));
			}
			bw.write(line(runId, iteration, records.size(), plannedHours, actualHours, actualHours - plannedHours));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// ---------------------------------------------------------------------------------------- production tuple

	/**
	 * The labour-economics production tuple for this iteration: L_T (actual operator-hours), Y (passenger-km served,
	 * summed unshared ride distances of performed requests), and service quality (served requests, mean wait,
	 * rejections, rejection rate). One cross-iteration row = one datapoint on the empirical production isoquant. Wait and
	 * rejection are read from the shared core-DRT {@link DrtEventSequenceCollector} (no recomputation).
	 */
	private void writeProduction(List<OperatorRecord> operatorRecords, int iteration, String perIterationCsv) {
		double operatorHours = operatorRecords.stream()
				.mapToDouble(r -> (r.actualEndTime() - r.startTime()) / 3600.0)
				.sum();

		double passengerKm = 0;
		double sumWait = 0;
		int served = 0;
		for (EventSequence seq : requestCollector.getPerformedRequestSequences().values()) {
			for (EventSequence.PersonEvents pe : seq.getPersonEvents().values()) {
				if (pe.getPickedUp().isPresent()) {
					served++;
					passengerKm += seq.getSubmitted().getUnsharedRideDistance() / 1000.0;
					sumWait += pe.getPickedUp().get().getTime() - seq.getSubmitted().getTime();
				}
			}
		}
		double meanWait = served == 0 ? Double.NaN : sumWait / served;
		int rejections = requestCollector.getRejectedRequestSequences().size();
		double rejectionRate = (served + rejections) == 0 ? Double.NaN : (double) rejections / (served + rejections);

		try (BufferedWriter bw = IOUtils.getBufferedWriter(perIterationCsv)) {
			bw.append(line("metric", "value"));
			bw.append(line("operatorHours_LT", operatorHours));
			bw.append(line("passengerKm_Y", passengerKm));
			bw.append(line("servedRequests", served));
			bw.append(line("meanWaitTime", meanWait));
			bw.append(line("rejections", rejections));
			bw.append(line("rejectionRate", rejectionRate));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		try (BufferedWriter bw = getAppendingBufferedWriter("production", ".csv")) {
			if (!productionHeaderWritten) {
				productionHeaderWritten = true;
				bw.write(line("runId", "iteration", "operatorHours_LT", "passengerKm_Y", "servedRequests",
						"meanWaitTime", "rejections", "rejectionRate"));
			}
			bw.write(line(runId, iteration, operatorHours, passengerKm, served, meanWait, rejections, rejectionRate));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// ---------------------------------------------------------------------------------------- incident hotspot map

	/**
	 * Writes the last iteration's incident hotspots as point features (one per link that saw at least one incident, at
	 * the link's to-node) into the {@code incident_hotspots} layer of a mode-specific GeoPackage. Written at shutdown so
	 * only the final iteration's spatial pattern is persisted, mirroring {@code DrtZonalWaitTimesAnalyzer}. The
	 * GeoPackage is RG-owned so later spatial RG layers can be added alongside without touching another module's output.
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String crs = matsimServices.getConfig().global().getCoordinateSystem();
		Collection<SimpleFeature> features = incidentHotspotFeatures(lastIterationIncidents, network, crs);
		if (!features.isEmpty()) {
			String fileName = matsimServices.getControllerIO()
					.getOutputFilename("drt_remoteGuidance_" + drtConfigGroup.getMode() + ".gpkg");
			GeoFileWriter.writeGeometries(features, fileName, new NameImpl(INCIDENT_HOTSPOT_LAYER));
		}
	}

	/**
	 * Builds the incident-hotspot point features: one per link with ≥1 incident, placed at the link's to-node, carrying
	 * incident count and mean queue delay / hold duration. Static and side-effect-free so it is unit-testable without the
	 * controller machinery. Returns empty if there are no incidents or the CRS is unknown.
	 */
	static Collection<SimpleFeature> incidentHotspotFeatures(List<IncidentRecord> incidents, Network network,
															 String crs) {
		// aggregate the last iteration's incidents per link.
		record Agg(int count, double sumQueueDelay, double sumDuration) {}
		Map<Id<Link>, Agg> byLink = new LinkedHashMap<>();
		for (IncidentRecord i : incidents) {
			byLink.merge(i.linkId(), new Agg(1, i.queueDelay(), i.actualDuration()),
					(a, b) -> new Agg(a.count() + b.count(), a.sumQueueDelay() + b.sumQueueDelay(),
							a.sumDuration() + b.sumDuration()));
		}
		if (byLink.isEmpty()) {
			return List.of();
		}

		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		try {
			typeBuilder.setCRS(MGC.getCRS(crs));
		} catch (IllegalArgumentException e) {
			log.warn("Coordinate reference system \"{}\" is unknown; set a crs in config global. "
					+ "Will not create the remote guidance incident hotspot GeoPackage.", crs);
			return List.of();
		}
		// the feature type name must match the GeoPackage layer name passed to GeoFileWriter.writeGeometries (the writer
		// creates the schema under the feature type name and then opens the layer by the given name), otherwise it fails
		// with "Schema 'incident_hotspots' does not exist".
		typeBuilder.setName(INCIDENT_HOTSPOT_LAYER);
		// note: GeoPackage/shp column names are truncated at 10 chars, keep them short.
		typeBuilder.add("the_geom", Point.class);
		typeBuilder.add("link", String.class);
		typeBuilder.add("count", Integer.class);
		typeBuilder.add("meanQueue", Double.class);
		typeBuilder.add("meanDur", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());

		Collection<SimpleFeature> features = new ArrayList<>();
		for (Map.Entry<Id<Link>, Agg> entry : byLink.entrySet()) {
			Link link = network.getLinks().get(entry.getKey());
			if (link == null) {
				continue; // link not in this mode's (sub)network — skip rather than guess a location
			}
			Agg agg = entry.getValue();
			Point point = MGC.coord2Point(link.getToNode().getCoord());
			Object[] attrs = new Object[] {
					point, entry.getKey().toString(), agg.count(),
					agg.sumQueueDelay() / agg.count(), agg.sumDuration() / agg.count()
			};
			features.add(builder.buildFeature(entry.getKey().toString(), attrs));
		}
		return features;
	}

	// ---------------------------------------------------------------------------------------- helpers

	private void writeStepChart(String pngFile, String title, String rangeLabel, XYSeries... series) {
		writeStepChart(pngFile, title, rangeLabel, List.of(), series);
	}

	private void writeStepChart(String pngFile, String title, String rangeLabel, List<Integer> dashedSeriesIndices,
								XYSeries... series) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		for (XYSeries s : series) {
			dataset.addSeries(s);
		}
		JFreeChart chart = ChartFactory.createXYStepChart(title, "time [h]", rangeLabel, dataset,
				PlotOrientation.VERTICAL, true, false, false);
		XYPlot plot = chart.getXYPlot();
		// use a plain numeric domain axis: the default axis renders the (small) hour values in a time-of-day-like
		// "01:00:00.004" format (cf. ShiftHistogramChart, which sets a NumberAxis for the same reason).
		plot.setDomainAxis(new NumberAxis("time [h]"));
		float[] dash = {6.0f, 4.0f};
		for (int i = 0; i < series.length; i++) {
			BasicStroke stroke = dashedSeriesIndices.contains(i)
					? new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f)
					: new BasicStroke(2.0f);
			plot.getRenderer().setSeriesStroke(i, stroke);
		}
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.gray);
		plot.setDomainGridlinePaint(Color.gray);
		try {
			ChartUtils.saveChartAsPNG(new File(pngFile), chart, 1024, 768);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static double mean(double[] sorted) {
		if (sorted.length == 0) {
			return Double.NaN;
		}
		return Arrays.stream(sorted).average().orElse(Double.NaN);
	}

	private static double max(double[] sorted) {
		return sorted.length == 0 ? Double.NaN : sorted[sorted.length - 1];
	}

	/** Nearest-rank percentile over an already-sorted array; {@code p} in (0, 100]. */
	private static double percentile(double[] sorted, double p) {
		if (sorted.length == 0) {
			return Double.NaN;
		}
		int rank = (int) Math.ceil(p / 100.0 * sorted.length);
		return sorted[Math.max(0, Math.min(sorted.length - 1, rank - 1))];
	}

	private Object[] prepend(Object head, Object[] tail) {
		Object[] out = new Object[tail.length + 1];
		out[0] = head;
		System.arraycopy(tail, 0, out, 1, tail.length);
		return out;
	}

	private String filename(IterationEndsEvent event, String prefix, String extension) {
		return matsimServices.getControllerIO().getIterationFilename(event.getIteration(),
				"drt_remoteGuidance_" + prefix + "_" + drtConfigGroup.getMode() + extension);
	}

	private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
		return IOUtils.getAppendingBufferedWriter(matsimServices.getControllerIO()
				.getOutputFilename("drt_remoteGuidance_" + prefix + "_" + drtConfigGroup.getMode() + extension));
	}

	private String line(Object... cells) {
		return Arrays.stream(cells).map(String::valueOf).collect(Collectors.joining(delimiter, "", "\n"));
	}
}
