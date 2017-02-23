package saleem.p0.resultanalysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.plot.XYPlot;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.charts.XYLineChart;

import saleem.stockholmmodel.utils.CollectionUtil;
/**
 * A utility class to plot different P0 related quantities, like absolute pressures, capacities etc.
 * 
 * @author Mohammad Saleem
 */
public class PlotStatistics {
	public void PlotCapacities(int iter, ArrayList<Double> times, ArrayList<Double> capacitieslink2, ArrayList<Double> capacitieslink4, ArrayList<Double> initialcapacitiesLink2, ArrayList<Double> initialcapacitiesLink4 ){
		 XYLineChart chart = new XYLineChart("Capacities Statistics", "Time", "Capacity");
		 CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		 XYPlot plot = (XYPlot)chart.getChart().getPlot();
		 plot.getRenderer().setSeriesPaint(2, Color.magenta);
		 plot.getRenderer().setSeriesPaint(3, Color.CYAN);
		 plot.getRenderer().setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		 plot.getRenderer().setSeriesStroke(3, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		 chart.addSeries("P0 Applied: Capacity Link 4", cutil.toArray(times), cutil.toArray(capacitieslink2));
		 chart.addSeries("P0 Applied: Capacity Link 5", cutil.toArray(times), cutil.toArray(capacitieslink4));
		 chart.addSeries("P0 Not Applied: Capacity Link 4", cutil.toArray(times), cutil.toArray(initialcapacitiesLink2));
		 chart.addSeries("P0 Not Applied: Capacity Link 5", cutil.toArray(times), cutil.toArray(initialcapacitiesLink4));
		 chart.addMatsimLogo();
		 chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\CapacitiesStats" + ".png", 800, 600);
	}
	public void PlotAbsolutePressureDiff(int iter, ArrayList<Double> iters, ArrayList<Double> initialabsolutepressuredifference, ArrayList<Double> avgabsolutepressuredifference ){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>(); 
		XYLineChart chart = new XYLineChart("Absolute of Pressure Difference between Link 1 and Link2", "Iteration Number", "Abs(P2-P4)");
		 XYPlot plot = (XYPlot)chart.getChart().getPlot();
//		 plot.getRenderer().setSeriesStroke(1, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		 chart.addSeries("P0 Applied: Absolute of Pressure Difference", cutil.toArray(iters), cutil.toArray(avgabsolutepressuredifference));
		 chart.addSeries("P0 Not Applied: Absolute of Pressure Difference", cutil.toArray(iters), cutil.toArray(initialabsolutepressuredifference));
		 chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AbsPressDiff" + ".png", 800, 600);
	}
	public void PlotDelays(int iter, ArrayList<Double> timeslink2, ArrayList<Double> timeslink4, ArrayList<Double> avgdelayslink2, ArrayList<Double> avgdelayslink4, ArrayList<Double> initialtimeslink2, ArrayList<Double> initialtimeslink4, ArrayList<Double> initialdelaysLink2, ArrayList<Double> initialdelaysLink4){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		XYLineChart chart = new XYLineChart("Average Delays Statistics", "Time", "Delay");
		XYPlot plot = (XYPlot)chart.getChart().getPlot();
		plot.getRenderer().setSeriesPaint(2, Color.magenta);
		plot.getRenderer().setSeriesPaint(3, Color.CYAN);
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		plot.getRenderer().setSeriesStroke(3, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
		chart.addSeries("P0 Applied: Average Delay Link 4", cutil.toArray(timeslink2), cutil.toArray(avgdelayslink2));
		chart.addSeries("P0 Applied: Average Delay  Link 5", cutil.toArray(timeslink4), cutil.toArray(avgdelayslink4));
		chart.addSeries("P0 Not Applied: Average Delay Link 4", cutil.toArray(initialtimeslink2), cutil.toArray(initialdelaysLink2));
		chart.addSeries("P0 Not Applied: Average Delay  Link 5", cutil.toArray(initialtimeslink4), cutil.toArray(initialdelaysLink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AverageDelaysStats" + ".png", 800, 600);
	}
	public void PlotCapacitiesGeneric(String path, Map<Id<Link>, Map<Double, Double>> capacities, Map<Id<Link>, List<Double>> withoutP0capacities){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		 XYLineChart chart = new XYLineChart("Capacities Statistics", "Time", "Capacity");
		 XYPlot plot = (XYPlot)chart.getChart().getPlot();
		 Iterator<Id<Link>> iterlinks = capacities.keySet().iterator();
		 int stroke = 0;
		 while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next();
			ArrayList<Double> times = cutil.toArrayList(capacities.get(linkid).keySet().iterator());
			ArrayList<Double> capacitieslink = cutil.toArrayList(capacities.get(linkid).values().iterator());
			chart.addSeries("P0 Applied: Capacity Link " + linkid.toString(), cutil.toArray(times), cutil.toArray(capacitieslink));
			stroke++;
			List<Double> capacitiesNoP0 = withoutP0capacities.get(linkid);
			plot.getRenderer().setSeriesStroke(stroke, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
			chart.addSeries("P0 Not Applied: Capacity Link " + linkid.toString(), cutil.toArray(times), cutil.toArray(capacitiesNoP0));
			stroke++;
		 }
		 chart.saveAsPng(path, 800, 600);
	}
	public void PlotDelaysGeneric(String path, Map<Id<Link>, Map<Double, Double>> delays, Map<Id<Link>, List<Double>> withoutP0delayLinks){
		XYLineChart chart = new XYLineChart("Average Delays Statistics", "Time", "Delay");
		XYPlot plot = (XYPlot)chart.getChart().getPlot();
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		Iterator<Id<Link>> iterlinks = delays.keySet().iterator();
		int stroke = 0;
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next();
			ArrayList<Double> times = cutil.toArrayList(delays.get(linkid).keySet().iterator());
			ArrayList<Double> delayslink = cutil.toArrayList(delays.get(linkid).values().iterator());
			chart.addSeries("P0 Applied: Average Delay  Link " + linkid.toString(), cutil.toArray(times), cutil.toArray(delayslink));
			stroke++;
			plot.getRenderer().setSeriesStroke(stroke, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0.0f));
			List<Double> delayslinkNoP0 = withoutP0delayLinks.get(linkid);
			chart.addSeries("P0 Not Applied: Average Delay  Link " + linkid.toString(), cutil.toArray(times), cutil.toArray(delayslinkNoP0));
			stroke++;
		}
        chart.saveAsPng(path, 800, 600);
	}
	public void PlotAbsPresGeneric(String path,  Iterator<Link> iterlinks, Map<Double, Double> dailyavgabspres, Map<Double, Map<Id<Link>, Double>> dailyabspreslinks){
		XYLineChart chart = new XYLineChart("Average Absloute Pressure Difference", "Time", "Avg Abs Pres Difference");
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		ArrayList<Double> iterations = cutil.toArrayList(dailyavgabspres.keySet().iterator());
		ArrayList<Double> values = cutil.toArrayList(dailyavgabspres.values().iterator());
		while(iterlinks.hasNext()){
			Id<Link> linkid = iterlinks.next().getId();
			ArrayList<Double> abspres = new ArrayList<Double>();
			Iterator<Double> iters = dailyabspreslinks.keySet().iterator();
			while(iters.hasNext()){
				Double iter = iters.next();
				abspres.add(dailyabspreslinks.get(iter).get(linkid));
			}
			chart.addSeries("Daily Average Absoloute Pressure Link: " + linkid, cutil.toArray(iterations), cutil.toArray(abspres));
		}
		chart.addSeries("Daily Average Absoloute Pressure  Difference ", cutil.toArray(iterations), cutil.toArray(values));
        chart.saveAsPng(path, 800, 600);
	}
	public void plotAbsolutePressures(int iter, ArrayList<Double> times, ArrayList<Double> abspreslink2, ArrayList<Double> abspreslink4){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		XYLineChart chart = new XYLineChart("Absloute Pressures", "Time", "Abs Pres");
		chart.addSeries("Absolute Pressure Link 4", cutil.toArray(times), cutil.toArray(abspreslink2));
		chart.addSeries("Abslute Pressure Link 5", cutil.toArray(times), cutil.toArray(abspreslink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AbsPres" + ".png", 800, 600);
	}
	public void PlotDelaysandCapacities(int iter, ArrayList<Double> times, ArrayList<Double> capacitieslink2, ArrayList<Double> capacitieslink4, ArrayList<Double> avgdelayslink2, ArrayList<Double> avgdelayslink4){
		CollectionUtil<Double> cutil = new CollectionUtil<Double>();
		XYLineChart chart = new XYLineChart("Average Delays and Capacities Statistics", "Time", "Delay/Capacity");
		chart.addSeries("Capacity Link 4", cutil.toArray(times), cutil.toArray(capacitieslink2));
		chart.addSeries("Capacity Link 5", cutil.toArray(times), cutil.toArray(capacitieslink4));
		chart.addSeries("Average Delay Link 4", cutil.toArray(times), cutil.toArray(avgdelayslink2));
		chart.addSeries("Average Delay Link 5", cutil.toArray(times), cutil.toArray(avgdelayslink4));
		chart.addMatsimLogo();
        chart.saveAsPng("H:\\Mike Work\\output\\ITERS\\it." + iter + "\\AverageCapacitiesAndDelaysStats" + ".png", 800, 600);
	}
}
