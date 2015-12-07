/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.zones.io;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import playground.johannes.gsv.zones.KeyMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class KeyMatrixXMLWriter extends MatsimXmlWriter {

	static final String MATRIX_TAG = "matrix";
	static final String CELL_TAG = "cell";
	static final String ROW_KEY = "row";
	static final String COL_KEY = "col";
	static final String VALUE_KEY = "value";

	public void write(KeyMatrix m, String file) {
		openFile(file);
		writeXmlHead();
		writeStartTag("matrix", null);
		writeEntries(m);
		writeEndTag("matrix");
		close();
	}
	
	
	
	protected void writeEntries(KeyMatrix m) {
		Set<String> keys = m.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double val = m.get(i, j);
				if(val != null) {
					List<Tuple<String, String>> atts = new ArrayList<>(3);
					atts.add(createTuple("row", i));
					atts.add(createTuple("col", j));
					atts.add(createTuple("value", val));
					writeStartTag("cell", atts, true);
				}
			}
		}
	}
}
