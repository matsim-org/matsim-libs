package playground.clruch.analysis;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.SortOrder;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;

/**
 * Created by Joel on 29.06.2017.
 */
public class UniqueDiagrams {

    public static void distanceStack(File directory, String fileTitle, String diagramTitle, //
                                     Double rebalance, Double pickup, Double customer) throws Exception {
        String[] labels = {"With Customer", "Pickup", "Rebalancing"};
        int width = 250; // Width of the image
        int height = 400; // Height of the image

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(customer, labels[0], "s1");
        dataset.addValue(pickup, labels[1], "s1");
        dataset.addValue(rebalance, labels[2], "s1");

        JFreeChart chart = ChartFactory.createStackedBarChart(
                diagramTitle, "", "",
                dataset, PlotOrientation.VERTICAL, true, false, false);

        chart.setBackgroundPaint(Color.white);
        chart.getCategoryPlot().getRangeAxis().setRange(0, 1.0);
        chart.getCategoryPlot().setRangeGridlinePaint(Color.lightGray);
        chart.getCategoryPlot().getDomainAxis().setTickLabelsVisible(false);
        chart.getCategoryPlot().getDomainAxis().setLowerMargin(0.0);
        chart.getCategoryPlot().getDomainAxis().setUpperMargin(0.0);
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(DiagramCreator.tickFont);
        chart.getTitle().setFont(DiagramCreator.titleFont);
        LegendItemCollection legend = new LegendItemCollection();
        legend.add(new LegendItem(labels[0], Color.red));
        legend.add(new LegendItem(labels[1], Color.blue));
        legend.add(new LegendItem(labels[2], Color.green));
        chart.getCategoryPlot().setFixedLegendItems(legend);
        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        chart.getLegend().setItemFont(DiagramCreator.tickFont);
        chart.getLegend().setSortOrder(SortOrder.DESCENDING);

        DiagramCreator.savePlot(directory, fileTitle, chart, width, height);
    }

    public static void distanceDistribution(File directory, String fileTitle, String diagramTitle, //
                                            boolean filter, String dataDirectory) throws Exception {
        String[] labels = {"With Customer", "Pickup", "Rebalancing"};
        Double[] scale = {-0.001, 0.001, 0.001};
        stackedTimeChart(directory, fileTitle, diagramTitle, filter, DiagramCreator.filterSize, 12,15, scale, //
                labels, "Distance [km]", dataDirectory);
    }

    public static void statusDistribution(File directory, String fileTitle, String diagramTitle, //
                                            boolean filter, String dataDirectory) throws Exception {
        String[] labels = {"With Customer", "Pickup", "Rebalancing", "Stay"};
        Double[] scale = {1.0, 1.0, 1.0, 1.0};
        stackedTimeChart(directory, fileTitle, diagramTitle, filter, 60,6 ,10, scale, //
                labels, "Vehicles", dataDirectory);
    }

    public static void stackedTimeChart(File directory, String fileTitle, String diagramTitle, //
                                            boolean filter, int filterSize, int from, int to, Double[] scale, //
                                        String[] labels, String yAxisLabel, String dataDirectory) throws Exception {
        int width = 1000; /* Width of the image */
        int height = 750; /* Height of the image */

        GlobalAssert.that(labels.length >= from - to && scale.length >= from - to);

        Tensor table = CsvFormat.parse(Files.lines(Paths.get(dataDirectory + "/summary.csv")));
        table = Transpose.of(table);

        Tensor values = DiagramCreator.filter(table.extract(from, to), table.get(0), filterSize, filter);

        final TimeTableXYDataset dataset = new TimeTableXYDataset();
        Tensor time = table.get(0);
        for (int i = 0; i < values.length(); i++)
            for (int j = 0; j < time.length(); j++)
                dataset.add(DiagramCreator.toTime(time.Get(j).number().doubleValue()), //
                        values.Get(i, j).number().doubleValue()*scale[i], labels[i]);

        JFreeChart timechart = ChartFactory.createStackedXYAreaChart(diagramTitle, "Time", //
                yAxisLabel, dataset, PlotOrientation.VERTICAL, false, false, false);

        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        DateAxis domainAxis = new DateAxis();
        domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.SECOND, 1));
        timechart.getXYPlot().setDomainAxis(domainAxis);
        timechart.getXYPlot().getDomainAxis().setAutoTickUnitSelection(true);

        timechart.getTitle().setFont(DiagramCreator.titleFont);
        timechart.getXYPlot().getDomainAxis().setLabelFont(DiagramCreator.axisFont);
        timechart.getXYPlot().getRangeAxis().setLabelFont(DiagramCreator.axisFont);
        timechart.getXYPlot().getDomainAxis().setTickLabelFont(DiagramCreator.tickFont);
        timechart.getXYPlot().getRangeAxis().setTickLabelFont(DiagramCreator.tickFont);

        LegendTitle legend = new LegendTitle(timechart.getXYPlot().getRenderer());
        legend.setItemFont(DiagramCreator.tickFont);
        legend.setPosition(RectangleEdge.TOP);
        timechart.addLegend(legend);

        DiagramCreator.savePlot(directory, fileTitle, timechart, width, height);
    }

}
