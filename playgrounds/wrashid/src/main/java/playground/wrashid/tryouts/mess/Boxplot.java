/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.tryouts.mess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

public class Boxplot {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

		final BoxAndWhiskerCategoryDataset dataset = createSampleDataset();

        final CategoryAxis xAxis = new CategoryAxis("Type");
        final NumberAxis yAxis = new NumberAxis("Value");
        yAxis.setAutoRangeIncludesZero(false);
        final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setFillBox(false);
        renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        final JFreeChart chart = ChartFactory.createBoxAndWhiskerChart( "Box-and-Whisker Demo", "x", "y", dataset, true);
        final ChartPanel chartPanel = new ChartPanel(chart);

        int width = 500;
		int height = 300;
        
        try {
			ChartUtilities.saveChartAsPNG(new File("boxPlot.png"), chart, width,
					height);
		} catch (IOException e) {

		}
        
	}
	
	
	
	
	
	
private static BoxAndWhiskerCategoryDataset createSampleDataset() {
        
        final int seriesCount = 3;
        final int categoryCount = 4;
        final int entityCount = 22;
        
        final DefaultBoxAndWhiskerCategoryDataset dataset 
            = new DefaultBoxAndWhiskerCategoryDataset();
        for (int i = 0; i < seriesCount; i++) {
            for (int j = 0; j < categoryCount; j++) {
                final List list = new ArrayList();
                // add some values...
                for (int k = 0; k < entityCount; k++) {
                    final double value1 = 10.0 + Math.random() * 3;
                    list.add(new Double(value1));
                    final double value2 = 11.25 + Math.random(); // concentrate values in the middle
                    list.add(new Double(value2));
                }
                dataset.add(list, "Series " + i, " Type " + j);
            }
            
        }

        return dataset;
    }

}

