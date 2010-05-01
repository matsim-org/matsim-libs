package playground.yu.utils.charts;

import java.awt.Color;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SubCategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.charts.ChartUtil;

public class StackedBarChart extends ChartUtil {
	private DefaultCategoryDataset dataset;

	public StackedBarChart(String title, String categoryAxisLabel,
			String valueLabel, PlotOrientation plotOrientation) {
		super(title, categoryAxisLabel, valueLabel);
		dataset = new DefaultCategoryDataset();
		this.chart = createChart(title, categoryAxisLabel, valueLabel, dataset,
				plotOrientation);
	}

	private JFreeChart createChart(String title, String axisLabel,
			String axisLabel2, DefaultCategoryDataset dataset,
			PlotOrientation plotOrientation) {
		JFreeChart chart = ChartFactory.createStackedBarChart(title, axisLabel,
				axisLabel2, dataset, plotOrientation, true, true, false);
		return chart;
	}

	@Override
	public JFreeChart getChart() {
		return chart;
	}

	/**
	 * @param columnKeys
	 *            [ck]
	 * @param values
	 *            [][ck]
	 */
	public void addSeries(String[] rowKeys, String[] columnKeys,
			double[][] values) {
		for (int rowIdx = 0; rowIdx < values.length; rowIdx++)
			for (int columnIdx = 0; columnIdx < values[rowIdx].length; columnIdx++)
				dataset.addValue(Double.valueOf(values[rowIdx][columnIdx]),
						rowKeys[rowIdx], columnKeys[columnIdx]);
	}

	public void addSeries(String[] rowKeys, String[] columnKeys,
			String[] subCategoryKeys, double[][] values) {
		for (int rowIdx = 0; rowIdx < values.length; rowIdx++)
			for (int columnIdx = 0; columnIdx < values[rowIdx].length; columnIdx++)
				dataset.addValue(Double.valueOf(values[rowIdx][columnIdx]),
						rowKeys[rowIdx], columnKeys[columnIdx]);

		GroupedStackedBarRenderer renderer = new GroupedStackedBarRenderer();

		KeyToGroupMap map = new KeyToGroupMap(subCategoryKeys[0]);
		int size = rowKeys.length / subCategoryKeys.length;
		for (int rowIdx = 0; rowIdx < rowKeys.length; rowIdx++)
			map.mapKeyToGroup(rowKeys[rowIdx], subCategoryKeys[rowIdx / size]);
		renderer.setSeriesToGroupMap(map);

		Color[] paints = new Color[size];
		Random random = MatsimRandom.getLocalInstance();
		for (int i = 0; i < paints.length; i++)
			paints[i] = new Color(random.nextInt(256), random.nextInt(256),
					random.nextInt(256));
		for (int i = 0; i < rowKeys.length; i++)
			renderer.setSeriesPaint(i, paints[i % size]);

		CategoryPlot plot = (CategoryPlot) chart.getPlot();

		SubCategoryAxis domainAxis = new SubCategoryAxis("subCategory / column");
		for (int subCtgr = 0; subCtgr < subCategoryKeys.length; subCtgr++)
			domainAxis.addSubCategory(subCategoryKeys[subCtgr]);

		plot.setDomainAxis(domainAxis);

		plot.setRenderer(renderer);
		// plot.setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
	}

	public void addValue(double value, String rowKey, String columnKey) {
		dataset.addValue(value, rowKey, columnKey);
	}

	public static void run1(String[] args) {
		StackedBarChart chart = new StackedBarChart("TITLE", "category",
				"values", PlotOrientation.VERTICAL);
		chart.addSeries(new String[] { "rowIdxA", "rowIdxB", "rowIdxC" },
				new String[] { "ctgrA", "ctgrB", "ctgrC", "ctgrD" },
				new double[][] { { 1, 2, 3, 4 }, { 8, 7, 6, 5 },
						{ 1.9, 2.9, 1.9, 2.9 } });
		// chart.addValue(7.2, "rowKeySpecialA", "ctgrB");
		// chart.addValue(2.5, "rowKeySpecialB", "ctgrD");
		// chart.addValue(3.3, "rowIdxB", "ctgrSpecial");
		// chart.addValue(6.1, "rowIdxC", "ctgrC");
		chart.saveAsPng("../matsimTests/charts/StackedBarChartTest.png", 800,
				600);
	}

	public static void run2(String[] args) {
		StackedBarChart chart = new StackedBarChart("TITLE", "category",
				"values", PlotOrientation.VERTICAL);
		chart.addSeries(new String[] { "subCtgyIdxA+rowIdxA",
		// "subCtgyIdxA+rowIdxB", "subCtgyIdxA+rowIdxC",
				"subCtgyIdxB+rowIdxA",
		// "subCtgyIdxB+rowIdxB",
				// "subCtgyIdxB+rowIdxC"
				},// rowKeys
				new String[] { "ctgrA", "ctgrB", "ctgrC", "ctgrD" },// columnKeys
				new String[] { "subCtgyIdxA", "subCtgyIdxB" },// subCategoryKeys
				new double[][] {
				// { 1, 2, 3, 4 },
						// { 8, 7, 6, 5 },
						{ 1.9, 2.9, 1.9, 2.9 }, { 2, 3, 4, 1 }
				// { 3, 4, 5, 6 },
				// { 1, 5, 2, 6 }
				});
		// chart.addValue(7.2, "rowKeySpecialA", "ctgrB");
		// chart.addValue(2.5, "rowKeySpecialB", "ctgrD");
		// chart.addValue(3.3, "rowIdxB", "ctgrSpecial");
		// chart.addValue(6.1, "rowIdxC", "ctgrC");
		chart.saveAsPng("../matsimTests/charts/StackedBarChartTest2.png", 800,
				600);
	}

	public static void main(String[] args) {
		run2(args);
	}
}
