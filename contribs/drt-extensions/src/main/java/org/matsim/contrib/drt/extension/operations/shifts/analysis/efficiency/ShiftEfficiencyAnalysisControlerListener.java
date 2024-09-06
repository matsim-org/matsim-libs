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
import jakarta.inject.Provider;
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
                record.getRevenueByShift(),
                record.getRequestsByShift(),
                record.getFinishedShifts(),
                filename(event, "shiftRevenue", ".png"),
                filename(event, "shiftRidesPerVrh", ".png"),
                filename(event, "shiftEfficiency", ".csv"),
                createGraphs);

        List<DrtShiftSpecification> finishedShifts = record.finishedShifts()
                .keySet()
                .stream()
                .map(id -> drtShiftsSpecification.get().getShiftSpecifications().get(id))
                .toList();

        double earliestShiftStart = finishedShifts.stream().map(DrtShiftSpecification::getStartTime).mapToDouble(d -> d).min().orElse(Double.NaN);
        double latestShiftEnd = finishedShifts.stream().map(DrtShiftSpecification::getEndTime).mapToDouble(d -> d).min().orElse(Double.NaN);

        double numberOfShifts = finishedShifts.size();
        double numberOfShiftHours = finishedShifts.
                stream()
                .map(s -> (s.getEndTime() - s.getStartTime()) - (s.getBreak().isPresent() ? s.getBreak().get().getDuration() : 0.))
                .mapToDouble(d -> d)
                .sum() / 3600.;

        long uniqueVehicles = record.getFinishedShifts().values().stream().distinct().count();

        double totalRevenue = record.revenueByShift().values().stream().mapToDouble(d -> d).sum();
        double meanRevenuePerShift = record.revenueByShift().values().stream().mapToDouble(d -> d).average().orElse(Double.NaN);
        double meanRevenuePerShiftHour = totalRevenue / numberOfShiftHours;

        double totalRides = record.getRequestsByShift().values().stream().mapToDouble(List::size).sum();
        double meanRidesPerShift = record.getRequestsByShift().values().stream().mapToDouble(List::size).average().orElse(Double.NaN);
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
        writeIterationShiftEfficiencyStats(stringJoiner.toString(), event.getIteration());

    }

    private void writeAndPlotShiftEfficiency(Map<Id<DrtShift>, Double> revenuePerShift,
                                             Map<Id<DrtShift>, List<Id<Request>>> requestsPerShift,
                                             Map<Id<DrtShift>, Id<DvrpVehicle>> finishedShifts,
                                             String shiftRevenue,
                                             String shiftRidesPerVrh,
                                             String csvFile,
                                             boolean createGraphs) {
        try (var bw = IOUtils.getBufferedWriter(csvFile)) {
            bw.append(line("ShiftId", "plannedFrom", "plannedTo", "vehicle", "rides", "revenue", "ridesPerVRH", "revenuePerVRH"));

            final List<Double> ridesPerVRHList = new ArrayList<>();
            final List<Double> revenuePerVRHList = new ArrayList<>();

            for (Map.Entry<Id<DrtShift>, Double> revenuePerShiftEntry : revenuePerShift.entrySet()) {
                DrtShiftSpecification drtShift = drtShiftsSpecification.get().getShiftSpecifications().get(revenuePerShiftEntry.getKey());
                int nRequests = requestsPerShift.getOrDefault(revenuePerShiftEntry.getKey(), Collections.EMPTY_LIST).size();
                double vehicleRevenueHour = drtShift.getEndTime() - drtShift.getStartTime();
                if (drtShift.getBreak().isPresent()) {
                    vehicleRevenueHour -= drtShift.getBreak().get().getDuration();
                }
                vehicleRevenueHour /= 3600.;
                double ridesPerVRH = nRequests / vehicleRevenueHour;
                double revenuePerVRH = revenuePerShiftEntry.getValue() / vehicleRevenueHour;
                Id<DvrpVehicle> dvrpVehicleId = finishedShifts.get(drtShift.getId());
				if(dvrpVehicleId != null) {
					bw.append(line(drtShift.getId().toString(), drtShift.getStartTime(), drtShift.getEndTime(),
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

    private void writeIterationShiftEfficiencyStats(String summarizeShiftEfficiency, int it) {
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
                bw.write(line("runId", "iteration", stringJoiner.toString()));
            }
            bw.write(runId + delimiter + it + delimiter + summarizeShiftEfficiency);
            bw.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String filename(IterationEndsEvent event, String prefix, String extension) {
        return matsimServices.getControlerIO()
                .getIterationFilename(event.getIteration(), prefix + "_" + drtConfigGroup.getMode() + extension);
    }

    private String line(Object... cells) {
        return Arrays.stream(cells).map(Object::toString).collect(Collectors.joining(delimiter, "", "\n"));
    }

    private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
        return IOUtils.getAppendingBufferedWriter(matsimServices.getControlerIO().getOutputFilename(prefix + "_" + drtConfigGroup.getMode() + extension));
    }
}
