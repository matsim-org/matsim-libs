package playground.yu.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.core.utils.charts.ChartUtil;

public class StackedBarChart extends ChartUtil {
	private DefaultCategoryDataset dataset;

	public StackedBarChart(String title, String categoryAxisLabel,
			String valueLabel) {
		super(title, categoryAxisLabel, valueLabel);
		dataset = new DefaultCategoryDataset();
		this.chart = createChart(title, categoryAxisLabel, valueLabel, dataset);
	}

	private JFreeChart createChart(String title, String axisLabel,
			String axisLabel2, DefaultCategoryDataset dataset) {
		JFreeChart chart = ChartFactory.createStackedBarChart(title, axisLabel,
				axisLabel2, dataset, PlotOrientation.HORIZONTAL, true, false,
				false);
		return chart;
	}

	@Override
	protected JFreeChart getChart() {
		return chart;
	}

	/**
	 * @param columnKeys
	 *            [ck]
	 * @param values
	 *            [][ck]
	 */
	private void addSeries(String[] columnKeys, double[][] values) {
		for (int rowKey = 0; rowKey < values.length; rowKey++)
			for (int columnKey = 0; columnKey < values[rowKey].length; columnKey++)
				dataset.addValue(Double.valueOf(values[rowKey][columnKey]),
						"rowKey" + String.valueOf(rowKey),
						columnKeys[columnKey]);
	}

	public void addValue(double value, String rowKey, String columnKey) {
		dataset.addValue(value, rowKey, columnKey);
	}

	public static void main(String[] args) {
		StackedBarChart chart = new StackedBarChart("TITLE", "category",
				"values");
		chart.addSeries(new String[] { "A", "B", "C", "D" }, new double[][] {
				{ 1, 1, 3, 1 }, { 0, 2, 2, 2 } });
		chart.addValue(12, "rowKey12", "D");
		chart.saveAsPng("output/StackedBarChartTest.png", 800, 600);
	}
}
