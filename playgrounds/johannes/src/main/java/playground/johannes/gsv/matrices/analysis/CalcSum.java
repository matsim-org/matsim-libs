/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.analysis;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;

/**
 * @author johannes
 */
public class CalcSum {

    public static void main(String args[]) {
        KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
        reader.setValidating(false);
        reader.parse("/home/johannes/gsv/fpd/telefonica/matrix/15.xml");
        KeyMatrix m = reader.getMatrix();

        System.out.println(String.format("Trip sum: %s.", MatrixOperations.sum(m)));
    }
}
