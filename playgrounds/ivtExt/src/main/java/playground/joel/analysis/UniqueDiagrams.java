package playground.joel.analysis;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.SortOrder;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Joel on 29.06.2017.
 */
public class UniqueDiagrams {

    public static void distanceStack(File directory, String fileTitle, String diagramTitle, //
                                     Double rebalance, Double pickup, Double customer) throws Exception {
        String[] labels = {"With Customer", "Pickup", "Rebalancing"};
        int width = 250; /* Width of the image */
        int height = 400; /* Height of the image */

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
                                            boolean filter) throws Exception {
        int width = 1000; /* Width of the image */
        int height = 750; /* Height of the image */

        Tensor table = CsvFormat.parse(Files.lines(Paths.get("output/data/summary.csv")));
        table = Transpose.of(table);

        Tensor values = DiagramCreator.filter(table.extract(12, 15), table.get(0), DiagramCreator.filterSize, filter);
        String[] labels = {"With Customer", "Pickup", "Rebalancing"};

        final TimeTableXYDataset dataset = new TimeTableXYDataset();
        Tensor time = table.get(0);
        for (int i = 0; i < values.length(); i++)
            for (int j = 0; j < time.length(); j++)
                dataset.add(DiagramCreator.toTime(time.Get(j).number().doubleValue()), //
                        values.Get(i, j).number().doubleValue()*(i == 0 ? -1.0 : 1.0)/1000, labels[i]);

        JFreeChart timechart = ChartFactory.createStackedXYAreaChart(diagramTitle, "Time", //
                "Distance [km]", dataset, PlotOrientation.VERTICAL, false, false, false);

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
