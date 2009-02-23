/**
 * 
 */
package playground.yu.utils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.MatrixSeries;
import org.jfree.data.xy.MatrixSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.matsim.utils.charts.ChartUtil;

/**
 * @author yu
 * 
 */
public class BubbleChart extends ChartUtil {
	private MatrixSeriesCollection dataset;

	/**
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 */
	public BubbleChart(String title, String xAxisLabel, String yAxisLabel) {
		super(title, xAxisLabel, yAxisLabel);
		dataset = new MatrixSeriesCollection();
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

	public void addSeries(final String title, final double[] xs,
			final double[] ys, final double[] zs) {
		MatrixSeries ms=new MatrixSeries(title, xs.length, ys.length);
//		TODO
		this.dataset.addSeries(ms);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
