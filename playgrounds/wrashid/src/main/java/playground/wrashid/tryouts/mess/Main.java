package playground.wrashid.tryouts.mess;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.swing.JFrame;

import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.math.plot.Plot2DPanel;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.utils.collections.QuadTree;


public class Main {

	public static void main(String[] args) {

		double[] x = { 1, 2, 3, 4, 5, 6 };
		double[] y = { 45, 89, 6, 32, 63, 12 };
 
		// create your PlotPanel (you can use it as a JPanel)
		Plot2DPanel plot = new Plot2DPanel();
 
		// define the legend position
		plot.addLegend("SOUTH");
 
		// add a line plot to the PlotPanel
		plot.addLinePlot("my plot", x, y);
 
		// put the PlotPanel in a JFrame like a JPanel
		JFrame frame = new JFrame("a plot panel");
		frame.setSize(600, 600);
		frame.setContentPane(plot);
		frame.setVisible(true);
		try {
			plot.toGraphicFile(new File("c:/tmp/abc.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
