/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.droeder.charts;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.signalsystems.model.SignalGroupState;

/**
 * @author droeder
 *
 */
public class DaSignalPlanChart {
	  private DefaultCategoryDataset dataset;
	  private Map<Integer, Color> seriesColor = new HashMap<Integer, Color>();
	  
	  public DaSignalPlanChart(){
		  this.dataset = new DefaultCategoryDataset();
	  }
	  
	  public void addData(Map<Id, TreeMap<Double, SignalGroupState>> data, double tMin, double tMax){
		  Integer i = 0;
		  
		  for (Entry<Id, TreeMap<Double, SignalGroupState>> e : data.entrySet()){
			  double temp = 0.0;
			  SignalGroupState tempState = SignalGroupState.RED;
			  for(Entry<Double, SignalGroupState> ee: e.getValue().entrySet()){
				  if (ee.getKey() > (tMin-1) && ee.getKey()< (tMax+1)){
					  dataset.addValue(ee.getKey()-temp, i, e.getKey());
					  setSeriesColor(i, tempState);
					  for(Entry<Id, TreeMap<Double, SignalGroupState>> eee : data.entrySet()){
						  if (!eee.getKey().equals(e.getKey())){
							  dataset.addValue(0, i, eee.getKey());
						  }
					  }
				  }else if (ee.getKey()>tMax){
					  dataset.addValue(ee.getKey()-temp, i, e.getKey());
					  setSeriesColor(i, tempState);
					  for(Entry<Id, TreeMap<Double, SignalGroupState>> eee : data.entrySet()){
						  if (!eee.getKey().equals(e.getKey())){
							  dataset.addValue(0, i, eee.getKey());
						  }
					  }
					  i++;
					  break;
				  }
				  i++;
				  temp = ee.getKey();
				  tempState = ee.getValue();
			  }
		  }
	  }
	  
	  private void setSeriesColor(Integer i, SignalGroupState state){
		  if(state.equals(SignalGroupState.RED)){
			  seriesColor.put(i,new Color(163, 0, 0));
		  }else if (state.equals(SignalGroupState.GREEN)){
			  seriesColor.put(i, new Color(0, 102, 0));
		  }else if (state.equals(SignalGroupState.YELLOW)){
			  seriesColor.put(i, new Color(255, 204, 0));
		  }else if (state.equals(SignalGroupState.REDYELLOW)){
			  seriesColor.put(i, new Color(255, 102, 0));
		  }
	  }
	  
	  public JFreeChart createSignalPlanChart (String title, String xAxis, String yAxis, double yMin, double yMax){
			JFreeChart chart = ChartFactory.createStackedBarChart(title, xAxis, yAxis, this.dataset, PlotOrientation.HORIZONTAL, false, false, false);
			DaAxisBuilder axis = new DaAxisBuilder();
			CategoryPlot plot = chart.getCategoryPlot();
			
			plot.setBackgroundPaint(Color.white);
		    plot.setDomainGridlinePaint(Color.lightGray);
		    plot.setRangeGridlinePaint(Color.black);
		    plot.setDomainAxis(axis.createCategoryAxis(xAxis));
		    plot.setRangeAxis(axis.createValueAxis(yAxis, yMin, yMax));
		    
			
			final BarRenderer renderer = (BarRenderer) plot.getRenderer();
			renderer.findRangeBounds(dataset);
			for (Entry<Integer, Color> ee : seriesColor.entrySet()){
				renderer.setSeriesPaint(ee.getKey(), ee.getValue());
			}
			return chart;
	  }
	  
	  public void writeDataToTxt(String fileName, Map<Id, TreeMap<Double, SignalGroupState>> data){
		  try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			
			for (Entry<Id, TreeMap<Double, SignalGroupState>> e: data.entrySet()){
				writer.write(e.getKey().toString());
				writer.newLine();
				for(Entry<Double, SignalGroupState> ee : e.getValue().entrySet()){
					writer.write(ee.getKey().toString() + "\t");
				}
				writer.newLine();
				for(Entry<Double, SignalGroupState> ee : e.getValue().entrySet()){
					writer.write(ee.getValue().toString() + "\t");
				}
				writer.newLine();
				writer.newLine();
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
}
