/**
 * 
 */
package playground.yu.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;
import org.matsim.core.utils.charts.ChartUtil;

/**
 * @author yu
 * 
 */
public class BubbleChart extends ChartUtil {
	private DefaultXYZDataset dataset;

	/**
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 */
	public BubbleChart(String title, String xAxisLabel, String yAxisLabel) {
		super(title, xAxisLabel, yAxisLabel);
		dataset = new DefaultXYZDataset();
		this.chart = createChart(title, xAxisLabel, yAxisLabel, this.dataset);
		addDefaultFormatting();
	}

	@Override
	protected JFreeChart getChart() {
		return chart;
	}

	private JFreeChart createChart(String title, String xAxisLabel,
			String yAxisLabel, XYZDataset dataset) {
		return ChartFactory.createBubbleChart(title, xAxisLabel, yAxisLabel,
				dataset, PlotOrientation.VERTICAL, true// legend?
				, false// tooltips?
				, false// URLs?
				);
	}

	/**
	 * @param title
	 * @param data
	 *            the data (must be an array with length 3, containing three
	 *            arrays of equal length, the first containing the x-values, the
	 *            second containing the y-values and the third containing the
	 *            z-values).
	 */
	public void addSeries(final String title, final double[][] data) {
		this.dataset.addSeries(title, data);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BubbleChart chart = new BubbleChart("TITLE", "x-axis", "y-axis");
		chart.addSeries("serie 1", new double[][] { { 1, 3, 5 }, { 2, 4, 6 },
				{ 0.1, 0.2, 0.3 } });
		chart.saveAsPng("output/bubbleTest.png", 800, 600);
	}
}
