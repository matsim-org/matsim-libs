/* *********************************************************************** *
 * project: org.matsim.*
 * MatricesReaderMatsimV1.java
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

package org.matsim.matrices;

import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * A reader for matrices files of MATSim according to <code>matrices_v1.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
public class MatricesReaderMatsimV1<T> extends MatsimXmlParser {

	private final static String MATRICES = "matrices";
	private final static String MATRIX = "matrix";
	private final static String ENTRY = "entry";

	private Matrices matrices;
	private Matrix currMatrix = null;

	public MatricesReaderMatsimV1(final Matrices matrices) {
		super(ValidationType.DTD_ONLY);
		this.matrices = matrices;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (MATRICES.equals(name)) {
			startMatrices(atts);
		} else if (MATRIX.equals(name)) {
			startMatrix(atts);
		} else if (ENTRY.equals(name)) {
			startEntry(atts);
		} else {
			throw new RuntimeException(this + "[tag=" + name + " not known or not supported]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (MATRICES.equals(name)) {
			this.matrices = null;
		} else if (MATRIX.equals(name)) {
			this.currMatrix = null;
		}
	}

	private void startMatrices(final Attributes atts) {
		this.matrices.setName(atts.getValue("name"));
	}

	private void startMatrix(final Attributes atts) {
		this.currMatrix = this.matrices.createMatrix(atts.getValue("id"),
				atts.getValue("desc"));
	}

	private void startEntry(final Attributes  atts) {
		this.currMatrix.createAndAddEntry(atts.getValue("from_id"), atts.getValue("to_id"), Double.parseDouble(atts.getValue("value")));
	}

}
