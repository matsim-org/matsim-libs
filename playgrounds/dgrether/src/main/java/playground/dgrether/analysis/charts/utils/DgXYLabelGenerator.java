/* *********************************************************************** *
 * project: org.matsim.*
 * DgLabelGenerator
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
package playground.dgrether.analysis.charts.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.labels.AbstractXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;


/**
 * @author dgrether
 *
 */
public class DgXYLabelGenerator extends AbstractXYItemLabelGenerator
implements XYItemLabelGenerator {
	private Map<Integer, List<String>> labels = new HashMap<Integer, List<String>>();

	public DgXYLabelGenerator() {
		
	}

	public void setLabels(int series, List<String> labels) {
		this.labels.put(series, labels);
	}

	public String generateLabel(XYDataset dataset, int series, int item) {
		return this.labels.get(series).get(item);
	}
}
