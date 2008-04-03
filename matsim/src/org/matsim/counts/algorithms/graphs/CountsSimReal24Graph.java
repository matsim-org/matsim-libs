/* *********************************************************************** *
 * project: org.matsim.*
 * CountsSimReal24Graph.java
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

package org.matsim.counts.algorithms.graphs;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.CustomXYURLGenerator;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.CountSimComparisonLinkFilter;
import org.matsim.counts.algorithms.graphs.helper.Comp;
import org.matsim.counts.algorithms.graphs.helper.MyComparator;
import org.matsim.utils.identifiers.IdI;

public class CountsSimReal24Graph extends CountsGraph{


	public CountsSimReal24Graph(final List<CountSimComparison> ccl, final int iteration, final String filename){
		super(ccl, iteration, filename, filename);
	}

	@Override
	public JFreeChart createChart(final int nbr) {

		XYSeriesCollection dataset0=new XYSeriesCollection();
		XYSeries series=new XYSeries("MATSim volumes");
		// easier to use another dataset
		XYSeriesCollection dataset_outliers=new XYSeriesCollection();
		XYSeries series_outliers=new XYSeries("MATSim outliers");

		CustomXYURLGenerator url_gen=new CustomXYURLGenerator();
		CustomXYToolTipGenerator tt_gen=new CustomXYToolTipGenerator();

		final ArrayList<String> urls = new ArrayList<String>();
		final ArrayList<String> tooltips = new ArrayList<String>();
		List<Comp> comps=new Vector<Comp>();
		
		
		//--------------------
		CountSimComparisonLinkFilter linkFilter=new CountSimComparisonLinkFilter(this.ccl_);
		
		Iterator<IdI> id_it = new CountSimComparisonLinkFilter(
				this.ccl_).getLinkIds().iterator();
				
		while (id_it.hasNext()) {
			IdI id= id_it.next();				
			
			double countVal=linkFilter.getAggregatedCountValue(id);
			double simVal=linkFilter.getAggregatedSimValue(id);
			
			if (countVal>0.0 &&	simVal>0.0) {
				series.add(countVal,simVal);
				comps.add(new Comp(countVal, "link"+id+".html", "Link "+id+"; " +
						"Count: "+ countVal +", Sim: "+ simVal));
			}
			else {
				/* values with simVal==0.0 or countVal==0.0 are drawn on the x==1 or/and y==1-line
				 * Such values are the result of a poor simulation run, but they can also represent 
				 * a valid result (closing summer road during winter time)
				 */
				countVal=Math.max(1.0, countVal);
				simVal=Math.max(1.0, simVal);
				series_outliers.add(countVal, simVal);
			}	
		}//while
		dataset0.addSeries(series);
		dataset_outliers.addSeries(series_outliers);
		
		Collections.sort(comps, new MyComparator());

		for (Iterator<Comp> iter = comps.iterator(); iter.hasNext();) {
			Comp cp = iter.next();
			urls.add(cp.getURL());
			tooltips.add(cp.getTooltip());
		}

		url_gen.addURLSeries(urls);
		tt_gen.addToolTipSeries(tooltips);

		String title="Avg. Weekday Traffic Volumes, Iteration: "+this.iteration_;
		this.setChartTitle(title);
		this.chart_ = ChartFactory.createXYLineChart(
		title,
		"Count Volumes", // x axis label
		"Sim Volumes", // y axis label
		dataset0, // data
		PlotOrientation.VERTICAL,
		false, // include legend
		true, // tooltips
		true // urls
		);
		XYPlot plot=this.chart_.getXYPlot();
		final LogarithmicAxis axis_x = new LogarithmicAxis("Count Volumes [veh/24h]");
		final LogarithmicAxis axis_y = new LogarithmicAxis("Sim Volumes [veh/24h]");
		axis_x.setAllowNegativesFlag(false);
		axis_y.setAllowNegativesFlag(false);

		//regular values
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setLinesVisible(false);
		renderer.setURLGenerator(url_gen);
		renderer.setSeriesPaint(0, Color.black);
		renderer.setSeriesToolTipGenerator(0, tt_gen);
		renderer.setSeriesShape(0, new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));
		
		//outliers
		XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
		renderer2.setLinesVisible(false);
		renderer2.setSeriesPaint(0, Color.red);
		renderer2.setSeriesShape(0, new Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));

		// error band
		DefaultXYDataset dataset1=new DefaultXYDataset();
		dataset1.addSeries("f1x", new double[][] {{1.0, 10000.0},{1.0, 10000.0}});
		dataset1.addSeries("f2x", new double[][] {{1.0, 10000.0},{2.0, 20000.0}});
		dataset1.addSeries("f05x", new double[][] {{2.0, 10000.0},{1.0, 5000.0}});
		
		XYLineAndShapeRenderer renderer3 = new XYLineAndShapeRenderer();
		renderer3.setShapesVisible(false);
		renderer3.setSeriesPaint(0, Color.blue);
		renderer3.setSeriesPaint(1, Color.blue);
		renderer3.setSeriesPaint(2, Color.blue);
		renderer3.setBaseSeriesVisibleInLegend(false);		
		renderer3.setSeriesItemLabelsVisible(0, true);
		renderer3.setSeriesItemLabelsVisible(1, false);
		renderer3.setSeriesItemLabelsVisible(2, false);
		
		XYTextAnnotation annotation0=new XYTextAnnotation("2.0 count",12000.0, 15500.0);
		annotation0.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation0);
		XYTextAnnotation annotation1=new XYTextAnnotation("count",13000.0, 10000.0);
		annotation1.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation1);
		XYTextAnnotation annotation2=new XYTextAnnotation("0.5 count",11000.0, 3500.0);
		annotation2.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation2);
		
		plot.setDomainAxis(axis_x);
        plot.setRangeAxis(axis_y);
        plot.setRenderer(0, renderer);
        
        plot.setRenderer(1, renderer2);
		plot.setDataset(1, dataset_outliers);
		
        plot.setRenderer(2, renderer3);
		plot.setDataset(2, dataset1);

		plot.getRangeAxis().setRange(1.0, 19000.0);
		plot.getDomainAxis().setRange(1.0, 19000.0);

		return this.chart_;
	}//drawGraph
}


