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

package playground.johannes.gsv.zones.io;

import org.matsim.core.utils.io.IOUtils;
import playground.johannes.gsv.zones.KeyMatrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 */
public class KeyMatrixTxtIO {

    private static final String HEADER = "from\tto\tvalue";

    private static final String TAB = "\t";

    public static void write(KeyMatrix m, String file) throws IOException {
        BufferedWriter writer = IOUtils.getBufferedWriter(file);

        writer.write(HEADER);
        writer.newLine();

        Set<String> keys = m.keys();
        for(String i : keys) {
            for(String j : keys) {
                Double val = m.get(i, j);
                if(val != null) {
                    writer.write(i);
                    writer.write(TAB);
                    writer.write(j);
                    writer.write(TAB);
                    writer.write(String.valueOf(val));
                    writer.newLine();
                }
            }
        }
        writer.close();
    }

    public static void read(KeyMatrix m, String file) throws IOException {
        BufferedReader reader = IOUtils.getBufferedReader(file);

        String line = reader.readLine();
        if(line.startsWith(HEADER)) {
            while((line = reader.readLine()) != null) {
                String tokens[] = line.split(TAB);

                Double val = new Double(tokens[2]);
                m.set(tokens[0], tokens[1], val);
            }
        } else {
            throw new RuntimeException("This does not appear to be a matrix file.");
        }

        reader.close();
    }
}
