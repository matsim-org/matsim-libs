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

package playground.johannes.gsv.matrices.io;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixXMLWriter;
import playground.johannes.gsv.zones.io.VisumOMatrixReader;

import java.io.IOException;

/**
 * @author johannes
 */
public class JoinMatrices {

    public static void main(String[] args) throws IOException {
        KeyMatrix sum = new KeyMatrix();
        for(int i = 0; i < args.length - 1; i++) {
            KeyMatrix m = new KeyMatrix();
            VisumOMatrixReader.read(m, args[i]);
            MatrixOperations.add(sum, m);
        }

        KeyMatrixXMLWriter writer = new KeyMatrixXMLWriter();
        writer.write(sum, args[args.length - 1]);
    }
}
