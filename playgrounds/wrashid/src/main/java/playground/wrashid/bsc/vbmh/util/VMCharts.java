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
	
	public void setAx(String chartName, Boolean logAx){
		charts.get(chartName).logAx=logAx;
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
		Boolean logAx = true;
		
		void print(String filename){
			XYScatterChart chart = new XYScatterChart(name, xName, yName,logAx); //True aktiviert log scale
			
			
			Iterator<String> names = series.keySet().iterator();
			for (LinkedList<double[]> serie : series.values()){
				String name = names.next();
				CSVWriter writer = new CSVWriter(filename+name);
				writer.writeLine(xName+", "+yName);
				double[] x = new double[serie.size()];
				double[] y = new double[serie.size()];
				int i=0;
				String xWert;
				String yWert;
				for(double[] element : serie){
					x[i]=element[0];
					y[i]=element[1]+0.1; // +0.1 nicht schoen aber sonst log scale nicht moeglich wegen 0 werten
					xWert=Double.toString(element[0]);
					yWert=Double.toString(element[1]);
					writer.writeLine(xWert+", "+yWert);
					i++;
				}
				chart.addSeries(name, x,y);
				writer.close();
			}
			
			//chart.getChart().getXYPlot().getRenderer().setBaseShape(new Ellipse2D.Double(0,0,5,5));
			chart.saveAsPng(filename, 2400, 1500);
			
		}
		
	}
	
	
}
