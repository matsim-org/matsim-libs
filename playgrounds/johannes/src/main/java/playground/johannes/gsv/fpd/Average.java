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

package playground.johannes.gsv.fpd;

import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class Average {

    public static void main(String args[]) {
        Set<NumericMatrix> matrices = new HashSet<NumericMatrix>();
        Set<String> files = new HashSet<String>();
        files.add("/home/johannes/gsv/fpd/telefonica/matrixv2/07.xml");
        files.add("/home/johannes/gsv/fpd/telefonica/matrixv2/08.xml");
        files.add("/home/johannes/gsv/fpd/telefonica/matrixv2/09.xml");
        files.add("/home/johannes/gsv/fpd/telefonica/matrixv2/10.xml");
        files.add("/home/johannes/gsv/fpd/telefonica/matrixv2/11.xml");
        files.add("/home/johannes/gsv/fpd/telefonica/matrixv2/12.xml");
        files.add("/home/johannes/gsv/fpd/telefonica/matrixv2/13.xml");

        NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
        reader.setValidating(false);
        for(String file : files) {
            reader.parse(file);
            matrices.add(reader.getMatrix());
        }

        NumericMatrix avr = new NumericMatrix();
        MatrixOperations.average(matrices, avr);

        NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
        writer.write(avr, "/home/johannes/gsv/fpd/telefonica/matrixv2/avr.xml");
    }


}
