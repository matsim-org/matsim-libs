package playground.clruch.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleEdge;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Dimensions;

/**
 * Created by Joel on 04.03.2017.
 */
public class DiagramCreator {
    public static final int filterSize = 120;

    public static Font titleFont = new Font("Dialog", Font.BOLD, 24);
    public static Font axisFont = new Font("Dialog", Font.BOLD, 18);
    public static Font tickFont = new Font("Dialog", Font.PLAIN, 14);

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
        if (maxRange != -1.0) timechart.getXYPlot().getRangeAxis().setRange(0, maxRange);
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        // line thickness
        for (int k = 0; k < time.length(); k++) {
            timechart.getXYPlot().getRenderer().setSeriesStroke(k, new BasicStroke(2.0f));
        }

        // set text fonts
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
        int width = 1000; /* Width of the image */
        int height = 750; /* Height of the image */
        savePlot(directory, fileTitle, timechart, width, height);
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

    public static void binCountGraph(File directory, String fileTitle, String diagramTitle, //
                                     Tensor binCounter, double binSize, double scaling, //
                                     String valueAxisLabel, String rangeAxisLabel, String rangeAxisAppendix, //
                                     int width, int height) throws Exception {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < binCounter.length(); i++) {
            dataset.addValue(binCounter.Get(i).number().doubleValue()*scaling, "", //
                    binName(binSize, i, rangeAxisAppendix));
        }
        JFreeChart chart = ChartFactory.createBarChart(
                diagramTitle,
                rangeAxisLabel, valueAxisLabel,
                dataset, PlotOrientation.VERTICAL,
                false, false, false);

        chart.getPlot().setBackgroundPaint(Color.white);
        chart.getCategoryPlot().setRangeGridlinePaint(Color.lightGray);
        chart.getCategoryPlot().getDomainAxis().setTickLabelFont(tickFont);
        chart.getCategoryPlot().getDomainAxis().setLowerMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setUpperMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setCategoryMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        chart.getCategoryPlot().getDomainAxis().setLabelFont(axisFont);
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(tickFont);
        chart.getCategoryPlot().getRangeAxis().setLabelFont(axisFont);
        chart.getTitle().setFont(titleFont);

        DiagramCreator.savePlot(directory, fileTitle, chart, width, height);
    }

    public static String binName(double binSize, int i, String appedix) {
        return i*binSize + " - " + (i+1)*binSize + appedix;
    }

    public static void createDiagram(File directory, String fileTitle, String diagramTitle, Tensor values, String valueAxisLabel) //
            throws Exception {
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        final TimeSeries series = new TimeSeries("");
        for (int i = 0; i < values.length(); i++) {
            Second daytime = toTime(i*108000/values.length());
            series.add(daytime, values.Get(i).number().doubleValue());
        }
        dataset.addSeries(series);

        JFreeChart timechart = ChartFactory.createTimeSeriesChart(diagramTitle, "Time", valueAxisLabel, //
                dataset, false, false, false);

        // range and colors of the background/grid
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        // line thickness
        for (int k = 0; k < values.length(); k++) {
            timechart.getXYPlot().getRenderer().setSeriesStroke(k, new BasicStroke(2.0f));
        }

        // set text fonts
        timechart.getTitle().setFont(titleFont);
        timechart.getXYPlot().getDomainAxis().setLabelFont(axisFont);
        timechart.getXYPlot().getRangeAxis().setLabelFont(axisFont);
        timechart.getXYPlot().getDomainAxis().setTickLabelFont(tickFont);
        timechart.getXYPlot().getRangeAxis().setTickLabelFont(tickFont);

        // save plot as png
        int width = 1000; // Width of the image
        int height = 750; // Height of the image
        savePlot(directory, fileTitle, timechart, width, height);
    }

    public static void savePlot(File directory, String fileTitle, JFreeChart chart, int width, int height)
            throws Exception {
        File fileChart = new File(directory, fileTitle + ".png");
        ChartUtilities.saveChartAsPNG(fileChart, chart, width, height);
        GlobalAssert.that(fileChart.exists() && !fileChart.isDirectory());
        System.out.println("Exported " + fileTitle + ".png");
    }
}
