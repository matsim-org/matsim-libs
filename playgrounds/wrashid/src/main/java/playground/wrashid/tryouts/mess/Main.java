package playground.wrashid.tryouts.mess;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

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

		double activityDurationInSeconds = 7200;
		double isMale = 1;
		double age = 30;
		double income = 15000;
		System.out.println("walk time beta :" + -0.108 * 60 * Math.pow(activityDurationInSeconds / 60 / 135, -0.08)
				* (1 + (0.021 * isMale)) * Math.pow(age / 40.0, 0.236));

		System.out.println("search time beta :" + -0.135 * 60 * Math.pow(activityDurationInSeconds / 60 / 135, -0.246)
				* (1 + (-0.102 * isMale)));

		System.out.println("cost beta :" + -0.135 *60* Math.pow((income / 7000), -0.1));

	}

}
