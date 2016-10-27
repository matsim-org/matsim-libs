/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.io;

import org.apache.log4j.Logger;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixTxtIO;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.IOException;

/**
 * @author johannes
 */
public class XML2NumericMatrix {

    private static final Logger logger = Logger.getLogger(XML2NumericMatrix.class);

    public static void main(String args[]) throws IOException {
        NumericMatrixXMLReader xmlReader = new NumericMatrixXMLReader();
        xmlReader.setValidating(false);
        logger.info("Loading xml matrix...");
        xmlReader.readFile("/Users/johannes/gsv/miv-matrix/refmatrices/tomtom.de.modena.xml");
        NumericMatrix m = xmlReader.getMatrix();

        logger.info("Writing txt matrix...");
        NumericMatrixTxtIO.write(m, "/Users/johannes/gsv/miv-matrix/refmatrices/tomtom.de.modena.txt.gz");
        logger.info("Done.");
    }
}
