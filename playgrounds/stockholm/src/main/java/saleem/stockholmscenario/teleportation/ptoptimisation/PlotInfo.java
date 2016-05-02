package saleem.stockholmscenario.teleportation.ptoptimisation;

import java.util.ArrayList;
import java.util.Iterator;

import org.jfree.chart.plot.XYPlot;
import org.matsim.core.utils.charts.XYLineChart;

public class PlotInfo {
	public void PlotWaitingPassengers(String path, ArrayList<Double> times, ArrayList<Double> waitingpassengers ){
		 XYLineChart chart = new XYLineChart("Waiting Passengers Plot", "Time", "Waiting Passengers");
		 chart.addSeries("", toArray(times), toArray(waitingpassengers));
		 chart.addMatsimLogo();
		 chart.saveAsPng(path, 800, 600);
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
}
