package saleem.p0;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;

import org.jfree.chart.plot.XYPlot;
import org.matsim.core.utils.charts.XYLineChart;

public class PlotStatistics {
	public void PlotCapacities(int iter, ArrayList<Double> times, ArrayList<Double> capacitieslink2, ArrayList<Double> capacitieslink4, ArrayList<Double> initialcapacitiesLink2, ArrayList<Double> initialcapacitiesLink4 ){
		 XYLineChart chart = new XYLineChart("Capacities Statistics", "Time", "Capacity");
		 XYPlot plot = (XYPlot)chart.getChart().getPlot();
		 plot.getRenderer().setSeriesPaint(2, Color.magenta);
		 plot.getRenderer().setSeriesPaint(3, Color.CYAN);
		 plot.getRenderer().setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		 plot.getRenderer().setSeriesStroke(3, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		 chart.addSeries("P0 Applied: Capacity Link 4", toArray(times), toArray(capacitieslink2));
		 chart.addSeries("P0 Applied: Capacity Link 5", toArray(times), toArray(capacitieslink4));
		 chart.addSeries("P0 Not Applied: Capacity Link 4", toArray(times), toArray(initialcapacitiesLink2));
		 chart.addSeries("P0 Not Applied: Capacity Link 5", toArray(times), toArray(initialcapacitiesLink4));
		 chart.addMatsimLogo();
		 chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\CapacitiesStats" + ".png", 800, 600);
	}
	public void PlotAbsolutePressureDiff(int iter, ArrayList<Double> iters, ArrayList<Double> initialabsolutepressuredifference, ArrayList<Double> avgabsolutepressuredifference ){
		 XYLineChart chart = new XYLineChart("Absolute of Pressure Difference between Link 1 and Link2", "Iteration Number", "Abs(P2-P4)");
		 XYPlot plot = (XYPlot)chart.getChart().getPlot();
//		 plot.getRenderer().setSeriesStroke(1, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		 chart.addSeries("P0 Applied: Absolute of Pressure Difference", toArray(iters), toArray(avgabsolutepressuredifference));
		 chart.addSeries("P0 Not Applied: Absolute of Pressure Difference", toArray(iters), toArray(initialabsolutepressuredifference));
		 chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AbsPressDiff" + ".png", 800, 600);
	}
	public void PlotDelays(int iter, ArrayList<Double> timeslink2, ArrayList<Double> timeslink4, ArrayList<Double> avgdelayslink2, ArrayList<Double> avgdelayslink4, ArrayList<Double> initialtimeslink2, ArrayList<Double> initialtimeslink4, ArrayList<Double> initialdelaysLink2, ArrayList<Double> initialdelaysLink4){
		XYLineChart chart = new XYLineChart("Average Delays Statistics", "Time", "Delay");
		XYPlot plot = (XYPlot)chart.getChart().getPlot();
		plot.getRenderer().setSeriesPaint(2, Color.magenta);
		plot.getRenderer().setSeriesPaint(3, Color.CYAN);
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		plot.getRenderer().setSeriesStroke(3, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		chart.addSeries("P0 Applied: Average Delay Link 4", toArray(timeslink2), toArray(avgdelayslink2));
		chart.addSeries("P0 Applied: Average Delay  Link 5", toArray(timeslink4), toArray(avgdelayslink4));
		chart.addSeries("P0 Not Applied: Average Delay Link 4", toArray(initialtimeslink2), toArray(initialdelaysLink2));
		chart.addSeries("P0 Not Applied: Average Delay  Link 5", toArray(initialtimeslink4), toArray(initialdelaysLink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AverageDelaysStats" + ".png", 800, 600);
	}
	public void plotAbsolutePressures(int iter, ArrayList<Double> times, ArrayList<Double> abspreslink2, ArrayList<Double> abspreslink4){
		XYLineChart chart = new XYLineChart("Absloute Pressures", "Time", "Abs Pres");
		chart.addSeries("Absolute Pressure Link 4", toArray(times), toArray(abspreslink2));
		chart.addSeries("Abslute Pressure Link 5", toArray(times), toArray(abspreslink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AbsPres" + ".png", 800, 600);
	}
	public void PlotDelaysandCapacities(int iter, ArrayList<Double> times, ArrayList<Double> capacitieslink2, ArrayList<Double> capacitieslink4, ArrayList<Double> avgdelayslink2, ArrayList<Double> avgdelayslink4){
		XYLineChart chart = new XYLineChart("Average Delays and Capacities Statistics", "Time", "Delay/Capacity");
		chart.addSeries("Capacity Link 4", toArray(times), toArray(capacitieslink2));
		chart.addSeries("Capacity Link 5", toArray(times), toArray(capacitieslink4));
		chart.addSeries("Average Delay Link 4", toArray(times), toArray(avgdelayslink2));
		chart.addSeries("Average Delay Link 5", toArray(times), toArray(avgdelayslink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AverageCapacitiesAndDelaysStats" + ".png", 800, 600);
	}
	public double[] toArray(ArrayList<Double> alist){//Converting a list to array
		double[] array = new double[alist.size()];
		Iterator<Double> iter = alist.iterator();
		int i=0;
		while(iter.hasNext()){
			array[i]=iter.next();
			i++;
		}
		return array;
	}
	public double[] toArrayDelays(ArrayList<Double> alist){//Converting a list to array and reducing the slope
		double[] array = new double[alist.size()];
		Iterator<Double> iter = alist.iterator();
		int i=0;
		while(iter.hasNext()){
			array[i]=iter.next()/10;
			i++;
		}
		return array;
	}
}
