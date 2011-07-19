/* *********************************************************************** *
 * project: org.matsim.*
 * MatrixTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.tests;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

import Jama.Matrix;

public class MatrixTest {
	public static void main(String[] args) {
		double[] vector = new double[] { 1.5983495872350983452340958e-20,
				-3.36549871354687981651654987e-15,
				4.454574684765432135486765465e-16,
				5.54687654324354687464653215e-20 };
		Matrix vec = new Matrix(vector, 4);
		vec.print(new DecimalFormat("0.###E00"), 15);
		try {
			PrintWriter writer = new PrintWriter("d:/tmp/a.log");
			vec.print(writer, new DecimalFormat("0.###E00"), 5);
			writer.close();
			writer = new PrintWriter("d:/tmp/b.log");
			vec.print(writer, new DecimalFormat(), 5);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
