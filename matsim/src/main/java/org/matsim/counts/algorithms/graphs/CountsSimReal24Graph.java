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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.CountSimComparisonLinkFilter;
import org.matsim.counts.algorithms.graphs.helper.Comp;
import org.matsim.counts.algorithms.graphs.helper.MyComparator;

public final class CountsSimReal24Graph extends CountsGraph{

	/* 
	 * TODO: Min. value of scale is set to 100.0 at the moment
	 * Make it dependend on the data (min value)!
	 */

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

		final ArrayList<String> urls = new ArrayList<>();
		final ArrayList<String> tooltips = new ArrayList<>();
		List<Comp> comps=new Vector<>();
		
		
		//--------------------
		CountSimComparisonLinkFilter linkFilter=new CountSimComparisonLinkFilter(this.ccl_);
		
		final Vector<Id<Link>> linkIds = new CountSimComparisonLinkFilter( this.ccl_).getLinkIds();
		Iterator<Id<Link>> id_it = linkIds.iterator();
		
		double maxCountValue=0.1;
		double maxSimValue=0.1;
		// yyyy PtCountsKMLWriterTest.testPtAlightKMLCreation never touches these and then leads to an exception later
		// when they are zero.  Don't know why. kai, sep'16
		
		while (id_it.hasNext()) {
			Id<Link> id= id_it.next();				
			
			double countVal=linkFilter.getAggregatedCountValue(id);
			double simVal=linkFilter.getAggregatedSimValue(id);
			
			if (countVal>100.0 &&	simVal>100.0) {
				
				if (countVal>maxCountValue) maxCountValue=countVal;
				if (simVal>maxSimValue) maxSimValue=simVal;
				
				series.add(countVal,simVal);
				comps.add(new Comp(countVal, "link"+id+".html", "Link "+id+"; " +
						"Count: "+ countVal +", Sim: "+ simVal));
			}
			else {
				/* values with simVal<100.0 or countVal<100.0 are drawn on the x==100 or/and y==100-line
				 */
				countVal=Math.max(100.0, countVal);
				simVal=Math.max(100.0, simVal);
				series_outliers.add(countVal, simVal);
				
				if (countVal>maxCountValue) maxCountValue=countVal;
				if (simVal>maxSimValue) maxSimValue=simVal;
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
		renderer.setDefaultLinesVisible(false);
		renderer.setURLGenerator(url_gen);
		renderer.setSeriesPaint(0, Color.black);
		renderer.setSeriesToolTipGenerator(0, tt_gen);
		renderer.setSeriesShape(0, new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));
		
		//outliers
		XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer();
		renderer2.setDefaultLinesVisible(false);
		renderer2.setSeriesPaint(0, Color.red);
		renderer2.setSeriesShape(0, new Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
	
		// error band
		DefaultXYDataset dataset1=new DefaultXYDataset();
		Gbl.assertIf( maxCountValue > 0. );
		dataset1.addSeries("f1x", new double[][] {{100.0, maxCountValue},{100.0, maxCountValue}});
		dataset1.addSeries("f2x", new double[][] {{100.0, maxCountValue},{200.0, 2*maxCountValue}});
		dataset1.addSeries("f05x", new double[][] {{200.0, maxCountValue},{100.0, 0.5*maxCountValue}});
		
		XYLineAndShapeRenderer renderer3 = new XYLineAndShapeRenderer();
		renderer3.setDefaultShapesVisible(false);
		renderer3.setSeriesPaint(0, Color.blue);
		renderer3.setSeriesPaint(1, Color.blue);
		renderer3.setSeriesPaint(2, Color.blue);
		renderer3.setDefaultSeriesVisibleInLegend(false);		
		renderer3.setSeriesItemLabelsVisible(0, true);
		renderer3.setSeriesItemLabelsVisible(1, false);
		renderer3.setSeriesItemLabelsVisible(2, false);
		
		XYTextAnnotation annotation0=new XYTextAnnotation("2.0 count",maxCountValue, 2*maxCountValue);
		annotation0.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation0);
		XYTextAnnotation annotation1=new XYTextAnnotation("count", maxCountValue, maxCountValue);
		annotation1.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation1);
		XYTextAnnotation annotation2=new XYTextAnnotation("0.5 count",maxCountValue, 0.5*maxCountValue);
		annotation2.setFont(new Font("SansSerif", Font.BOLD, 11));
		plot.addAnnotation(annotation2);
		
		plot.setDomainAxis(axis_x);
        plot.setRangeAxis(axis_y);
        plot.setRenderer(0, renderer);
        
        plot.setRenderer(1, renderer2);
		plot.setDataset(1, dataset_outliers);
		
        plot.setRenderer(2, renderer3);
		plot.setDataset(2, dataset1);

		//plot.getRangeAxis().setRange(1.0, 19000.0);
		//plot.getDomainAxis().setRange(1.0, 19000.0);

		return this.chart_;
	}//drawGraph
}


