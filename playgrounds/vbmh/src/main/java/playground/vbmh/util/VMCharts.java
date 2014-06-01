package playground.vbmh.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.core.utils.charts.XYLineChart;
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
	
	public void setLine(String chartName, Boolean line){
		charts.get(chartName).line=line;
	}
	
	public void setBoxXStart(String chartName, double xStart){
		charts.get(chartName).boxXStart=xStart;
	}
	public void setBoxXEnd(String chartName, double xEnd){
		charts.get(chartName).boxXEnd=xEnd;
	}
	
	public void setBox(String chartName, Boolean box){
		charts.get(chartName).box=box;
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
	
	static public void setInterval(String chartName, double interval){
		charts.get(chartName).interval=interval;
	}
	
	static public void printCharts(String directory, int iter){
		for (SingleChart chart : charts.values()){
			chart.print(directory+"/"+chart.name+"_"+iter);
		}
	}
	
	static public void clear(){
		charts.clear();
	}
	
	
	
	
	class SingleChart {
		double boxXStart=0.0;
		double boxXEnd=0.0;
		HashMap<String, LinkedList<double[]>> series = new HashMap<String, LinkedList<double[]>>();
		String name;
		String xName = "X";
		String yName = "Y";
		Boolean logAx = true;
		Boolean line = false;
		Boolean scatter = true;
		Boolean box = false;
		double interval;
		
		void print(String filename){
			XYScatterChart chart = new XYScatterChart(name, xName, yName,logAx); //True aktiviert log scale
			XYLineChart lineChart = new XYLineChart(name, xName, yName,logAx);
			VMBoxPlot boxPlot = new VMBoxPlot(name, xName, yName, interval);
			
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
					if(logAx){
						y[i]=element[1]+0.1; // +0.1 nicht schoen aber sonst log scale nicht moeglich wegen 0 werten
					}else{
						y[i]=element[1];
					}
						
					xWert=Double.toString(element[0]);
					yWert=Double.toString(element[1]);
					writer.writeLine(xWert+", "+yWert);
					i++;
				}
				chart.addSeries(name, x,y);
				lineChart.addSeries(name, x,y);
				boxPlot.addSeries(name, x, y);
				writer.close();
			}
			
			//chart.getChart().getXYPlot().getRenderer().setBaseShape(new Ellipse2D.Double(0,0,5,5));
			if(line){
				lineChart.saveAsPng(filename+"line"+".png", 2400, 1500);
			}
			
			if(box){
				boxPlot.xStartValue=boxXStart;
				boxPlot.xEndValue=boxXEnd;
				boxPlot.divideXAxis(interval);
				boxPlot.saveAsPng(filename+"box"+".png", 2400, 1500);
			}
			
			if(true){//if(!box && !line){
				chart.saveAsPng(filename+"scatter"+".png", 2400, 1500);
			}
			
		}
		
	}
	
	
}
