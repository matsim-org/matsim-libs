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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author droeder
 *
 */
public class DaChartWriter {
	
	public static void writeChart(String filename, Integer width, Integer height, JFreeChart jchart){
		writeToPng(filename, width, height, jchart);
		writeToTxt(filename, jchart);
		
	}
	
	private static void writeToPng(String path, Integer width, Integer height, JFreeChart jchart) {
		String title = path + jchart.getTitle().toString() + ".png";
		
		try {
			ChartUtilities.saveChartAsPNG(new File(title), jchart, width, height, null, true, 5);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static void writeToTxt(String filename, JFreeChart chart) {
		filename += ".txt";
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			try{ /*read "try" as if (plot instanceof XYPlot)*/
				XYPlot xy = chart.getXYPlot();
				String yAxisLabel = xy.getRangeAxis().getLabel();
				
				String xAxisLabel = "";
				if (xy.getDomainAxis() != null){
					xAxisLabel = xy.getDomainAxis().getLabel();
				}
				String header = xAxisLabel + "\t " + yAxisLabel;
				writer.write(header);
				writer.newLine();
				for (int i = 0; i < xy.getDatasetCount(); i++){
					XYDataset xyds = xy.getDataset(i);
					for (int seriesIndex = 0; seriesIndex < xyds.getSeriesCount(); seriesIndex ++) {
						writer.newLine();
						writer.write("Series" + xyds.getSeriesKey(seriesIndex).toString());
						writer.newLine();
						int items = xyds.getItemCount(seriesIndex);
						for (int itemsIndex = 0; itemsIndex < items; itemsIndex++){
							Number xValue = xyds.getX(seriesIndex, itemsIndex);
							Number yValue = xyds.getY(seriesIndex, itemsIndex);
							writer.write(xValue.toString());
							writer.write("\t");
							writer.write(yValue.toString());
							writer.newLine();
						}
					}
				}
			} catch(ClassCastException e){ //else instanceOf CategoryPlot
				CategoryPlot cp = chart.getCategoryPlot();
				String header = "CategoryRowKey \t CategoryColumnKey \t CategoryRowIndex \t CategoryColumnIndex \t Value";
				writer.write(header);
				writer.newLine();
				for (int i = 0; i < cp.getDatasetCount(); i++) {
					CategoryDataset cpds = cp.getDataset(i);
					for (int rowIndex = 0; rowIndex < cpds.getRowCount(); rowIndex++){
						for (int columnIndex = 0; columnIndex < cpds.getColumnCount(); columnIndex ++) {
							Number value = cpds.getValue(rowIndex, columnIndex);
							writer.write(cpds.getRowKey(rowIndex).toString());
							writer.write("\t");
							writer.write(cpds.getColumnKey(columnIndex).toString());
							writer.write("\t");
							writer.write(Integer.toString(rowIndex));
							writer.write("\t");
							writer.write(Integer.toString(columnIndex));
							writer.write("\t");
							writer.write(value.toString());
							writer.newLine();
						}
					}
				}
				
				
			}
		  writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

}
