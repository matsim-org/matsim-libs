package playground.wrashid.tryouts.mess;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.GeneralLib;

public class Main {

	public static void main(String[] args) {
		QuadTree<Integer> quadTree = new QuadTree<Integer>(0, 0, 9000, 9000);

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				quadTree.put(i * 1000 + 500, j * 1000 + 500, 1);
			}
		}

		quadTree.put(8500.0, 9000, 1);
		
	}

}
