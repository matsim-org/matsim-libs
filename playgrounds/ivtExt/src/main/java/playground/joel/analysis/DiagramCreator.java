package playground.joel.analysis;

import java.awt.*;
import java.io.File;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Dimensions;
import org.jfree.ui.RectangleEdge;
import playground.clruch.utils.GlobalAssert;

/**
 * Created by Joel on 04.03.2017.
 */
public class DiagramCreator {
    public static final int filterSize = 120;

    static Second toTime(double time) {
        int days = (int) (time / 86400) + 1;
        int hours = (int) (time / 3600) - (days - 1) * 24;
        int minutes = (int) (time / 60) - hours * 60 - (days - 1) * 1440;
        int seconds = (int) time - minutes * 60 - hours * 3600 - (days - 1) * 86400;
        Second second = new Second(seconds, minutes, hours, days, 1, 2017); // month and year can not be zero
        return second;
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, //
                                     Tensor time, Tensor values) throws Exception {
        createDiagram(directory, fileTitle, diagramTitle, time, values, 1.1, false);
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, //
                                     Tensor time, Tensor values, boolean filter) throws Exception {
        createDiagram(directory, fileTitle, diagramTitle, time, values, 1.1, filter);
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, Tensor time, //
                                     Tensor values, Double maxRange) throws Exception {
        createDiagram(directory, fileTitle, diagramTitle, time, values, maxRange, false);
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, Tensor time, //
                                     Tensor valuesIn, Double maxRange, boolean filter) throws Exception {
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        Tensor values = filter(valuesIn, time, filterSize, filter);
        String[] labels = {"50% quantile", "95% quantile", "Mean"}; // only displayed for waiting time plots
        for (int i = 0; i < values.length(); i++) {
            final TimeSeries series = new TimeSeries(labels[i]);
            for (int j = 0; j < time.length(); j++) {
                Second daytime = toTime(time.Get(j).number().doubleValue());
                series.add(daytime, diagramTitle == "Waiting Times" ? values.Get(i, j).number().doubleValue()/60 : //
                        values.Get(i, j).number().doubleValue());
            }
            dataset.addSeries(series);
        }

        String valueAxisLabel = "Value";
        if (diagramTitle == "Waiting Times") {
            valueAxisLabel = "Waiting Times [min]";
        }
        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Time", valueAxisLabel, //
                dataset, false, false, false);

        // range and colors of the background/grid
        timechart.getXYPlot().getRangeAxis().setRange(0, maxRange);
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        // line thickness
        for (int k = 0; k < time.length(); k++) {
            timechart.getXYPlot().getRenderer().setSeriesStroke(k, new BasicStroke(2.0f));
        }

        // set text fonts
        Font titleFont = new Font("Dialog", Font.BOLD, 24);
        Font axisFont = new Font("Dialog", Font.BOLD, 18);
        Font tickFont = new Font("Dialog", Font.PLAIN, 14);
        timechart.getTitle().setFont(titleFont);
        timechart.getXYPlot().getDomainAxis().setLabelFont(axisFont);
        timechart.getXYPlot().getRangeAxis().setLabelFont(axisFont);
        timechart.getXYPlot().getDomainAxis().setTickLabelFont(tickFont);
        timechart.getXYPlot().getRangeAxis().setTickLabelFont(tickFont);

        // add legend for waiting time plots
        if (diagramTitle == "Waiting Times") {
            LegendTitle legend = new LegendTitle(timechart.getXYPlot().getRenderer());
            legend.setItemFont(tickFont);
            legend.setPosition(RectangleEdge.TOP);
            timechart.addLegend(legend);
        }

        // save plot as png
        int width = 1200; /* Width of the image */
        int height = 900; /* Height of the image */
        File timeChart = new File(directory, fileTitle + ".png");
        ChartUtilities.saveChartAsPNG(timeChart, timechart, width, height);
        GlobalAssert.that(timeChart.exists() && !timeChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }

    /**
     * this function applies a standard moving average filter of length size to values //
     * if filter is set to true in AnalyzeAll
     *
     * @param values
     * @param size
     */
    public static Tensor filter(Tensor values, Tensor time, int size, boolean filter) {
        if (filter) {
            Tensor temp = values.copy();
            int offset = (int) (size / 2.0);
            for (int i = 0; i < Dimensions.of(values).get(0); i++) {
                for (int j = size % 2 == 0 ? offset - 1 : offset; j < time.length() - offset; j++) {
                    double sum = 0;
                    for (int k = size % 2 == 0 ? -offset + 1 : -offset; k <= offset; k++) {
                        if (size % 2 == 0) {
                            sum += values.Get(i, j + k).number().doubleValue();
                        } else {
                            sum += values.Get(i, j + k).number().doubleValue();
                        }
                    }
                    temp.set(RealScalar.of(sum / size), i, j);
                }
            }
            return temp;
        } else return values;
    }
}
