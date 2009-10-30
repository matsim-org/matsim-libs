/* *********************************************************************** *
 * project: org.matsim.*
 * ChartWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.utils.charts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author dgrether
 *
 */
public class DgChartWriter {
	
	private static final Logger log = Logger.getLogger(DgChartWriter.class);

	
	public static void writerChartToFile(String filename, JFreeChart jchart) {
		filename += ".png";
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), jchart, 800, 600, null, true, 9);
			log.info("DeltaScoreIncomeChart written to : " +filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void writeChartDataToFile(String filename, JFreeChart chart) {
		filename += ".txt";
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			
			XYPlot xy = chart.getXYPlot();
			xy.getRangeAxis().getLabel();
			xy.getDomainAxis().getLabel();
			for (int i = 0; i < xy.getDatasetCount(); i++){
				XYDataset xyds = xy.getDataset(i);
				
				for (int seriesIndex = 0; seriesIndex < xyds.getSeriesCount(); seriesIndex ++) {
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
		  writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
			
		
	}
	
}
