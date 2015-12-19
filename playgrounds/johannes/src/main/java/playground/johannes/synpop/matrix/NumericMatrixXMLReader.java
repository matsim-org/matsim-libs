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

package playground.johannes.synpop.matrix;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * @author johannes
 *
 */
public class NumericMatrixXMLReader extends MatsimXmlParser {

	private NumericMatrix m;
	
	public NumericMatrix getMatrix() {
		return m;
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equalsIgnoreCase(NumericMatrixXMLWriter.MATRIX_TAG)) {
			m = new NumericMatrix();
		} else if(name.equalsIgnoreCase(NumericMatrixXMLWriter.CELL_TAG)) {
			String row = atts.getValue(NumericMatrixXMLWriter.ROW_KEY);
			String col = atts.getValue(NumericMatrixXMLWriter.COL_KEY);
			String val = atts.getValue(NumericMatrixXMLWriter.VALUE_KEY);
			
			m.set(row, col, new Double(val));
		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}
}
