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
import org.matsim.core.utils.io.IOUtils;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author johannes
 */
public class GSVMatrixIO {

    private static final Logger logger = Logger.getLogger(GSVMatrixIO.class);

    private static final String SEPARATOR = ";";

    public static NumericMatrix read(String file) {
        NumericMatrix m = new NumericMatrix();
        int errCnt = 0;
        try {
            BufferedReader reader = IOUtils.getBufferedReader(file);
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                String tokens[] = line.split(SEPARATOR);
                if(tokens.length == 9)
                    m.add(tokens[0], tokens[1], Double.parseDouble(tokens[8]));
                else
                    errCnt++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(errCnt > 0) logger.warn(String.format("Skipped reading %s lines.", errCnt));

        return m;
    }
}
