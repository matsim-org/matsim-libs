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
package air.analysis.categoryhistogram;

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


/**
 * @author dgrether
 */
public class CategoryHistogramWriter {

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
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Filename " + filename + " is not found", e);
		}
		write(histo, stream);
		stream.close();
	}

	public void write(CategoryHistogram histo, PrintStream stream){
		// data about modes, add all first
		stream.print("time");
		for (String legMode : histo.getCategoryData().keySet()) {
			stream.print("\t"+departuresName+"_" + legMode + "\t" + arrivalsName + "_" + legMode + "\t"+ abortName + "_" + legMode
					+ "\ten-route_" + legMode);
		}
		stream.print("\n");

		Map<String, Integer> enRouteMap = new HashMap<String, Integer>();
		if (histo.getFirstIndex() == null && histo.getLastIndex() == null){
			throw new RuntimeException("No data in histogram");
		}
		for (int i = histo.getFirstIndex() - 2; i <= histo.getLastIndex() + 2; i++) {
			stream.print(Integer.toString(i * histo.getBinSizeSeconds()));
			for (String m : histo.getCategoryData().keySet()) {
				int departures = histo.getDepartures(m, i);
				int arrivals = histo.getArrivals(m, i);
				int stuck = histo.getAbort(m, i);
				int enRoute = CategoryHistogramUtils.getNotNullInteger(enRouteMap, m);
				int modeEnRoute = enRoute + departures - arrivals - stuck;
				enRouteMap.put(m, modeEnRoute);
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

	public JFreeChart getGraphic(final CategoryHistogram modeData, final String modeName) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries(this.departuresName, false, true);
		final XYSeries arrivalsSerie = new XYSeries(this.arrivalsName, false, true);
		final XYSeries onRouteSerie = new XYSeries(this.enRouteName, false, true);
		Integer enRoute = 0;
		if (modeData.getFirstIndex() == null && modeData.getLastIndex() == null){
			throw new RuntimeException("No data in histogram");
		}
		for (int i = modeData.getFirstIndex() - 2; i <= modeData.getLastIndex() + 2; i++) {
			int departures = modeData.getDepartures(modeName, i);
			int arrivals = modeData.getArrivals(modeName, i);
			int stuck = modeData.getAbort(modeName, i);
			enRoute = enRoute + departures - arrivals - stuck;
			double hour = i * modeData.getBinSizeSeconds() / 60.0 / 60.0;
			departuresSerie.add(hour, departures);
			arrivalsSerie.add(hour, arrivals);
			onRouteSerie.add(hour, enRoute);
		}
		xyData.addSeries(departuresSerie);
		xyData.addSeries(arrivalsSerie);
		xyData.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(this.title + ", " + modeName + ", it."
				+ modeData.getIteration(), "time [h]", yTitle , xyData, PlotOrientation.VERTICAL, true, // legend
				false, // tooltips
				false // urls
				);

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
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
