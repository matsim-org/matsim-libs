package others.sergioo.visUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class BarChart extends ApplicationFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BarChart(String title, Map<? extends Object, Integer> data, int width, int height, PlotOrientation plotOrientation) {
		super(title);
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for(Entry<? extends Object, Integer> value:data.entrySet())
			dataset.addValue(value.getValue(), "Bar", value.getKey().toString());
		JFreeChart chart = ChartFactory.createBarChart(
		title, null /* x-axis label*/, 
		"Milliseconds" /* y-axis label */, dataset, plotOrientation, false, false, false);
		chart.setBackgroundPaint(Color.white);
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setDrawBarOutline(false);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setFillZoomRectangle(true);
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.setPreferredSize(new Dimension(width, height));
		setContentPane(chartPanel);
		pack();
		RefineryUtilities.centerFrameOnScreen(this);
		setVisible(true);
	}
	
	public BarChart(String title, Map<? extends Object, Integer> data) {
		this(title, data, 600, 400, PlotOrientation.VERTICAL);
	}
	
	public static void main(String[] args) {
		new BarChart("Hola", new HashMap<Object, Integer>());
	}
	
}
