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
		
		
		
		if (-28800.121845080957 > 0.31){
			System.out.println("bad");
		} else {
			System.out.println("good");
		}
		
		Evaluator eval=new Evaluator();
		
		try {
			System.out.println(eval.evaluate("1+1"));
		} catch (EvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
