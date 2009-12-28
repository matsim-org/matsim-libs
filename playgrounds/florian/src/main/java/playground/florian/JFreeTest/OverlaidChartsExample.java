package playground.florian.JFreeTest;

import java.awt.Dimension;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class OverlaidChartsExample extends ApplicationFrame {

	public OverlaidChartsExample(String title) {
		super(title);
		
		//Create first dataset
		DefaultCategoryDataset data1 = new DefaultCategoryDataset();
		for (int x = 1; x < 10; x++) {
			for (int y = 1; y < 10; y++) {
				data1.addValue(x * y, "Reihe " + x, "Spalte " + y);
			}
		}
		//Create first Renderer
		CategoryItemRenderer renderer1 = new BarRenderer();
		//Create plot
		CategoryPlot plot = new CategoryPlot();
		plot.setDataset(data1);
		plot.setRenderer(renderer1);
		plot.setDomainAxis(new CategoryAxis("Spalte"));
		plot.setRangeAxis(new NumberAxis("Value"));
		plot.setOrientation(PlotOrientation.VERTICAL);
		
		//create second dataset
		DefaultCategoryDataset data2 = new DefaultCategoryDataset();
		for (int x = 10; x > 0; x--) {
			for (int y = 10; y > 0; y--) {
				data2.addValue(x*y, "Reihe2 " + x, "Spalte2 " + y);
			}
		}
		//create second renderer
		CategoryItemRenderer renderer2 = new LineAndShapeRenderer();
		
		//Add second dataset and renderer to the previously created plot
		plot.setDataset(1, data2);
		plot.setRenderer(1, renderer2);
		
		//Create chart
		JFreeChart chart = new JFreeChart("Test",plot);
		ChartPanel chartPanel = new ChartPanel(chart, false);
		chartPanel.setPreferredSize(new Dimension(1024, 768));
		setContentPane(chartPanel);
		
	}

	public static void main(String[] args) {
		OverlaidChartsExample trial = new OverlaidChartsExample("Test");
		trial.pack();
		RefineryUtilities.centerFrameOnScreen(trial);
		trial.setVisible(true);
		
	}

}
