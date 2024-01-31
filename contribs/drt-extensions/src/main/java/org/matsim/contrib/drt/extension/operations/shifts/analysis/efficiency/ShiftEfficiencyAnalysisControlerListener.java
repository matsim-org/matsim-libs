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

import jakarta.inject.Provider;
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

    @Inject
    public ShiftEfficiencyAnalysisControlerListener(DrtConfigGroup drtConfigGroup,
                                                    ShiftEfficiencyTracker shiftEfficiencyTracker,
                                                    Provider<DrtShiftsSpecification> drtShiftsSpecification,
                                                    MatsimServices matsimServices) {
        this.drtConfigGroup = drtConfigGroup;
        this.shiftEfficiencyTracker = shiftEfficiencyTracker;
        this.drtShiftsSpecification = drtShiftsSpecification;
        this.matsimServices = matsimServices;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
		int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && event.getIteration() % createGraphsInterval == 0;

		writeAndPlotShiftEfficiency(
                shiftEfficiencyTracker.getCurrentRecord().getRevenueByShift(),
                shiftEfficiencyTracker.getCurrentRecord().getRequestsByShift(),
                shiftEfficiencyTracker.getCurrentRecord().getFinishedShifts(),
                filename(event, "shiftRevenue", ".png"),
                filename(event, "shiftRidesPerVrh", ".png"),
                filename(event, "shiftEfficiency", ".csv"),
                createGraphs);
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

    private String filename(IterationEndsEvent event, String prefix, String extension) {
        return matsimServices.getControlerIO()
                .getIterationFilename(event.getIteration(), prefix + "_" + drtConfigGroup.getMode() + extension);
    }

    private static String line(Object... cells) {
        return Arrays.stream(cells).map(Object::toString).collect(Collectors.joining(";", "", "\n"));
    }
}
