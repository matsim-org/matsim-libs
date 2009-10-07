/* *********************************************************************** *
 * project: org.matsim.*
 * ChartData
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
package playground.benjamin.income1;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.collections.Tuple;


/**
 * @author dgrether
 *
 */
public class ChartData {

	private String chartName;
	private String xLabel;
	
	private String yLabel;

	private Map<String, Tuple<double[], double[]>> series = new HashMap<String, Tuple<double[], double[]>>();
	
	public ChartData(String chartName, String xlabel, String ylabel) {
		this.chartName = chartName;
		this.xLabel = xlabel;
		this.yLabel = ylabel;
	}
	
	public void addSeries(String seriesName, double[] xvalues, double[] yvalues){
		this.series.put(seriesName, new Tuple<double[], double[]>(xvalues, yvalues));
	}
	
	public String getChartName() {
		return chartName;
	}

	
	public void setChartName(String chartName) {
		this.chartName = chartName;
	}

	
	public String getXLabel() {
		return xLabel;
	}

	
	public void setXLabel(String label) {
		xLabel = label;
	}

	
	public String getYLabel() {
		return yLabel;
	}

	
	public void setYLabel(String label) {
		yLabel = label;
	}

	
	public Map<String, Tuple<double[], double[]>> getSeries() {
		return series;
	}

	
	public void setSeries(Map<String, Tuple<double[], double[]>> series) {
		this.series = series;
	}



}
