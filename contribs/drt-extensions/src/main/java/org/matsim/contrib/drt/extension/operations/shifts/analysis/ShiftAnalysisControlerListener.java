package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.analysis.DensityScatterPlots;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftAnalysisControlerListener implements IterationEndsListener {

    private final static Logger logger = LogManager.getLogger(ShiftAnalysisControlerListener.class);

	private final String delimiter;

	private DrtConfigGroup drtConfigGroup;
	private final ShiftDurationXY shiftDurationXY;
    private final BreakCorridorXY breakCorridorXY;
    private final MatsimServices matsimServices;

    @Inject
    public ShiftAnalysisControlerListener(Config config, DrtConfigGroup drtConfigGroup,
										  ShiftDurationXY shiftDurationXY,
										  BreakCorridorXY breakCorridorXY,
										  MatsimServices matsimServices) {
		this.drtConfigGroup = drtConfigGroup;
		this.shiftDurationXY = shiftDurationXY;
        this.breakCorridorXY = breakCorridorXY;
        this.matsimServices = matsimServices;
		this.delimiter = config.global().getDefaultDelimiter();
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
		int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && event.getIteration() % createGraphsInterval == 0;

		writeAndPlotShiftDurationComparison(shiftDurationXY.getShift2plannedVsActualDuration(),
                filename(event, "shiftDurationComparison", ".png"),
                filename(event, "shiftDurationComparison", ".csv"),
                createGraphs);
        writeAndPlotBreakDurationComparison(shiftDurationXY.getShift2plannedVsActualBreakDuration(),
                filename(event, "shiftBreakDurationComparison", ".png"),
                filename(event, "shiftBreakDurationComparison", ".csv"),
                createGraphs);
        writeAndPlotBreakTimesComparison(breakCorridorXY.getShift2plannedVsActualBreakStart(),
                breakCorridorXY.getShift2plannedVsActualBreakEnd(),
                filename(event, "shiftBreakEndTimesComparison", ".png"),
                filename(event, "shiftBreakEndTimesComparison", ".csv"),
                createGraphs);
    }

    private void writeAndPlotBreakTimesComparison(Map<Id<DrtShift>, Tuple<Double, Double>> shift2plannedVsActualBreakStart,
                                                  Map<Id<DrtShift>, Tuple<Double, Double>> shift2plannedVsActualBreakEnd,
                                                  String pngFile, String csvFile,
                                                  boolean createGraphs) {
        try (var bw = IOUtils.getBufferedWriter(csvFile)) {
            XYSeries times = new XYSeries("breakEndTimes", true, true);
            bw.append(line("ShiftId", "earliestBreakStart", "latestBreakEnd", "actualStart", "actualEnd"));
            for (Map.Entry<Id<DrtShift>, Tuple<Double, Double>> entry: shift2plannedVsActualBreakStart.entrySet()) {
                final Tuple<Double, Double> breakEndTuple = shift2plannedVsActualBreakEnd.get(entry.getKey());
                if(breakEndTuple != null) {
                    bw.append(line(entry.getKey(), entry.getValue().getFirst(), breakEndTuple.getFirst(),
                            entry.getValue().getSecond(), breakEndTuple.getSecond()));
                    times.add(breakEndTuple.getFirst(), breakEndTuple.getSecond());
                } else {
                    logger.warn("No end time found for break of shift " + entry.getKey());
                }
            }

            if (createGraphs) {
                final JFreeChart chart2 = DensityScatterPlots.createPlot("Break end times", "Planned break end time",
                        "Actual break end time", times,null);
                ChartUtils.writeChartAsPNG(new FileOutputStream(pngFile), chart2, 1500, 1500);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeAndPlotShiftDurationComparison(Map<Id<DrtShift>, Tuple<Double, Double>> shift2plannedVsActualDuration,
                                                     String plotFilename, String csvFileName, boolean createGraphs) {
        try (var bw = IOUtils.getBufferedWriter(csvFileName)) {
            XYSeries times = new XYSeries("shiftDurations", true, true);

            bw.append(line("ShiftId", "actualDuration", "plannedDuration", "deviate"));
            for (Map.Entry<Id<DrtShift>, Tuple<Double, Double>> entry: shift2plannedVsActualDuration.entrySet()) {

                    double actualDuration = entry.getValue().getSecond();
                    double plannedDuration = entry.getValue().getFirst();
                    bw.append(line(entry.getKey(), actualDuration, plannedDuration,
                            actualDuration - plannedDuration));
                    times.add(actualDuration, plannedDuration);

            }

            if (createGraphs) {
                final JFreeChart chart2 = DensityScatterPlots.createPlot("Shift durations", "Actual shift duration [s]",
                        "Initially planned shift duration [s]", times,null);
                ChartUtils.writeChartAsPNG(new FileOutputStream(plotFilename), chart2, 1500, 1500);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeAndPlotBreakDurationComparison(Map<Id<DrtShift>, Tuple<Double, Double>> shift2plannedVsActualBreakDuration,
                                                     String plotFilename, String csvFileName, boolean createGraphs) {
        try (var bw = IOUtils.getBufferedWriter(csvFileName)) {
            XYSeries times = new XYSeries("breakDurations", true, true);

            bw.append(line("ShiftId", "actualDuration", "plannedDuration", "deviate"));
            for (Map.Entry<Id<DrtShift>, Tuple<Double, Double>> entry: shift2plannedVsActualBreakDuration.entrySet()) {

                double actualDuration = entry.getValue().getSecond();
                double plannedDuration = entry.getValue().getFirst();
                bw.append(line(entry.getKey(), actualDuration, plannedDuration,
                        actualDuration - plannedDuration));
                times.add(actualDuration, plannedDuration);

            }

            if (createGraphs) {
                final JFreeChart chart2 = DensityScatterPlots.createPlot("Break durations", "Actual break duration [s]",
                        "Initially planned break duration [s]", times,null);
                ChartUtils.writeChartAsPNG(new FileOutputStream(plotFilename), chart2, 1500, 1500);
            }
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
}
