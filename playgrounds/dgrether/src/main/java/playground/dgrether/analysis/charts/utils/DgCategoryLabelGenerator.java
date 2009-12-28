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
import java.util.Map;

import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.data.category.CategoryDataset;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author dgrether
 *
 */
public class DgCategoryLabelGenerator 
implements CategoryItemLabelGenerator {
	private Map<Tuple<Integer, Integer>, String> labels = 
		new HashMap<Tuple<Integer, Integer>, String>();

	public DgCategoryLabelGenerator() {
	}

	public String generateColumnLabel(CategoryDataset dataset, int column) {
		return null;
	}

	public String generateLabel(CategoryDataset dataset, int row, int column) {
		return this.labels.get(new Tuple<Integer, Integer>(row, column));
	}

	public String generateRowLabel(CategoryDataset dataset, int row) {
		return null;
	}

	public void addLabel(int row, int column, String value) {
		this.labels.put(new Tuple<Integer, Integer>(row, column), value);
	}

}
