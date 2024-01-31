package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftHistogramChart {
    static JFreeChart getGraphic(final ShiftHistogram.DataFrame dataFrame, int iteration) {
        final XYSeriesCollection xyData = new XYSeriesCollection();
        final XYSeries shiftStartSerie = new XYSeries("shift start", false, true);
        final XYSeries shiftEndSerie = new XYSeries("shift end", false, true);
        final XYSeries activeSerie = new XYSeries("active", false, true);
        final XYSeries shiftBreakStartSerie = new XYSeries("shift break start", false, true);
        final XYSeries shiftBreakEndSerie = new XYSeries("shift break end", false, true);
        final XYSeries activeBreakSerie = new XYSeries("active break", false, true);
        int active = 0;
        int activeBreak = 0;
        for (int i = 0; i < dataFrame.countsStart.length; i++) {
            activeBreak = activeBreak + dataFrame.countsBreaksStart[i] - dataFrame.countsBreaksEnd[i];
            active = active + dataFrame.countsStart[i] - dataFrame.countsEnd[i] - dataFrame.countsBreaksStart[i] + dataFrame.countsBreaksEnd[i];
            double hour = i*dataFrame.binSize / 60.0 / 60.0;
            shiftStartSerie.add(hour, dataFrame.countsStart[i]);
            shiftEndSerie.add(hour, dataFrame.countsEnd[i]);
            activeSerie.add(hour, active);
            shiftBreakStartSerie.add(hour, dataFrame.countsBreaksStart[i]);
            shiftBreakEndSerie.add(hour, dataFrame.countsBreaksEnd[i]);
            activeBreakSerie.add(hour, activeBreak);
        }

        xyData.addSeries(shiftStartSerie);
        xyData.addSeries(shiftEndSerie);
        xyData.addSeries(activeSerie);
        xyData.addSeries(shiftBreakStartSerie);
        xyData.addSeries(shiftBreakEndSerie);
        xyData.addSeries(activeBreakSerie);

        final JFreeChart chart = ChartFactory.createXYStepChart(
                "Shift Histogram, it." + iteration,
                "time", "# shifts/breaks",
                xyData,
                PlotOrientation.VERTICAL,
                true,   // legend
                false,   // tooltips
                false   // urls
        );

        XYPlot plot = chart.getXYPlot();

        final CategoryAxis axis1 = new CategoryAxis("hour");
        axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
        plot.setDomainAxis(new NumberAxis("time"));

        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(2, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(3, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(4, new BasicStroke(2.0f));
        plot.getRenderer().setSeriesStroke(5, new BasicStroke(2.0f));
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.gray);
        plot.setDomainGridlinePaint(Color.gray);

        return chart;
    }

    /**
     * Writes a graphic showing the number of shift starts, ends and active shifts
     * to the specified file.
     *
     * @param shiftHistogram
     * @param filename
     *
     */
    public static void writeGraphic(ShiftHistogram shiftHistogram, final String filename) {
        try {
            ChartUtils.saveChartAsPNG(new File(filename), getGraphic(shiftHistogram.getData(), shiftHistogram.getIteration()), 1024, 768);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
