package playground.joel.analysis;

import ch.ethz.idsc.tensor.Tensor;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.SortOrder;
import playground.clruch.utils.GlobalAssert;

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

        File stackedChart = new File(directory, fileTitle + ".png");
        ChartUtilities.saveChartAsPNG(stackedChart, chart, width, height);
        GlobalAssert.that(stackedChart.exists() && !stackedChart.isDirectory());
        System.out.println("exported " + fileTitle + ".png");
    }
}
