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

package playground.johannes.gsv.matrices;

import java.util.Set;

import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixWriter;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOpertaions;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;

/**
 * @author johannes
 * 
 */
public class ODMatrix2Visum {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Matrix m1 = new Matrix("1", null);

		KeyMatrixXMLReader reader2 = new KeyMatrixXMLReader();
		reader2.setValidating(false);
		reader2.parse("/home/johannes/gsv/matrices/miv.avr.xml");
		KeyMatrix m2 = reader2.getMatrix();

		MatrixOpertaions.applyFactor(m2, 11.0);
		MatrixOpertaions.applyDiagonalFactor(m2, 1.3);
		
		MatrixOpertaions.sysmetrize(m2);
		
		
		Set<String> keys = m2.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = m2.get(i, j);
				if (val != null) {
					m1.createEntry(i, j, val);
				}
			}
		}

		VisumMatrixWriter writer = new VisumMatrixWriter(m1);
		writer.writeFile("/home/johannes/gsv/matrices/miv.avr.fma");
		
		KeyMatrixXMLWriter xmlWriter = new KeyMatrixXMLWriter();
		xmlWriter.write(m2, "/home/johannes/gsv/matrices/miv.avr2.xml");
	}

}
