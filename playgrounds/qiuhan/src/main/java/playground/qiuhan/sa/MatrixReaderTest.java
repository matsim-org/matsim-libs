/* *********************************************************************** *
 * project: org.matsim.*
 * DailyTrafficLoadCurveReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.qiuhan.sa;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

/**
 * tests the function of {@code VisumMatrixReader}
 * 
 * @author Qiuhan
 * 
 */
public class MatrixReaderTest {
	public static void main(String[] args) {
		Matrix m = new Matrix("5oev_o",
				"from QZ-Matrix 5 oev_o_format.mtx of Sonja's DA");
		new VisumMatrixReader(m)
				.readFile("C:/Users/Chen/Documents/Studien/QiuhanSA/QZ-Matrix 5 oev_o_format.mtx");
		System.out
				.println(m.getEntry(new IdImpl("11713"), new IdImpl("11614")));
	}
}
