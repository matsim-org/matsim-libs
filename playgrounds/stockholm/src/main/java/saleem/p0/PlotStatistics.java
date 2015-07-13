package saleem.p0;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.utils.charts.XYLineChart;

public class PlotStatistics {
	public void PlotCapacities(ArrayList<Double> times, ArrayList<Double> capacitieslink2, ArrayList<Double> capacitieslink4 ){
		 XYLineChart chart = new XYLineChart("Capacities Statistics", "Time", "Capacity");
		 chart.addSeries("Capacity Link 2", toArray(times), toArray(capacitieslink2));
		 chart.addSeries("Capacity Link 4", toArray(times), toArray(capacitieslink4));
		 chart.addMatsimLogo();
         chart.saveAsPng("H:\\Mike Work\\output\\CapacitiesStats" + ".png", 800, 600);
	}
	public void PlotDelays(ArrayList<Double> times, ArrayList<Double> avgdelayslink2, ArrayList<Double> avgdelayslink4){
		XYLineChart chart = new XYLineChart("Average Delays Statistics", "Time", "Delay");
		chart.addSeries("Average Delay Link 2", toArray(times), toArray(avgdelayslink2));
		chart.addSeries("Average Delay  Link 4", toArray(times), toArray(avgdelayslink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\AverageDelaysStats" + ".png", 800, 600);
	}
	public void PlotDelaysandCapacities(ArrayList<Double> times, ArrayList<Double> capacitieslink2, ArrayList<Double> capacitieslink4, ArrayList<Double> avgdelayslink2, ArrayList<Double> avgdelayslink4){
		XYLineChart chart = new XYLineChart("Average Delays and Capacities Statistics", "Time", "Delay/Capacity");
		chart.addSeries("Capacity Link 2", toArray(times), toArray(capacitieslink2));
		chart.addSeries("Capacity Link 4", toArray(times), toArray(capacitieslink4));
		chart.addSeries("Average Delay Link 2", toArray(times), toArray(avgdelayslink2));
		chart.addSeries("Average Delay Link 4", toArray(times), toArray(avgdelayslink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\AverageCapacitiesAndDelaysStats" + ".png", 800, 600);
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
