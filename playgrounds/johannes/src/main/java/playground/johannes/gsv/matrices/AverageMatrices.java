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

import java.util.ArrayList;
import java.util.List;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOpertaions;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;
import playground.johannes.gsv.zones.io.ODMatrixXMLReader;

/**
 * @author johannes
 *
 */
public class AverageMatrices {

	public static void main(String[] args) {
		ODMatrixXMLReader reader = new ODMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/miv.512.xml");
		KeyMatrix m1 = reader.getMatrix().toKeyMatrix("gsvId");
		
		reader = new ODMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/miv.521.xml");
		KeyMatrix m2 = reader.getMatrix().toKeyMatrix("gsvId");
		
		List<KeyMatrix> list = new ArrayList<>(2);
		list.add(m1);
		list.add(m2);
		
		KeyMatrix avr = MatrixOpertaions.average(list);
		
		KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
		writer.write(avr, "/home/johannes/gsv/matrices/miv.avr.xml");
	}
}
