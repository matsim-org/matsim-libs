package playground.singapore.calibration.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.core.utils.charts.ChartUtil;

/**
 * Creates two-dimensional, vertical stacked bar charts.
 *
 * @author achakirov
 */

public class StackedBarChart extends ChartUtil {
	
	private final String[] categories;
	private final DefaultCategoryDataset dataset;
	private final CategoryPlot plot;

	/**
	 * Creates a new StackedBarChart with default category-labels (numbered from 1 to the number of categories).
	 * The same as {@link #StackedBarChart(String, String, String, String[]) StackedBarChart(title, xAxisLabel, yAxisLabel, null)}.
	 *
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 */
	public StackedBarChart(final String title, final String xAxisLabel, final String yAxisLabel) {
		this(title, xAxisLabel, yAxisLabel, new String[]{});
	}

	/**
	 * Creates a new StackedBarChart with the specified category-labels.
	 *
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 * @param categories
	 */
	public StackedBarChart(final String title, final String xAxisLabel, final String yAxisLabel, final String[] categories) {
		super(title, xAxisLabel, yAxisLabel);
		this.dataset = new DefaultCategoryDataset();
		this.chart = createChart(title, xAxisLabel, yAxisLabel, this.dataset);
		this.plot = this.chart.getCategoryPlot();
		this.categories = categories.clone();

	//	addDefaultFormatting();


	}

	public DefaultCategoryDataset getDataset() {
		return dataset;
	}

	@Override
	public JFreeChart getChart() {
		return this.chart;
	}

	private JFreeChart createChart(final String title, final String categoryAxisLabel,
			final String valueAxisLabel, final CategoryDataset dataset) {
		return ChartFactory.createStackedBarChart(title, categoryAxisLabel, valueAxisLabel, 
				dataset, PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
	}

	/**
	 * Adds a new data series to the chart with the specified title.
	 *
	 * @param title
	 * @param values
	 */
	public void addSeries(final String title, final double[] values) {
		int cnt = 1;
		for (double value : values) {
			String category = (cnt > this.categories.length ? Integer.toString(cnt) : this.categories[cnt-1]);
			this.dataset.addValue(value, title, category);
			cnt++;
		}
	}

}


