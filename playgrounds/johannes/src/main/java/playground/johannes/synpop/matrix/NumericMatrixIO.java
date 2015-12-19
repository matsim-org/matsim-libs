/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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

import java.io.IOException;

/**
 * @author jillenberger
 */
public class NumericMatrixIO {

    public static NumericMatrix read(String file) {
        if(file.endsWith(".xml") || file.endsWith(".xml.gz")) {
            NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
            reader.setValidating(false);
            reader.parse(file);
            return reader.getMatrix();
        } else {
            try {
                NumericMatrix m = new NumericMatrix();
                NumericMatrixTxtIO.read(m, file);
                return m;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static void write(NumericMatrix m, String file) throws IOException {
        if(file.endsWith(".xml") || file.endsWith(".xml.gz")) {
            NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
            writer.write(m, file);
        } else {
            NumericMatrixTxtIO.write(m, file);
        }
    }
}
