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

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import playground.johannes.gsv.zones.KeyMatrix;

import java.util.Stack;

/**
 * @author johannes
 *
 */
public class KeyMatrixXMLReader extends MatsimXmlParser {

	private KeyMatrix m;
	
	public KeyMatrix getMatrix() {
		return m;
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equalsIgnoreCase(ODMatrixXMLWriter.MATRIX_TAG)) {
			m = new KeyMatrix();
		} else if(name.equalsIgnoreCase(ODMatrixXMLWriter.CELL_TAG)) {
			String row = atts.getValue(ODMatrixXMLWriter.ROW_KEY);
			String col = atts.getValue(ODMatrixXMLWriter.COL_KEY);
			String val = atts.getValue(ODMatrixXMLWriter.VALUE_KEY);
			
			m.set(row, col, new Double(val));
		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}
}
