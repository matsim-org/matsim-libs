package playground.wrashid.bsc.vbmh.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.core.utils.charts.XYScatterChart;

public class VMCharts {
	static HashMap<String, SingleChart> charts = new HashMap<String, SingleChart>();
	
	
	public void addChart(String name){
		charts.put(name, new SingleChart());
		charts.get(name).name=name;
	}
	
	public void addSeries(String chartName, String seriesName){
		charts.get(chartName).series.put(seriesName, new LinkedList<double[]>());
	}
	
	static public void addValues(String chartName, String seriesName, double x, double y){
		charts.get(chartName).series.get(seriesName).add(new double[]{x, y});
		//System.out.println("Add values");
	}
	
	
	static public void setAxis(String chartName, String xName, String yName){
		charts.get(chartName).xName=xName;
		charts.get(chartName).yName=yName;
	}
	
	static public void printCharts(String directory, int iter){
		for (SingleChart chart : charts.values()){
			chart.print(directory+"/"+chart.name+"_"+iter+".png");
		}
	}
	
	static public void clear(){
		charts.clear();
	}
	
	
	
	
	class SingleChart {
		HashMap<String, LinkedList<double[]>> series = new HashMap<String, LinkedList<double[]>>();
		String name;
		String xName = "X";
		String yName = "Y";
		
		void print(String filename){
			XYScatterChart chart = new XYScatterChart(name, xName, yName);
			
			Iterator<String> names = series.keySet().iterator();
			for (LinkedList<double[]> serie : series.values()){
				double[] x = new double[serie.size()];
				double[] y = new double[serie.size()];
				int i=0;
				for(double[] element : serie){
					x[i]=element[0];
					y[i]=element[1];
					i++;
				}
				chart.addSeries(names.next(), x,y);
			}
			
			
			chart.saveAsPng(filename, 800, 600);
			
		}
		
	}
	
	
}
