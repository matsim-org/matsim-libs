package playground.yu.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
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

	public void addValue(double value, String rowKey, String columnKey) {
		dataset.addValue(value, rowKey, columnKey);
	}

	public static void main(String[] args) {
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
		chart.saveAsPng("output/StackedBarChartTest.png", 800, 600);
	}
}
