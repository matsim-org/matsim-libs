/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */

package org.matsim.contrib.drt.extension.operations.shifts.analysis.efficiency;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public final class ShiftEfficiencyAnalysisControlerListener implements IterationEndsListener {
    private final Provider<DrtShiftsSpecification> drtShiftsSpecification;
    private final MatsimServices matsimServices;

    private final DrtConfigGroup drtConfigGroup;
    private final ShiftEfficiencyTracker shiftEfficiencyTracker;

    private final String delimiter;
    private final String runId;
    private boolean headerWritten = false;
    private static final String notAvailableString = "NA";
    // label used in the per-type breakdown for the combined aggregate over all shift types
    private static final String COMBINED_TYPE = "all";
    // label used for shifts without an explicit type
    private static final String UNSPECIFIED_TYPE = "unspecified";


    @Inject
    public ShiftEfficiencyAnalysisControlerListener(DrtConfigGroup drtConfigGroup,
                                                    ShiftEfficiencyTracker shiftEfficiencyTracker,
                                                    Provider<DrtShiftsSpecification> drtShiftsSpecification,
                                                    MatsimServices matsimServices) {
        this.drtConfigGroup = drtConfigGroup;
        this.shiftEfficiencyTracker = shiftEfficiencyTracker;
        this.drtShiftsSpecification = drtShiftsSpecification;
        this.matsimServices = matsimServices;
        this.delimiter = matsimServices.getConfig().global().getDefaultDelimiter();
        this.runId = Optional.ofNullable(matsimServices.getConfig().controller().getRunId()).orElse(notAvailableString);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
		int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && event.getIteration() % createGraphsInterval == 0;

        ShiftEfficiencyTracker.Record record = shiftEfficiencyTracker.getCurrentRecord();
        writeAndPlotShiftEfficiency(
                record,
                record.getRevenueByShift(),
                record.getRequestsByShift(),
                record.getFinishedShifts(),
                filename(event, "shiftRevenue", ".png"),
                filename(event, "shiftRidesPerVrh", ".png"),
                filename(event, "shiftEfficiency", ".csv"),
                createGraphs);

        // breakdown by shift type (taken from the shift lifecycle events), with the combined ("all") aggregate kept for
        // backwards compatibility. Driven entirely from the events, so transient shifts without a persistent
        // specification (e.g. remote-guidance virtual shifts) are included too.
        Map<String, List<Id<DrtShift>>> shiftsByType = record.finishedShifts().keySet().stream()
                .collect(Collectors.groupingBy(id -> record.getShiftTypeById().getOrDefault(id, UNSPECIFIED_TYPE)));

        writeIterationShiftEfficiencyStats(COMBINED_TYPE,
                summarize(record, record.finishedShifts().keySet()), event.getIteration());
        for (Map.Entry<String, List<Id<DrtShift>>> entry : shiftsByType.entrySet()) {
            writeIterationShiftEfficiencyStats(entry.getKey(), summarize(record, entry.getValue()),
                    event.getIteration());
        }
    }

    /**
     * Computes the aggregated efficiency metrics over the given subset of (finished) shifts.
     */
    private String summarize(ShiftEfficiencyTracker.Record record, Collection<Id<DrtShift>> shiftIds) {
        // driven from the actual (event-based) shift run times rather than the planned specification: honest realised
        // durations, and works for transient shifts (e.g. remote-guidance virtual shifts) that have no specification.
        Map<Id<DrtShift>, Double> starts = record.getActualStartByShift();
        Map<Id<DrtShift>, Double> ends = record.getActualEndByShift();
        List<Id<DrtShift>> shifts = shiftIds.stream()
                .filter(id -> starts.containsKey(id) && ends.containsKey(id))
                .toList();

        double earliestShiftStart = shifts.stream().mapToDouble(starts::get).min().orElse(Double.NaN);
        double latestShiftEnd = shifts.stream().mapToDouble(ends::get).max().orElse(Double.NaN);

        double numberOfShifts = shifts.size();
        double numberOfShiftHours = shifts.stream()
                .mapToDouble(id -> ends.get(id) - starts.get(id))
                .sum() / 3600.;

        long uniqueVehicles = shiftIds.stream().map(id -> record.getFinishedShifts().get(id)).distinct().count();

        double totalRevenue = shiftIds.stream().mapToDouble(id -> record.revenueByShift().getOrDefault(id, 0.)).sum();
        double meanRevenuePerShift = shiftIds.stream().mapToDouble(id -> record.revenueByShift().getOrDefault(id, 0.)).average().orElse(Double.NaN);
        double meanRevenuePerShiftHour = totalRevenue / numberOfShiftHours;

        Map<Id<DrtShift>, List<Id<Request>>> requestsByShift = record.getRequestsByShift();
        double totalRides = shiftIds.stream().mapToDouble(id -> requestsByShift.getOrDefault(id, List.of()).size()).sum();
        double meanRidesPerShift = shiftIds.stream().mapToDouble(id -> requestsByShift.getOrDefault(id, List.of()).size()).average().orElse(Double.NaN);
        double meanRidesPerShiftHour = totalRides / numberOfShiftHours;

        StringJoiner stringJoiner = new StringJoiner(delimiter);
        stringJoiner
                .add(earliestShiftStart + "")
                .add(latestShiftEnd + "")
                .add(numberOfShifts + "")
                .add(numberOfShiftHours + "")
                .add(uniqueVehicles + "")
                .add(meanRevenuePerShift + "")
                .add(meanRevenuePerShiftHour + "")
                .add(totalRevenue + "")
                .add(meanRidesPerShift + "")
                .add(meanRidesPerShiftHour + "")
                .add(totalRides + "");
        return stringJoiner.toString();
    }

    private void writeAndPlotShiftEfficiency(ShiftEfficiencyTracker.Record record,
                                             Map<Id<DrtShift>, Double> revenuePerShift,
                                             Map<Id<DrtShift>, List<Id<Request>>> requestsPerShift,
                                             Map<Id<DrtShift>, Id<DvrpVehicle>> finishedShifts,
                                             String shiftRevenue,
                                             String shiftRidesPerVrh,
                                             String csvFile,
                                             boolean createGraphs) {
        Map<Id<DrtShift>, Double> starts = record.getActualStartByShift();
        Map<Id<DrtShift>, Double> ends = record.getActualEndByShift();
        Map<Id<DrtShift>, String> types = record.getShiftTypeById();
        try (var bw = IOUtils.getBufferedWriter(csvFile)) {
            bw.append(line("ShiftId", "shiftType", "actualFrom", "actualTo", "vehicle", "rides", "revenue", "ridesPerVRH", "revenuePerVRH"));

            final List<Double> ridesPerVRHList = new ArrayList<>();
            final List<Double> revenuePerVRHList = new ArrayList<>();

            for (Map.Entry<Id<DrtShift>, Double> revenuePerShiftEntry : revenuePerShift.entrySet()) {
                Id<DrtShift> shiftId = revenuePerShiftEntry.getKey();
                // driven from the actual (event-based) run time. Shifts still active at run end have no end time and are
                // skipped (their vehicle-revenue-hour is undefined). Works for transient shifts without a specification.
                Double start = starts.get(shiftId);
                Double end = ends.get(shiftId);
                if (start == null || end == null) {
                    continue;
                }
                int nRequests = requestsPerShift.getOrDefault(shiftId, Collections.EMPTY_LIST).size();
                double vehicleRevenueHour = (end - start) / 3600.;
                double ridesPerVRH = nRequests / vehicleRevenueHour;
                double revenuePerVRH = revenuePerShiftEntry.getValue() / vehicleRevenueHour;
                Id<DvrpVehicle> dvrpVehicleId = finishedShifts.get(shiftId);
				if(dvrpVehicleId != null) {
					bw.append(line(shiftId.toString(), types.getOrDefault(shiftId, UNSPECIFIED_TYPE),
							start, end,
							dvrpVehicleId.toString(), nRequests, revenuePerShiftEntry.getValue(), ridesPerVRH, revenuePerVRH));
					ridesPerVRHList.add(ridesPerVRH);
					revenuePerVRHList.add(revenuePerVRH);
				}
            }
            bw.flush();

            if (createGraphs) {
                final DefaultBoxAndWhiskerCategoryDataset ridesPerVRHDataset
                        = new DefaultBoxAndWhiskerCategoryDataset();
                final DefaultBoxAndWhiskerCategoryDataset revenuePerVRHDataset
                        = new DefaultBoxAndWhiskerCategoryDataset();

                ridesPerVRHDataset.add(ridesPerVRHList, "", "");
                revenuePerVRHDataset.add(revenuePerVRHList, "", "");

                JFreeChart chartRides = ChartFactory.createBoxAndWhiskerChart("Rides per VRH distribution", "", "Rides", ridesPerVRHDataset, false);
                JFreeChart chartRevenue = ChartFactory.createBoxAndWhiskerChart("Revenue per VRH distribution", "", "Revenue", revenuePerVRHDataset, false);

                ((BoxAndWhiskerRenderer) chartRides.getCategoryPlot().getRenderer()).setMeanVisible(false);
                ((BoxAndWhiskerRenderer) chartRevenue.getCategoryPlot().getRenderer()).setMeanVisible(false);

                ChartUtils.writeChartAsPNG(new FileOutputStream(shiftRidesPerVrh), chartRides, 1500, 1500);
                ChartUtils.writeChartAsPNG(new FileOutputStream(shiftRevenue), chartRevenue, 1500, 1500);
            }
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeIterationShiftEfficiencyStats(String shiftType, String summarizeShiftEfficiency, int it) {
        try (var bw = getAppendingBufferedWriter("drt_shift_efficiency_metrics", ".csv")) {
            if (!headerWritten) {
                headerWritten = true;
                StringJoiner stringJoiner = new StringJoiner(delimiter);
                stringJoiner
                        .add("earliestShiftStart")
                        .add("latestShiftEnd")
                        .add("numberOfShifts")
                        .add("numberOfShiftHours")
                        .add("uniqueVehicles")
                        .add("meanRevenuePerShift")
                        .add("meanRevenuePerShiftHour")
                        .add("totalRevenue")
                        .add("meanRidesPerShift")
                        .add("meanRidesPerShiftHour")
                        .add("totalRides");
                bw.write(line("runId", "iteration", "shiftType", stringJoiner.toString()));
            }
            bw.write(runId + delimiter + it + delimiter + shiftType + delimiter + summarizeShiftEfficiency);
            bw.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String filename(IterationEndsEvent event, String prefix, String extension) {
        return matsimServices.getControllerIO()
                .getIterationFilename(event.getIteration(), prefix + "_" + drtConfigGroup.getMode() + extension);
    }

    private String line(Object... cells) {
        return Arrays.stream(cells).map(Object::toString).collect(Collectors.joining(delimiter, "", "\n"));
    }

    private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
        return IOUtils.getAppendingBufferedWriter(matsimServices.getControllerIO().getOutputFilename(prefix + "_" + drtConfigGroup.getMode() + extension));
    }
}
