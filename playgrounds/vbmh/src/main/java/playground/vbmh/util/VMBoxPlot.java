package playground.vbmh.util;

import java.awt.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

public class VMBoxPlot {
	double interval;
	double xStartValue=0.0;
	double xEndValue=0.0;
	double maximumXValue=0.0;
	//HashMap<String, LinkedList<double[]>> series = new HashMap<String, LinkedList<double[]>>();
	HashMap<String, Series> series = new HashMap<String, VMBoxPlot.Series>();
	String name, xName, yName;

	public VMBoxPlot(String name, String xName, String yName, double interval) {
		this.name = name;
		this.xName = xName;
		this.yName = yName;
		this.interval = interval;
	}

	public void addSeries(String name, double[] x, double [] y){
		series.put(name, new Series(name, x, y));
		double xMax= series.get(name).getXMax();
		if(xMax>maximumXValue){
			maximumXValue=xMax;
		}
	}
	
	public void divideXAxis(double interval){
		double xMin = 0;
		double xMax = interval;
		while (xMin < maximumXValue){
			for(Series item : series.values()){
				item.addInterval(xMin, xMax);
			}
			xMin+=interval;
			xMax+=interval;
		}
	}
	
	public void saveAsPng(String filename, double xResulution, double yResulution){
		DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
		
		double xMin=xStartValue;
		if(xEndValue!=0){
			maximumXValue = xEndValue;
		}
		while (xMin < maximumXValue){
			for(Series item : series.values()){
				ArrayList values = item.values.get(xMin);
				if(values==null){
					values = new ArrayList<Double>();
					System.out.println("NULL");
				}
				else if(values.size()!=0){
					dataset.add(values, item.name, xMin);
				}
			}
			xMin+=interval;
		}
		
		
		
//		for(Series item : series.values()){
//			Set<Double> keys = item.values.keySet();
//			Iterator<Double> iterator = keys.iterator();
//			for(ArrayList interval : item.values.values()){
//				dataset.add(interval, item.name, iterator.next());
//			}
//		}
		
		//CategoryAxis xAxis = new CategoryAxis(xName);
       // NumberAxis yAxis = new NumberAxis(yName);
        //yAxis.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(false);
        renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
       // CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(name, xName, yName, dataset, true);
        ChartPanel chartPanel = new ChartPanel(chart);

        int width = (int) xResulution;
		int height = (int) yResulution;
        
        try {
			ChartUtilities.saveChartAsPNG(new File(filename), chart, width,
					height);
		} catch (IOException e) {

		}

	}
	
	
	//--------------------------//--------------------------	
	class Series{
		String name;
		double[] x; 
		double [] y;
		HashMap<Double, ArrayList> values = new HashMap<Double, ArrayList>();
		public Series(String name, double[] x, double[] y) {
			this.name = name;
			this.x = x;
			this.y = y;
		}		
//--------------------------
		void addInterval(double xMin, double xMax){
			values.put(xMin, new ArrayList());
			for(int i=0; i<x.length; i++){
				if (x[i]<xMax && x[i]>=xMin){
					values.get(xMin).add(y[i]);
				}
			}
		}
//--------------------------
		double getXMax(){
			double xMax=0.0;
			for(int i=0; i<x.length; i++){
				if (x[i]>=xMax){
					xMax=x[i];
				}
			}
			return xMax;
		}
//--------------------------		
	}	
}
