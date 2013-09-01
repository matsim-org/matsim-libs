/* *********************************************************************** *
 * project: org.matsim.*
 * HistogramWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.categoryhistogram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.utils.misc.Time;


/**
 * @author dgrether
 */
public class CategoryHistogramWriter {
	
//	private static final Logger log = Logger.getLogger(CategoryHistogramWriter.class);
	
	private String title = "Leg Histogram";
	private String departuresName = "departures";
	private String arrivalsName = "arrivals";
	private String abortName = "stuck";
	private String enRouteName = "enRoute";
	private String yTitle = "# vehicles";
	
	public CategoryHistogramWriter() {}
	
	public void writeCsv(CategoryHistogram histo, String filename){
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename + ".csv"));
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Filename " + filename + " is not found", e);
		}
		write(histo, stream);
		stream.close();
	}

	public void write(CategoryHistogram histo, PrintStream stream){
		this.checkIndex(histo);
		// data about modes, add all first
		stream.print("time\ttime");
		for (String legMode : histo.getCategoryData().keySet()) {
			stream.print("\t"+departuresName+"_" + legMode + "\t" + arrivalsName + "_" + legMode + "\t"+ abortName + "_" + legMode
					+ "\ten-route_" + legMode);
		}
		stream.print("\n");

		Map<String, Integer> enRouteMap = new HashMap<String, Integer>();
		for (int i = (histo.getFirstIndex() - 2); i <= histo.getLastIndex() + 2; i++) {
			int seconds = i * histo.getBinSizeSeconds();
			stream.print(Time.writeTime(seconds));
			stream.print("\t");
			stream.print(Integer.toString(seconds));
			for (String cat : histo.getCategoryData().keySet()) {
				int departures = histo.getDepartures(cat, i);
				int arrivals = histo.getArrivals(cat, i);
				int stuck = histo.getAbort(cat, i);
//				log.error("Cat: " + cat + " index " + i + " dep " + departures + " arr " + arrivals + " stuck " + stuck);
				int enRoute = CategoryHistogramUtils.getNotNullInteger(enRouteMap, cat);
				int modeEnRoute = enRoute + departures - arrivals - stuck;
				enRouteMap.put(cat, modeEnRoute);
				stream.print("\t" + departures + "\t" + arrivals + "\t" + stuck + "\t" + modeEnRoute);
			}
			// new line
			stream.print("\n");
		}
	}

	public void writeGraphics(CategoryHistogram ch, String filenamePrefix) {
		for (String c : ch.getLegModes()){
			this.writeGraphic(ch, filenamePrefix + "_" + c + ".png", c);
		}
	}

	private void checkIndex(CategoryHistogram histo){
		if (histo.getFirstIndex() == null && histo.getLastIndex() == null){
			throw new RuntimeException("No data in histogram");
		}
	}
	
	public JFreeChart getGraphic(final CategoryHistogram histo, final String modeName) {
		this.checkIndex(histo);
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries(this.departuresName, false, true);
		final XYSeries arrivalsSerie = new XYSeries(this.arrivalsName, false, true);
		final XYSeries onRouteSerie = new XYSeries(this.enRouteName, false, true);
		Integer enRoute = 0;
		for (int i = histo.getFirstIndex() - 2 ; i <= histo.getLastIndex() + 2; i++) {
			int departures = histo.getDepartures(modeName, i);
			int arrivals = histo.getArrivals(modeName, i);
			int stuck = histo.getAbort(modeName, i);
			enRoute = enRoute + departures - arrivals - stuck;
			double hour = i * histo.getBinSizeSeconds() / 60.0 / 60.0;
			departuresSerie.add(hour, departures);
			arrivalsSerie.add(hour, arrivals);
			onRouteSerie.add(hour, enRoute);
		}
		xyData.addSeries(departuresSerie);
		xyData.addSeries(arrivalsSerie);
		xyData.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(this.title + ", " + modeName + ", " +
				"it."
				+ histo.getIteration(), 
				"time [h]", 
				yTitle , 
				xyData, 
				PlotOrientation.VERTICAL, 
				true, // legend
				false, // tooltips
				false // urls
				);

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		
		plot.getRenderer().setSeriesStroke(0, new BasicStroke(1.0f));
		plot.getRenderer().setSeriesStroke(1, new BasicStroke(1.0f));
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(1.0f));
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.gray);  
		plot.setDomainGridlinePaint(Color.gray);  
		
		return chart;
	}
	
	/**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips with the specified transportation mode to the
	 * specified file.
	 *
	 * @param filename
	 * @param legMode
	 *
	 * @see #getGraphic(String)
	 */
	public void writeGraphic(final CategoryHistogram modeData, final String filename, final String legMode) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(modeData, legMode), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public void setTitle(String title) {
		this.title = title;
	}

	
	public void setDeparturesName(String departuresName) {
		this.departuresName = departuresName;
	}

	
	public void setArrivalsName(String arrivalsName) {
		this.arrivalsName = arrivalsName;
	}

	
	public void setAbortName(String abortName) {
		this.abortName = abortName;
	}

	
	public void setEnRouteName(String enRouteName) {
		this.enRouteName = enRouteName;
	}

	
	public void setyTitle(String yTitle) {
		this.yTitle = yTitle;
	}

	
	
}
