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

public class VisumMatrixReader {
	public static double[][] readMatrix(String file) {
		VisumMatrixReader reader = new VisumMatrixReader();
		reader.readFile(file);
		return reader.getMatrix();
	}

	public static int[] readIds(String file) {
		VisumMatrixReader reader = new VisumMatrixReader();
		reader.readFile(file);
		return reader.getIds();
	}

	private int[] ids;
	private double[][] matrix;

	public void readFile(String file) {
		try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
			NumberReader nr = new NumberReader(reader);

			nr.nextDouble();// time interval - from
			nr.nextDouble();// time interval - to
			nr.nextDouble();// factor

			int count = nr.nextInt(); // number of objects

			// object ids
			ids = new int[count];
			for (int i = 0; i < count; i++) {
				ids[i] = nr.nextInt();
			}

			// values
			matrix = (double[][])Array.newInstance(double.class, count, count);
			for (int i = 0; i < count; i++) {
				for (int j = 0; j < count; j++) {
					matrix[i][j] = nr.nextDouble();
				}
			}
		} catch (IOException | ParseException e) {
			throw new RuntimeException(e);
		}
	}

	public int[] getIds() {
		return ids;
	}

	public double[][] getMatrix() {
		return matrix;
	}

	private static class NumberReader {
		private final BufferedReader reader;
		private final NumberFormat nf = NumberFormat.getInstance(Locale.US);
		private StringTokenizer st;

		private NumberReader(BufferedReader reader) throws IOException {
			this.reader = reader;
			moveToNextValidLine();
		}

		public double nextDouble() throws IOException, ParseException {
			return nextNumber().doubleValue();
		}

		public int nextInt() throws IOException, ParseException {
			return nextNumber().intValue();
		}

		public Number nextNumber() throws IOException, ParseException {
			if (!st.hasMoreTokens()) {
				moveToNextValidLine();
			}

			return nf.parse(st.nextToken());
		}

		private void moveToNextValidLine() throws IOException {
			String line;
			do {
				line = reader.readLine();
			} while (!isValidLine(line));

			st = new StringTokenizer(line);
		}

		private boolean isValidLine(String line) {
			if (line.trim().isEmpty()) {
				return false;
			}

			char firstChar = line.charAt(0);
			return firstChar != '*' && firstChar != '$';
		}
	}
}
