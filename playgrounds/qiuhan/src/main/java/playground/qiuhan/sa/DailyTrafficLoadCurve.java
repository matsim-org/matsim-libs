/* *********************************************************************** *
 * project: org.matsim.*
 * DailyTrafficLoadCurve.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.qiuhan.sa;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.charts.XYScatterChart;

import playground.yu.utils.container.Collection2Array;

/**
 * saves informastion about daily traffic load curve
 * 
 * @author Qiuhan
 * 
 */
public class DailyTrafficLoadCurve {
	private Map<Integer/* time */, Double/* share */> data;

	public DailyTrafficLoadCurve() {
		this.data = new HashMap<Integer, Double>();
	}

	public void addTrafficShare(int time, double share) {
		this.data.put(time, share);
	}

	public Double getTrafficShare(int time) {
		return this.data.get(time);
	}

	public Map<Integer, Double> getTrafficLoad() {
		return this.data;
	}

	public void writeTrafficLoadCurveChart(String chartFilename) {
		XYScatterChart chart = new XYScatterChart(
				"Tagesganglinie ÖPNV in Berlin", "Zeit",
				"Anteil des Verkehraufkommens");
		chart.addSeries("Anteil des Verkehraufkommens",
				Collection2Array.toDoubleArray(data.keySet()),
				Collection2Array.toArrayFromDouble(this.data.values()));
		chart.saveAsPng(chartFilename, 800, 600);
	}

}
