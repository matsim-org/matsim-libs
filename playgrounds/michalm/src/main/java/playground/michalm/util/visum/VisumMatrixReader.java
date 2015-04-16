/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.util.visum;

import java.io.*;
import java.lang.reflect.Array;
import java.text.*;
import java.util.*;

import org.matsim.core.utils.io.IOUtils;


public class VisumMatrixReader
{
    public static double[][] readMatrixFile(String file)
    {
        try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
            NumberReader nr = new NumberReader(reader);

            nr.nextDouble();// time interval - from
            nr.nextDouble();// time interval - to
            nr.nextDouble();// factor

            int count = nr.nextInt(); // number of objects

            // object ids
            for (int i = 0; i < count; i++) {
                nr.nextInt();
            }

            // values
            double[][] odMatrix = (double[][])Array.newInstance(double.class, count, count);
            for (int i = 0; i < count; i++) {
                for (int j = 0; j < count; j++) {
                    odMatrix[i][j] = nr.nextDouble();
                }
            }

            return odMatrix;
        }
        catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }


    private static class NumberReader
    {
        private final BufferedReader reader;
        private final NumberFormat nf = NumberFormat.getInstance(Locale.US);
        private StringTokenizer st;


        private NumberReader(BufferedReader reader)
            throws IOException
        {
            this.reader = reader;
            moveToNextValidLine();
        }


        public double nextDouble()
            throws IOException, ParseException
        {
            return nextNumber().doubleValue();
        }


        public int nextInt()
            throws IOException, ParseException
        {
            return nextNumber().intValue();
        }


        public Number nextNumber()
            throws IOException, ParseException
        {
            if (!st.hasMoreTokens()) {
                moveToNextValidLine();
            }

            return nf.parse(st.nextToken());
        }


        private void moveToNextValidLine()
            throws IOException
        {
            String line;
            do {
                line = reader.readLine();
            }
            while (!isValidLine(line));

            st = new StringTokenizer(line);
        }


        private boolean isValidLine(String line)
        {
            if (line.trim().isEmpty()) {
                return false;
            }

            char firstChar = line.charAt(0);
            return firstChar != '*' && firstChar != '$';
        }
    }
}
