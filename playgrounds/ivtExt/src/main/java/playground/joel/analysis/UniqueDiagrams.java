package playground.joel.analysis;

import ch.ethz.idsc.tensor.Tensor;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.SortOrder;

import java.awt.*;
import java.io.File;

/**
 * Created by Joel on 29.06.2017.
 */
public class UniqueDiagrams {
    static Font titleFont = new Font("Dialog", Font.BOLD, 24);
    static Font axisFont = new Font("Dialog", Font.BOLD, 18);
    static Font tickFont = new Font("Dialog", Font.PLAIN, 14);

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
        chart.getCategoryPlot().getRangeAxis().setTickLabelFont(tickFont);
        chart.getTitle().setFont(titleFont);
        LegendItemCollection legend = new LegendItemCollection();
        legend.add(new LegendItem(labels[0], Color.red));
        legend.add(new LegendItem(labels[1], Color.blue));
        legend.add(new LegendItem(labels[2], Color.green));
        chart.getCategoryPlot().setFixedLegendItems(legend);
        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        chart.getLegend().setItemFont(tickFont);
        chart.getLegend().setSortOrder(SortOrder.DESCENDING);

        DiagramCreator.savePlot(directory, fileTitle, chart, width, height);
    }

    public static void binCountGraph(File directory, String fileTitle, String diagramTitle, //
                                     Tensor binCounter, double binSize, double scaling, //
                                     String valueAxisLabel, String rangeAxisLabel, String rangeAxisAppendix) throws Exception {
        int width = 1600; /* Width of the image */
        int height = 800; /* Height of the image */

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < binCounter.length(); i++) {
            dataset.addValue(binCounter.Get(i).number().doubleValue()*scaling, "", //
                    binName(binSize, i, rangeAxisAppendix));
        }
        JFreeChart chart = ChartFactory.createBarChart(
                diagramTitle,
                rangeAxisLabel, valueAxisLabel,
                dataset,PlotOrientation.VERTICAL,
                false, false, false);

        chart.setBackgroundPaint(Color.white);
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
}
